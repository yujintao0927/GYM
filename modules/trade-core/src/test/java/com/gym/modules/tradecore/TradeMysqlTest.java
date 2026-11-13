package com.gym.modules.tradecore;
import com.gym.modules.tradecore.application.TradeService;
import com.gym.modules.tradecore.persistence.TradeDatabase;
import com.gym.modules.tradecore.ports.TradePorts;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
class TradeMysqlTest {
 TradeService service; TradeDatabase db; String actor; boolean allowed; String storeStatus;
 @BeforeEach void setup(){
  db=new TradeDatabase("jdbc:mysql://localhost:3306/gym_dev_test_biz002?allowPublicKeyRetrieval=true&useSSL=false",System.getenv("GYM_DB_USERNAME"),System.getenv("GYM_DB_PASSWORD"));
  db.migrate(); allowed=true;storeStatus="ACTIVE";actor=UUID.randomUUID().toString();
  service=new TradeService(db,new TradePorts(){
   public Decision authorize(String a,String action,String merchant,String store,String owner){return allowed&&a.equals(actor)?Decision.ALLOW:Decision.DENY;}
   public Account requireActive(String id){return new Account(id,1);}
   public Store requireStore(String id){return new Store(id,"merchant-fixture",storeStatus,1);}
   public Policy policy(String id,String version){return new Policy(id,version,Approval.CONFIGURED_FIXTURE,java.util.Set.of(Purpose.values()));}
   public boolean supportsSameInstructionRetry(){return true;}
  });
 }
 String order(String kind){
  String product=service.publish(actor,UUID.randomUUID().toString(),"PERSONAL".equals(kind)?null:"store-fixture",kind,"COUNT",10000,10,2,"fixture","v1");
  return service.createOrder(actor,UUID.randomUUID().toString(),product,actor);
 }
 @Test void paymentSnapshotHoldAndUniqueMoneyAreAtomic(){
  String id=order("MEMBERSHIP"); service.confirmPayment(actor,"pay",id,10000,"CNY","SIMULATED");
  service.confirmPayment(actor,"pay",id,10000,"CNY","SIMULATED");
  assertThat(service.query(actor,id).remaining()).isEqualTo(12);
  service.consume(actor,"consume",id,3,1);
  String refund=service.requestRefund(actor,"refund",id,2);
  assertThat(service.query(actor,id).refundMinor()).isEqualTo(7500);
  assertThatThrownBy(()->service.consume(actor,"during-hold",id,1,3)).hasMessageContaining("RIGHTS_UNAVAILABLE");
  service.approve(actor,"approve",id,3);
  service.dispatch(actor,"dispatch",id,4);
  fact(actor,"fact",id,7500,"CNY","SIMULATED","SUCCEEDED");
  fact(actor,"fact-duplicate",id,7500,"CNY","SIMULATED","SUCCEEDED");
  assertThat(service.query(actor,id).state()).isEqualTo("CLOSED");
  assertThat(db.jdbc().queryForObject("select payload from trd_outbox where aggregate_id=? and type='trade.RefundSUCCEEDED'",String.class,id)).contains("\"refundMinor\":7500");
  assertThat(db.jdbc().queryForObject("select count(*) from trd_money where order_id=? and kind='REFUND'",Integer.class,id)).isEqualTo(1);
  assertThat(refund).isNotBlank();
 }
 @Test void idempotencyConflictAndCurrentRevocation(){
  String id=order("MEMBERSHIP");service.confirmPayment(actor,"pay",id,10000,"CNY","SIMULATED");
  assertThatThrownBy(()->service.confirmPayment(actor,"pay",id,9999,"CNY","SIMULATED")).hasMessageContaining("IDEMPOTENCY_MISMATCH");
  allowed=false;
  assertThatThrownBy(()->service.confirmPayment(actor,"pay",id,10000,"CNY","SIMULATED")).hasMessageContaining("DENIED");
  assertThatThrownBy(()->service.query(actor,id)).hasMessageContaining("DENIED");
 }
 @Test void personalOrdinaryRefundDeniedAndIndependentSourcesSurvive(){
  String id=order("PERSONAL");service.confirmPayment(actor,"pay-personal",id,10000,"CNY","SIMULATED");
  assertThatThrownBy(()->service.requestRefund(actor,"refund-personal",id,1)).hasMessageContaining("PERSONAL_REFUND_ESCALATION_REQUIRED");
  assertThat(service.query(actor,id).remaining()).isEqualTo(12);
 }
 @Test void concurrencyCannotOverconsumeAndReconnectionKeepsState() throws Exception {
  String id=order("COACHING");service.confirmPayment(actor,"pay-concurrent",id,10000,"CNY","SIMULATED");
  var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
  try {
   var tasks=new ArrayList<Future<Boolean>>();
   for(int n=0;n<2;n++){String key="parallel-"+n;tasks.add(pool.submit(()->{start.await();try{service.consume(actor,key,id,8,1);return true;}catch(IllegalStateException e){return false;}}));}
   start.countDown();int successes=0;for(var f:tasks)if(f.get(10,TimeUnit.SECONDS))successes++;
   assertThat(successes).isEqualTo(1);assertThat(service.query(actor,id).remaining()).isEqualTo(4);
   try(var c=db.dataSource().getConnection();var st=c.prepareStatement("select remaining_units from trd_order where id=?")){st.setString(1,id);try(var rs=st.executeQuery()){assertThat(rs.next()).isTrue();assertThat(rs.getLong(1)).isEqualTo(4);}}
  } finally {pool.shutdownNow();}
 }
 @Test void repeatedMigrationAndWrongDatabaseGuard(){
  assertThat(db.migrate()).isZero();
  System.out.println("V4_FLYWAY_CHECKSUM="+db.jdbc().queryForObject("select checksum from flyway_schema_history where version='4'",Integer.class));
  assertThatThrownBy(()->new TradeDatabase("jdbc:mysql://localhost:3306/gym_dev", "unused", "unused")).hasMessageContaining("ISOLATED_DATABASE_REQUIRED");
 }
 @Test void reservationReleaseAndCountRefundWithdrawalRestoreOnlyOwnSource(){
  String first=order("MEMBERSHIP"),second=order("MEMBERSHIP");
  service.confirmPayment(actor,"p1",first,10000,"CNY","SIMULATED");service.confirmPayment(actor,"p2",second,10000,"CNY","SIMULATED");
  service.reserve(actor,"reserve",first,"booking-fixture",4,1);
  assertThat(service.query(actor,first).reserved()).isEqualTo(4);
  assertThatThrownBy(()->service.requestRefund(actor,"blocked",first,2)).hasMessageContaining("RESERVATION_IN_PROGRESS");
  service.finishReservation(actor,"release",first,"booking-fixture",false,2);
  service.requestRefund(actor,"hold",first,3);
  assertThat(service.query(actor,second).state()).isEqualTo("ACTIVE");
  service.restore(actor,"withdraw",first,true,4);
  assertThat(service.query(actor,first).remaining()).isEqualTo(12);
  service.consume(actor,"after-withdraw",first,2,5);
  assertThat(service.query(actor,first).remaining()).isEqualTo(10);
  String next=service.requestRefund(actor,"second-application",first,6);
  assertThat(service.query(actor,first).refundMinor()).isEqualTo(8333);
  assertThat(db.jdbc().queryForObject("select count(*) from trd_outbox where aggregate_id=? and type='trade.RefundRequested'",Integer.class,first)).isEqualTo(2);
  assertThat(next).isNotBlank();
 }
 @Test void simulatedRetryUsesOriginalInstructionAndReconciliationIsSeparate(){
  String id=order("COACHING");service.confirmPayment(actor,"p",id,10000,"CNY","SIMULATED");
  service.requestRefund(actor,"r",id,1);String instruction=service.approve(actor,"a",id,2);service.dispatch(actor,"d",id,3);
  fact(actor,"failed",id,10000,"CNY","SIMULATED","FAILED_CONFIRMED");
  assertThatThrownBy(()->service.retryRefund(actor,"bad-retry",id,false,5)).hasMessageContaining("SAFE_RETRY_UNAVAILABLE");
  assertThat(service.retryRefund(actor,"retry",id,true,5)).isEqualTo(instruction);
  service.reconcile(actor,"diff",id,"PAYMENT",9999);
  assertThat(db.jdbc().queryForObject("select count(*) from trd_difference where order_id=?",Integer.class,id)).isEqualTo(1);
  assertThat(service.query(actor,id).state()).isEqualTo("REFUND_HOLD");
 }
 @Test void crossSubjectAndDefaultAuthorizationDenyWithoutDataChange(){
  String id=order("MEMBERSHIP");service.confirmPayment(actor,"p",id,10000,"CNY","SIMULATED");
  assertThatThrownBy(()->service.query("other-fixture",id)).hasMessageContaining("RESOURCE_NOT_FOUND");
  var denied=new TradeService(db,new TradePorts(){public Account requireActive(String a){return new Account(a,1);}});
  assertThatThrownBy(()->denied.consume(actor,"denied",id,1,1)).hasMessageContaining("DENIED");
  assertThat(service.query(actor,id).remaining()).isEqualTo(12);
 }
 @Test void personalHasNoStoreAndPausedStoreCannotConsume(){
  String personal=order("PERSONAL"),membership=order("MEMBERSHIP");
  service.confirmPayment(actor,"personal",personal,10000,"CNY","SIMULATED");service.confirmPayment(actor,"member",membership,10000,"CNY","SIMULATED");
  assertThat(service.query(actor,personal).store()).isNull();storeStatus="SUSPENDED";
  assertThatThrownBy(()->service.consume(actor,"paused",membership,1,1)).hasMessageContaining("STORE_UNAVAILABLE");
  service.consume(actor,"independent",personal,1,1);
  assertThat(service.query(actor,personal).remaining()).isEqualTo(11);
 }
 @Test void outboxFailureRollsBackPaymentGrantMoneyAndReceipt(){
  String id=order("MEMBERSHIP");String trigger="trd_test_"+UUID.randomUUID().toString().replace("-","");
  db.jdbc().execute("CREATE TRIGGER "+trigger+" BEFORE INSERT ON trd_outbox FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture failure'");
  try{
   assertThatThrownBy(()->service.confirmPayment(actor,"rollback",id,10000,"CNY","SIMULATED")).hasMessage("503 DATABASE_OPERATION_FAILED");
   assertThat(service.query(actor,id).state()).isEqualTo("AWAITING_PAYMENT");
   assertThat(service.query(actor,id).remaining()).isZero();
   assertThat(db.jdbc().queryForObject("select count(*) from trd_money where order_id=?",Integer.class,id)).isZero();
   assertThat(db.jdbc().queryForObject("select count(*) from trd_receipt where actor_id=? and operation='payment.confirm' and idem_key='rollback'",Integer.class,actor)).isZero();
  }finally{db.jdbc().execute("DROP TRIGGER "+trigger);}
  service.confirmPayment(actor,"rollback",id,10000,"CNY","SIMULATED");assertThat(service.query(actor,id).remaining()).isEqualTo(12);
 }
 void fact(String actor,String key,String order,long amount,String currency,String mode,String state){
  var v=service.query(actor,order);service.refundFact(actor,key,order,v.instructionId(),v.version(),amount,currency,mode,state);
 }
 @Test void suspensionAllowsReleaseThenRefundAndResolveIsScoped(){
  String id=order("MEMBERSHIP");service.confirmPayment(actor,"p",id,10000,"CNY","SIMULATED");
  service.reserve(actor,"reserve",id,"booking",2,1);storeStatus="SUSPENDED";
  service.finishReservation(actor,"release",id,"booking",false,2);
  service.requestRefund(actor,"refund",id,3);
  assertThat(service.query(actor,id).state()).isEqualTo("REFUND_HOLD");
  assertThat(service.resolve(actor,actor,"store-fixture","STORE_ENTRY")).isEmpty();
 }
 @Test void staleFailureAndWrongInstructionCannotUnlockRetry(){
  String id=order("COACHING");service.confirmPayment(actor,"p",id,10000,"CNY","SIMULATED");service.requestRefund(actor,"r",id,1);
  String instruction=service.approve(actor,"a",id,2);service.dispatch(actor,"d",id,3);
  fact(actor,"unknown",id,10000,"CNY","SIMULATED","UNKNOWN");
  assertThatThrownBy(()->service.refundFact(actor,"stale",id,instruction,4,10000,"CNY","SIMULATED","FAILED_CONFIRMED")).hasMessageContaining("STALE_CALLBACK");
  assertThatThrownBy(()->service.refundFact(actor,"wrong-id",id,"wrong",5,10000,"CNY","SIMULATED","SUCCEEDED")).hasMessageContaining("INSTRUCTION_MISMATCH");
  assertThat(service.query(actor,id).refundState()).isEqualTo("UNKNOWN");
  service.refundFact(actor,"late-success",id,instruction,4,10000,"CNY","SIMULATED","SUCCEEDED");assertThat(service.query(actor,id).state()).isEqualTo("CLOSED");
 }
 @Test void reconciliationUsesRealFactsAndUnknownDimensionsAreRejected(){
  String id=order("MEMBERSHIP");assertThat(service.reconcile(actor,"unpaid",id,"PAYMENT",0)).isEqualTo("MATCH");
  service.confirmPayment(actor,"p",id,10000,"CNY","SIMULATED");
  assertThat(service.reconcile(actor,"paid",id,"NET",10000)).isEqualTo("MATCH");
  assertThatThrownBy(()->service.reconcile(actor,"unknown-dimension",id,"SETTLEMENT",0)).hasMessageContaining("POLICY_UNAPPROVED");
 }
 @Test void resolveKeepsPersonalAndOtherStoreSourcesIndependent(){
  String id=order("MEMBERSHIP"),personal=order("PERSONAL");service.confirmPayment(actor,"p",id,10000,"CNY","SIMULATED");service.confirmPayment(actor,"p2",personal,10000,"CNY","SIMULATED");
  assertThat(service.resolve(actor,actor,"another-store","STORE_ENTRY")).isEmpty();
  assertThat(service.resolve(actor,actor,null,"PUBLIC_PAID")).hasSize(2);
  storeStatus="SUSPENDED";assertThat(service.resolve(actor,actor,null,"PUBLIC_PAID")).extracting(com.gym.modules.tradecore.api.TradeView::id).containsExactly(personal);
 }
 @Test void dependencyFailureDoesNotLeakDetailsOrPartiallyWrite(){
  String id=order("MEMBERSHIP");service.confirmPayment(actor,"p",id,10000,"CNY","SIMULATED");
  var unavailable=new TradeService(db,new TradePorts(){
   public Account requireActive(String a){throw new IllegalStateException("private provider detail fixture");}
  });
  assertThatThrownBy(()->unavailable.consume(actor,"failed-dependency",id,1,1)).hasMessage("503 DEPENDENCY_UNAVAILABLE");
  assertThat(service.query(actor,id).remaining()).isEqualTo(12);
 }
 @Test void personalPaymentAnomalyPreservesManualCaseWithoutOrdinaryRefund(){
  String id=order("PERSONAL");service.confirmPayment(actor,"p",id,10000,"CNY","SIMULATED");
  String caseId=service.raisePaymentException(actor,"exception",id,"DUPLICATE_PAYMENT",20000);
  assertThat(caseId).isNotBlank();assertThat(service.query(actor,id).remaining()).isEqualTo(12);
  assertThat(db.jdbc().queryForObject("select state from trd_difference where id=?",String.class,caseId)).isEqualTo("MANUAL_REVIEW");
 }
 @Test void refundSuccessFailureRollsBackClosureAndRefundMoney(){
  String id=order("MEMBERSHIP");service.confirmPayment(actor,"p",id,10000,"CNY","SIMULATED");service.requestRefund(actor,"r",id,1);service.approve(actor,"a",id,2);service.dispatch(actor,"d",id,3);
  String trigger="trd_test_"+UUID.randomUUID().toString().replace("-","");
  db.jdbc().execute("CREATE TRIGGER "+trigger+" BEFORE INSERT ON trd_outbox FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture failure'");
  try{
   assertThatThrownBy(()->fact(actor,"success-rollback",id,10000,"CNY","SIMULATED","SUCCEEDED")).hasMessage("503 DATABASE_OPERATION_FAILED");
   assertThat(service.query(actor,id).state()).isEqualTo("REFUND_HOLD");
   assertThat(db.jdbc().queryForObject("select count(*) from trd_money where order_id=? and kind='REFUND'",Integer.class,id)).isZero();
  }finally{db.jdbc().execute("DROP TRIGGER "+trigger);}
  fact(actor,"success-rollback",id,10000,"CNY","SIMULATED","SUCCEEDED");assertThat(service.query(actor,id).state()).isEqualTo("CLOSED");
 }
 @Test void sourceCursorIsBoundToSubjectStoreAndCapability(){
  String a=order("MEMBERSHIP"),b=order("PERSONAL");service.confirmPayment(actor,"pa",a,10000,"CNY","SIMULATED");service.confirmPayment(actor,"pb",b,10000,"CNY","SIMULATED");
  var first=service.resolvePage(actor,actor,null,"PUBLIC_PAID",1,null);
  assertThat(first.items()).hasSize(1);assertThat(first.nextCursor()).isNotBlank();
  var second=service.resolvePage(actor,actor,null,"PUBLIC_PAID",1,first.nextCursor());assertThat(second.items()).hasSize(1);
  assertThat(second.items().getFirst().id()).isNotEqualTo(first.items().getFirst().id());
  assertThatThrownBy(()->service.resolvePage(actor,actor,"different-store","PUBLIC_PAID",1,first.nextCursor())).hasMessageContaining("CURSOR_SCOPE_MISMATCH");
 }
}
