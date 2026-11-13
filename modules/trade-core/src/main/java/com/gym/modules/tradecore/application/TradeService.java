package com.gym.modules.tradecore.application;
import com.gym.modules.tradecore.persistence.TradeDatabase;
import com.gym.modules.tradecore.api.TradeView;
import com.gym.modules.tradecore.ports.TradePorts;
import com.gym.modules.tradecore.domain.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Function;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;

/** SIMULATED application facade. All writes and corresponding receipts/events share one local transaction. */
public final class TradeService {
 private final TradeDatabase db;private final JdbcTemplate j;private final TradePorts ports;
 public TradeService(TradeDatabase database,TradePorts dependencies){db=Objects.requireNonNull(database);j=db.jdbc();ports=Objects.requireNonNull(dependencies);}
 private static IllegalStateException fail(String code){return new IllegalStateException(code);}
 private static void valid(boolean test,String code){if(!test)throw fail(code);}
 private void auth(String actor,String action,String merchant,String store,String owner){
  valid(actor!=null&&!actor.isBlank(),"401 SUBJECT_INVALID");
  var account=dependency(()->ports.requireActive(actor));valid(account!=null&&actor.equals(account.accountId()),"401 SUBJECT_INVALID");
  var decision=dependency(()->ports.authorize(actor,action,merchant,store,owner));
  valid(decision!=null&&decision!=TradePorts.Decision.UNAVAILABLE,"503 AUTHORIZATION_UNAVAILABLE");
  valid(decision==TradePorts.Decision.ALLOW,owner!=null&&!actor.equals(owner)?"404 RESOURCE_NOT_FOUND":"403 DENIED");
 }
 private void policy(String id,String version,TradePorts.Purpose... purposes){
  var p=dependency(()->ports.policy(id,version));
  valid(p!=null&&id.equals(p.policyId())&&version.equals(p.version())&&p.approval()!=null&&p.approval()!=TradePorts.Approval.UNAPPROVED,"422 POLICY_UNAPPROVED");
  for(var purpose:purposes)valid(p.purposes().contains(purpose),"422 POLICY_UNAPPROVED");
 }
 private <T>T dependency(java.util.function.Supplier<T> call){
  try{return call.get();}catch(RuntimeException unavailable){throw fail("503 DEPENDENCY_UNAVAILABLE");}
 }
 private <T>T tx(java.util.function.Supplier<T> action){
  try{return db.transactions().execute(s->action.get());}catch(DataAccessException e){throw fail("503 DATABASE_OPERATION_FAILED");}
 }
 private Map<String,Object> row(String table,String id,boolean lock){
  valid(id!=null&&!id.isBlank(),"422 INVALID_ID");
  var rows=j.queryForList("select * from "+table+" where id=?"+(lock?" for update":""),id);
  valid(rows.size()==1,"404 RESOURCE_NOT_FOUND");return rows.getFirst();
 }
 private String s(Map<String,Object> r,String key){Object value=r.get(key);return value==null?null:value.toString();}
 private long n(Map<String,Object> r,String key){return ((Number)r.get(key)).longValue();}
 private static String id(){return UUID.randomUUID().toString();}
 private String hash(Object... values){
  try{
   var d=MessageDigest.getInstance("SHA-256");
   for(Object v:values){byte[] b=String.valueOf(v).getBytes(StandardCharsets.UTF_8);d.update(java.nio.ByteBuffer.allocate(4).putInt(b.length).array());d.update(b);}
   return HexFormat.of().formatHex(d.digest());
  }catch(java.security.NoSuchAlgorithmException e){throw new AssertionError(e);}
 }
 private String receipt(String actor,String op,String key,String body,java.util.function.Supplier<String> work){
  valid(key!=null&&!key.isBlank()&&key.length()<=100,"422 INVALID_KEY");
  // Serialize the same global actor/operation/key even when targets differ. Placeholder rolls back on failure.
  j.update("insert into trd_receipt(actor_id,operation,idem_key,body_hash,result_id) values(?,?,?,?, '') on duplicate key update idem_key=idem_key",actor,op,key,body);
  var r=j.queryForMap("select * from trd_receipt where actor_id=? and operation=? and idem_key=? for update",actor,op,key);
  valid(body.equals(s(r,"body_hash")),"409 IDEMPOTENCY_MISMATCH");
  if(!s(r,"result_id").isEmpty())return s(r,"result_id");
  String result=work.get();j.update("update trd_receipt set result_id=? where actor_id=? and operation=? and idem_key=?",result,actor,op,key);return result;
 }
 private String jsonId(String value){return value==null?"null":"\""+value+"\"";}
 private void event(Map<String,Object> r,String type,long version){
  var current=row("trd_order",s(r,"id"),false);
  String payload="{\"orderId\":"+jsonId(s(current,"id"))+",\"mode\":\"SIMULATED\",\"kind\":"+jsonId(s(current,"kind"))+",\"paidMinor\":"+n(current,"paid_minor")+",\"refundMinor\":"+n(current,"refund_minor")+",\"remainingUnits\":"+n(current,"remaining_units")+",\"reservedUnits\":"+n(current,"reserved_units")+",\"refundId\":"+jsonId(s(current,"refund_id"))+",\"instructionId\":"+jsonId(s(current,"instruction_id"))+",\"state\":"+jsonId(s(current,"state"))+"}";
  j.update("insert into trd_outbox(event_id,type,aggregate_id,aggregate_version,occurred_at,merchant_id,store_id,payload) values(?,?,?,?,CURRENT_TIMESTAMP(6),?,?,?)",id(),type,s(r,"id"),version,s(r,"merchant_id"),s(r,"store_id"),payload);
 }
 public String publish(String actor,String key,String store,String kind,String formula,long price,long purchased,long gift,String policyId,String policyVersion){return tx(()->{
  valid(Set.of("MEMBERSHIP","COACHING","PERSONAL").contains(kind),"422 INVALID_KIND");
  valid("COUNT".equals(formula),"422 TIME_LIFECYCLE_POLICY_UNAPPROVED");
  valid(price>=0&&purchased>0&&gift>=0,"422 INVALID_PRODUCT");Math.addExact(purchased,gift);
  boolean personal="PERSONAL".equals(kind);
  valid(personal?store==null:store!=null&&!store.isBlank(),"422 INVALID_PRODUCT_SCOPE");
  var st=personal?new TradePorts.Store("","","ACTIVE",0):dependency(()->ports.requireStore(store));valid(st!=null&&(personal||store.equals(st.storeId())),"503 STORE_UNAVAILABLE");
  String savedStore=st.storeId();auth(actor,"trade.product.publish",personal?null:st.merchantId(),personal?null:savedStore,null);policy(policyId,policyVersion,TradePorts.Purpose.NORMALIZED_COUNT,personal?TradePorts.Purpose.PERSONAL_PRICE:"COACHING".equals(kind)?TradePorts.Purpose.COACHING_PRICE:TradePorts.Purpose.CARD_PRODUCT);
  valid("ACTIVE".equals(st.status()),"422 STORE_NOT_SELLING");
  return receipt(actor,"publish",key,hash(store,kind,formula,price,purchased,gift,policyId,policyVersion),()->{
   String product=id();j.update("insert into trd_product(id,store_id,merchant_id,kind,formula,price_minor,purchased_units,gift_units,policy_id,policy_version) values(?,?,?,?,?,?,?,?,?,?)",product,savedStore,st.merchantId(),kind,formula,price,purchased,gift,policyId,policyVersion);return product;
  });
 });}
 public String createOrder(String actor,String key,String product,String beneficiary){return tx(()->{
  var p=row("trd_product",product,true);String store=s(p,"store_id"),merchant=s(p,"merchant_id");
  auth(actor,"trade.order.create",emptyToNull(merchant),emptyToNull(store),beneficiary);
  var account=dependency(()->ports.requireActive(beneficiary));valid(account!=null&&beneficiary.equals(account.accountId()),"401 BENEFICIARY_INVALID");
  var st="PERSONAL".equals(s(p,"kind"))?new TradePorts.Store("","","ACTIVE",0):dependency(()->ports.requireStore(store));valid(st!=null&&merchant.equals(st.merchantId())&&"ACTIVE".equals(st.status()),"422 STORE_NOT_SELLING");policy(s(p,"policy_id"),s(p,"policy_version"));
  return receipt(actor,"createOrder",key,hash(product,beneficiary),()->{
   String order=id();j.update("insert into trd_order(id,product_id,owner_id,store_id,merchant_id,kind,formula,paid_minor,total_units,state,policy_id,policy_version) values(?,?,?,?,?,?,?,?,?,'AWAITING_PAYMENT',?,?)",order,product,beneficiary,store,merchant,s(p,"kind"),s(p,"formula"),n(p,"price_minor"),Math.addExact(n(p,"purchased_units"),n(p,"gift_units")),s(p,"policy_id"),s(p,"policy_version"));return order;
  });
 });}
 private String change(String actor,String key,String order,String action,Object body,Function<Map<String,Object>,String> work){return tx(()->{
  var r=row("trd_order",order,true);auth(actor,"trade."+action,emptyToNull(s(r,"merchant_id")),emptyToNull(s(r,"store_id")),s(r,"owner_id"));
  return receipt(actor,action,key,hash(order,body),()->work.apply(r));
 });}
 private void version(Map<String,Object> r,long expected){valid(n(r,"version")==expected,"409 VERSION_CONFLICT");}
 private static String emptyToNull(String s){return s==null||s.isEmpty()?null:s;}
 private void active(Map<String,Object> r){
  valid("ACTIVE".equals(s(r,"state")),"409 RIGHTS_UNAVAILABLE");
  if(!"PERSONAL".equals(s(r,"kind"))){var st=dependency(()->ports.requireStore(s(r,"store_id")));valid(st!=null&&s(r,"store_id").equals(st.storeId())&&s(r,"merchant_id").equals(st.merchantId())&&Set.of("ACTIVE","GRACE").contains(st.status()),"503 STORE_UNAVAILABLE");}
  policy(s(r,"policy_id"),s(r,"policy_version"));
 }
 public String confirmPayment(String actor,String key,String order,long amount,String currency,String mode){return change(actor,key,order,"payment.confirm",hash(amount,currency,mode),r->{
  valid("SIMULATED".equals(mode)&&"CNY".equals(currency)&&amount==n(r,"paid_minor"),"422 PAYMENT_MISMATCH");
  if(!"AWAITING_PAYMENT".equals(s(r,"state")))return order;
  policy(s(r,"policy_id"),s(r,"policy_version"),TradePorts.Purpose.SIMULATED_ACTIVATION);
  j.update("insert into trd_money(id,order_id,kind,amount_minor) values(?,?,'PAYMENT',?)",id(),order,amount);
  j.update("update trd_order set state='ACTIVE',remaining_units=total_units,version=version+1 where id=?",order);event(r,"trade.PaymentSimulated",n(r,"version")+1);return order;
 });}
 public TradeView query(String actor,String order){return tx(()->{
  var r=row("trd_order",order,false);auth(actor,"trade.order.read",emptyToNull(s(r,"merchant_id")),emptyToNull(s(r,"store_id")),s(r,"owner_id"));
  return new TradeView(order,s(r,"owner_id"),emptyToNull(s(r,"store_id")),s(r,"kind"),s(r,"state"),n(r,"remaining_units"),n(r,"reserved_units"),n(r,"version"),n(r,"refund_minor"),s(r,"refund_state"),s(r,"instruction_id"),"SIMULATED");
 });}
 public String consume(String actor,String key,String order,long units,long expected){return change(actor,key,order,"rights.consume",hash(units,expected),r->{
  active(r);version(r,expected);valid(units>0&&units<=n(r,"remaining_units")-n(r,"reserved_units"),"409 INSUFFICIENT_RIGHTS");
  j.update("update trd_order set remaining_units=remaining_units-?,version=version+1 where id=?",units,order);event(r,"trade.RightsConsumed",expected+1);return order;
 });}
 public String requestRefund(String actor,String key,String order,long expected){return change(actor,key,order,"refund.request",expected,r->{
  valid("ACTIVE".equals(s(r,"state")),"409 RIGHTS_UNAVAILABLE");version(r,expected);valid(!"PERSONAL".equals(s(r,"kind")),"422 PERSONAL_REFUND_ESCALATION_REQUIRED");
  valid(n(r,"reserved_units")==0,"409 RESERVATION_IN_PROGRESS");valid(s(r,"refund_id")==null||Set.of("WITHDRAWN","REJECTED").contains(s(r,"refund_state")),"409 REFUND_ALREADY_REQUESTED");
  policy(s(r,"policy_id"),s(r,"policy_version"));
  long amount=RefundRules.amount(BigInteger.valueOf(n(r,"paid_minor")),BigInteger.valueOf(n(r,"total_units")),BigInteger.valueOf(n(r,"remaining_units")),BigInteger.valueOf(n(r,"refunded_minor"))).longValueExact();
  String refund=id();j.update("update trd_order set state='REFUND_HOLD',refund_id=?,refund_minor=?,refund_remaining=remaining_units,refund_state='REQUESTED',version=version+1 where id=?",refund,amount,order);event(r,"trade.RefundRequested",expected+1);return refund;
 });}
 public String approve(String actor,String key,String order,long expected){return change(actor,key,order,"refund.approve",expected,r->{
  version(r,expected);valid("REQUESTED".equals(s(r,"refund_state")),"409 INVALID_REFUND_STATE");String instruction=id();
  j.update("update trd_order set refund_state='APPROVED_NOT_SENT',instruction_id=?,version=version+1 where id=?",instruction,order);event(r,"trade.RefundApproved",expected+1);return instruction;
 });}
 public String dispatch(String actor,String key,String order,long expected){return change(actor,key,order,"refund.dispatch",expected,r->{
  version(r,expected);var i=new RefundInstruction(s(r,"instruction_id"),n(r,"refund_minor"),RefundInstruction.State.valueOf(s(r,"refund_state"))).dispatch();
  j.update("update trd_order set refund_state=?,version=version+1 where id=?",i.state().name(),order);event(r,"trade.RefundDispatchSimulated",expected+1);return i.instructionId();
 });}
 public String refundFact(String actor,String key,String order,String instructionId,long observedVersion,long amount,String currency,String mode,String state){return change(actor,key,order,"refund.fact",hash(instructionId,observedVersion,amount,currency,mode,state),r->{
  valid(Objects.equals(instructionId,s(r,"instruction_id")),"409 INSTRUCTION_MISMATCH");
  valid(observedVersion>0&&observedVersion<=n(r,"version"),"409 STALE_CALLBACK");
  if(!"SUCCEEDED".equals(state))valid(observedVersion==n(r,"version"),"409 STALE_CALLBACK");
  var before=new RefundInstruction(s(r,"instruction_id"),n(r,"refund_minor"),RefundInstruction.State.valueOf(s(r,"refund_state")));
  var after=before.observe(RefundInstruction.State.valueOf(state),amount,currency,mode);
  if(before.state()==RefundInstruction.State.SUCCEEDED)return before.instructionId();
  if(after.state()==RefundInstruction.State.SUCCEEDED){
   valid(amount<=n(r,"paid_minor")-n(r,"refunded_minor"),"409 REFUND_CAP_EXCEEDED");
   j.update("insert into trd_money(id,order_id,kind,amount_minor) values(?,?,'REFUND',?)",id(),order,amount);
   j.update("update trd_order set state='CLOSED',remaining_units=0,reserved_units=0,refunded_minor=refunded_minor+?,refund_state='SUCCEEDED',version=version+1 where id=?",amount,order);
  } else j.update("update trd_order set refund_state=?,version=version+1 where id=?",after.state().name(),order);
  event(r,"trade.Refund"+after.state().name(),n(r,"version")+1);return after.instructionId();
 });}

 public String reserve(String actor,String key,String order,String businessKey,long units,long expected){return change(actor,key,order,"rights.reserve",hash(businessKey,units,expected),r->{
  active(r);version(r,expected);valid(businessKey!=null&&!businessKey.isBlank()&&businessKey.length()<=100&&units>0&&units<=n(r,"remaining_units")-n(r,"reserved_units"),"409 INSUFFICIENT_RIGHTS");
  var old=j.queryForList("select * from trd_reservation where order_id=? and business_key=?",order,businessKey);
  valid(old.isEmpty(),"409 BUSINESS_KEY_CONFLICT");
  j.update("insert into trd_reservation(business_key,order_id,units,state) values(?,?,?,'RESERVED')",businessKey,order,units);
  j.update("update trd_order set reserved_units=reserved_units+?,version=version+1 where id=?",units,order);event(r,"trade.RightsReserved",expected+1);return order;
 });}
 public String finishReservation(String actor,String key,String order,String businessKey,boolean consume,long expected){return change(actor,key,order,"rights.finish",hash(businessKey,consume,expected),r->{
  if(consume)active(r);else valid("ACTIVE".equals(s(r,"state")),"409 RIGHTS_UNAVAILABLE");version(r,expected);
  var rows=j.queryForList("select * from trd_reservation where order_id=? and business_key=? for update",order,businessKey);
  valid(rows.size()==1,"404 RESERVATION_NOT_FOUND");var reservation=rows.getFirst();valid("RESERVED".equals(s(reservation,"state")),"409 RESERVATION_TERMINAL");long units=n(reservation,"units");
  j.update("update trd_reservation set state=? where order_id=? and business_key=?",consume?"CONSUMED":"RELEASED",order,businessKey);
  j.update("update trd_order set reserved_units=reserved_units-?,remaining_units=remaining_units-?,version=version+1 where id=?",units,consume?units:0,order);event(r,consume?"trade.ReservationConsumed":"trade.ReservationReleased",expected+1);return order;
 });}
 public String restore(String actor,String key,String order,boolean withdraw,long expected){return change(actor,key,order,withdraw?"refund.withdraw":"refund.reject",expected,r->{
  version(r,expected);valid("REQUESTED".equals(s(r,"refund_state")),"409 REFUND_CANNOT_RESTORE");
  // COUNT rights have no elapsed duration to return. TIME requires the approved combined recovery policy.
  valid("COUNT".equals(s(r,"formula")),"422 RECOVERY_POLICY_UNAPPROVED");
  j.update("update trd_order set state='ACTIVE',refund_state=?,version=version+1 where id=?",withdraw?"WITHDRAWN":"REJECTED",order);
  event(r,withdraw?"trade.RefundWithdrawn":"trade.RefundRejected",expected+1);return order;
 });}
 public String retryRefund(String actor,String key,String order,boolean sameIdRetryVerified,long expected){return change(actor,key,order,"refund.retry",hash(sameIdRetryVerified,expected),r->{
  version(r,expected);var instruction=new RefundInstruction(s(r,"instruction_id"),n(r,"refund_minor"),RefundInstruction.State.valueOf(s(r,"refund_state"))).retry(sameIdRetryVerified&&dependency(()->ports.supportsSameInstructionRetry()));
  j.update("update trd_order set refund_state='SUBMITTING',version=version+1 where id=?",order);event(r,"trade.RefundRetrySimulated",expected+1);return instruction.instructionId();
 });}
 public String escalate(String actor,String key,String order,long expected){return change(actor,key,order,"refund.escalate",expected,r->{
  version(r,expected);var instruction=new RefundInstruction(s(r,"instruction_id"),n(r,"refund_minor"),RefundInstruction.State.valueOf(s(r,"refund_state"))).escalate();
  j.update("update trd_order set refund_state='MANUAL_REVIEW',version=version+1 where id=?",order);event(r,"trade.RefundManualReview",expected+1);return instruction.instructionId();
 });}
 public String reconcile(String actor,String key,String order,String dimension,long observed){return change(actor,key,order,"reconcile",hash(dimension,observed),r->{
  valid(observed>=0,"422 INVALID_AMOUNT");valid(Set.of("PAYMENT","REFUND","NET").contains(dimension),"422 POLICY_UNAPPROVED");
  long payment=j.queryForObject("select coalesce(sum(amount_minor),0) from trd_money where order_id=? and kind='PAYMENT'",Long.class,order);
  long refund=j.queryForObject("select coalesce(sum(amount_minor),0) from trd_money where order_id=? and kind='REFUND'",Long.class,order);
  long expected="PAYMENT".equals(dimension)?payment:"REFUND".equals(dimension)?refund:payment-refund;
  if(observed==expected)return "MATCH";
  String difference=id();j.update("insert into trd_difference(id,order_id,expected_minor,observed_minor) values(?,?,?,?)",difference,order,expected,observed);
  event(r,"trade.Reconciliation"+dimension,n(r,"version"));return difference;
 });}
 public List<TradeView> resolve(String actor,String accountId,String store,String capability){
  var page=resolvePage(actor,accountId,store,capability,100,null);valid(page.nextCursor()==null,"422 SOURCE_PAGINATION_REQUIRED");return page.items();
 }
 public com.gym.modules.tradecore.api.SourcePage resolvePage(String actor,String accountId,String store,String capability,int limit,String cursor){return tx(()->{
  auth(actor,"trade.rights.resolve",null,store,accountId);valid(capability!=null&&Set.of("PUBLIC_PAID","STORE_ENTRY","COACHING").contains(capability),"422 CAPABILITY_POLICY_UNAPPROVED");
  valid(limit>=1&&limit<=100,"422 INVALID_LIMIT");String binding=hash(actor,accountId,store,capability),after="";
  if(cursor!=null){
   try{String decoded=new String(Base64.getUrlDecoder().decode(cursor),StandardCharsets.UTF_8);var parts=decoded.split("[|]",-1);
    valid(parts.length==2&&binding.equals(parts[0]),"409 CURSOR_SCOPE_MISMATCH");UUID.fromString(parts[1]);after=parts[1];
   }catch(IllegalArgumentException malformed){throw fail("422 INVALID_CURSOR");}
  }
  var rows=j.queryForList("select id from trd_order where owner_id=? and state='ACTIVE' and id>? order by id limit ?",accountId,after,limit+1);
  boolean hasMore=rows.size()>limit;if(hasMore)rows=rows.subList(0,limit);List<TradeView> result=new ArrayList<>();
  for(var candidate:rows){var r=row("trd_order",s(candidate,"id"),false);if(n(r,"remaining_units")<=n(r,"reserved_units"))continue;
   if(!"PERSONAL".equals(s(r,"kind"))){var st=dependency(()->ports.requireStore(s(r,"store_id")));valid(st!=null,"503 STORE_UNAVAILABLE");if(!s(r,"merchant_id").equals(st.merchantId())||!Set.of("ACTIVE","GRACE").contains(st.status()))continue;}
   boolean match="PUBLIC_PAID".equals(capability)&&Set.of("MEMBERSHIP","PERSONAL").contains(s(r,"kind"))||"STORE_ENTRY".equals(capability)&&"MEMBERSHIP".equals(s(r,"kind"))&&Objects.equals(store,s(r,"store_id"))||"COACHING".equals(capability)&&"COACHING".equals(s(r,"kind"))&&Objects.equals(store,s(r,"store_id"));
   if(match)result.add(query(actor,s(r,"id")));
  }
  String next=hasMore?Base64.getUrlEncoder().withoutPadding().encodeToString((binding+"|"+s(rows.getLast(),"id")).getBytes(StandardCharsets.UTF_8)):null;
  return new com.gym.modules.tradecore.api.SourcePage(result,next);
 });}
 public String raisePaymentException(String actor,String key,String order,String reason,long observed){return change(actor,key,order,"payment.exception",hash(reason,observed),r->{
  valid(Set.of("DUPLICATE_PAYMENT","PAYMENT_ANOMALY").contains(reason)&&observed>=0,"422 INVALID_PAYMENT_EXCEPTION");
  String caseId=id();j.update("insert into trd_difference(id,order_id,expected_minor,observed_minor) values(?,?,?,?)",caseId,order,n(r,"paid_minor"),observed);
  event(r,"trade.PaymentException"+reason,n(r,"version"));return caseId;
 });}
}
