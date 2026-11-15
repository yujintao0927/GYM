package com.gym.modules.bookingcore;
import com.gym.modules.bookingcore.application.BookingService;
import com.gym.modules.bookingcore.api.*;
import com.gym.modules.bookingcore.ports.BookingPorts;
import com.gym.modules.bookingcore.persistence.BookingDatabase;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
class BookingApplicationTest {
 @org.junit.jupiter.api.BeforeAll static void migrate() throws Exception {new MysqlBookingTest().migrationIsExplicitRepeatableAndPersistent();}
 final String prefix=UUID.randomUUID().toString();
 final BookingPorts fixture=new BookingPorts() {
  public PolicyStatus getPolicy(String id,long version){return policyApproved?PolicyStatus.CONFIGURED_FIXTURE:PolicyStatus.UNAPPROVED;}
  public Decision authorize(String actor,String action,String merchant,String store,String owner){return (revoked||(foreignCoach&&action.equals("booking.coach"))||actor.equals("denied")||(owner!=null&&!owner.equals(actor)))?Decision.DENY:Decision.ALLOW;}
  public void requireActive(String id) {if(id.equals("inactive"))throw new BookingFailure(401,"ACTOR_INVALID");}
  public Store requireStore(String id){if(unavailable)throw new IllegalStateException("fixture-sensitive-marker");return new Store(id,merchant,"ACTIVE",1);}
  public Purchase requirePurchase(String order,String owner,String store,String session,String coach,String kind,String policy,long version) {return new Purchase(order,owner,store,"fixture-product",productKind,policy,version,true,true,100);}
  public Coach requireCoach(String coach,String store){return new Coach(coach,store,foreignCoach?"foreign":"fixture-merchant","ACTIVE",1);}
  public List<Right> resolve(String owner,String store,String capability){return List.of(new Right("fixture-source",strictVersion?sourceVersion.get():1,swappedOrder?"other-order":"order","fixture-product",prefix));}
  public String createOrder(CancellationOrder command){if(failTrade)throw new BookingFailure(503,"TRADE_UNAVAILABLE"); charges.incrementAndGet();return "SIMULATED:"+command.businessKey();}
  public RightsResult reserve(String source,long quantity,long version,String key){if(reserveBarrier!=null)try{reserveBarrier.await(5,TimeUnit.SECONDS);}catch(Exception e){throw new IllegalStateException("FIXTURE_BARRIER",e);}if(strictVersion&&version!=sourceVersion.get())throw new BookingFailure(409,"RIGHTS_VERSION_CONFLICT");return new RightsResult("reserve:"+key,strictVersion?sourceVersion.incrementAndGet():2);}
  public RightsResult consume(String ref,long quantity,long version,String key){if(strictVersion&&version!=sourceVersion.get())throw new BookingFailure(409,"RIGHTS_VERSION_CONFLICT");if(fail)throw new BookingFailure(503,"RIGHTS_UNAVAILABLE"); consumes.incrementAndGet();return new RightsResult("consume:"+key,strictVersion?sourceVersion.incrementAndGet():3);}
  public RightsResult release(String ref,long quantity,long version,String key){if(strictVersion&&version!=sourceVersion.get())throw new BookingFailure(409,"RIGHTS_VERSION_CONFLICT");return new RightsResult("release:"+key,strictVersion?sourceVersion.incrementAndGet():3);}
 };
 volatile boolean fail,revoked,unavailable,failTrade;
 volatile String merchant="fixture-merchant",productKind="COUNT";
 volatile boolean policyApproved=true,strictVersion,foreignCoach,swappedOrder;
 volatile CyclicBarrier reserveBarrier;
 final java.util.concurrent.atomic.AtomicLong sourceVersion=new java.util.concurrent.atomic.AtomicLong(1);
 final java.util.concurrent.atomic.AtomicInteger charges=new java.util.concurrent.atomic.AtomicInteger(),consumes=new java.util.concurrent.atomic.AtomicInteger();
 BookingService service(){return new BookingService(BookingDatabase.connect(MysqlBookingTest.URL,System.getenv("GYM_DB_USERNAME"),System.getenv("GYM_DB_PASSWORD")),fixture);}
 BookingApi.Context ctx(String actor,String key){return new BookingApi.Context(actor,UUID.randomUUID().toString(),prefix+key,"SIMULATED");}
 String session(){return service().schedule(ctx("staff","schedule"),new BookingApi.Schedule("store",prefix,"GROUP",Instant.parse("2027-01-01T10:00:00Z"),Instant.parse("2027-01-01T11:00:00Z"),1,"policy",1));}
 @Test void lastSeatRaceAndReplayAndDifferentBody() throws Exception {
  var s=service(); var id=session();
  try(var pool=Executors.newFixedThreadPool(2)) {
   var barrier=new CyclicBarrier(2);
   var tasks=new ArrayList<Future<Boolean>>();
   for(String actor:List.of("one","two"))tasks.add(pool.submit(()->{barrier.await();try{s.book(ctx(actor,"book"),new BookingApi.Book(id,"fixture-source",1,"order"));return true;}catch(BookingFailure e){assertThat(e.code()).isEqualTo("CAPACITY_FULL");return false;}}));
   assertThat(tasks.get(0).get()?1:0).isNotEqualTo(tasks.get(1).get()?1:0);
  }
 }
 @Test void failureRollsBackAndReconnectionFindsOnlyOneFulfilment() {
  var s=service(); var id=session();var context=ctx("one","book");
  var b=s.book(context,new BookingApi.Book(id,"fixture-source",1,"order"));
  assertThat(s.book(context,new BookingApi.Book(id,"fixture-source",1,"order"))).isEqualTo(b);
  assertThatThrownBy(()->s.book(context,new BookingApi.Book(id,"fixture-source",1,"changed"))).isInstanceOf(BookingFailure.class);
  fail=true;
  assertThatThrownBy(()->s.change(ctx("one","check"),b,0,"CHECK_IN")).isInstanceOf(BookingFailure.class);
  assertThat(service().find(ctx("one","read"),b).state()).isEqualTo("BOOKED");
  fail=false;s.change(ctx("one","check"),b,0,"CHECK_IN");
  assertThat(service().find(ctx("one","read2"),b).state()).isEqualTo("FULFILLED");
  assertThatThrownBy(()->s.change(ctx("one","cancel"),b,1,"CANCEL")).isInstanceOf(BookingFailure.class);
  assertThatThrownBy(()->s.find(ctx("denied","read"),b)).isInstanceOf(BookingFailure.class);
 }
 @Test void acceptedCancellationRunsTradeExactlyOnceAndReleasesCapacity() {
  var s=service();var id=session();var b=s.book(ctx("one","book"),new BookingApi.Book(id,"fixture-source",1,"order"));
  var cancel=ctx("one","cancel");
  s.change(cancel,b,0,"CANCEL");
  assertThat(s.change(cancel,b,0,"CANCEL")).isEqualTo(b); assertThat(charges.get()).isEqualTo(1);
  assertThat(service().find(ctx("one","read"),b).state()).isEqualTo("CANCELLED");
  assertThat(s.book(ctx("two","book2"),new BookingApi.Book(id,"fixture-source",1,"order"))).isNotBlank();
 }
 @Test void coachOverlapRejectsWhileTouchingBoundarySucceeds() {
  var s=service();session();
  assertThatThrownBy(()->s.schedule(ctx("staff","overlap"),new BookingApi.Schedule("store",prefix,"GROUP",Instant.parse("2027-01-01T10:30:00Z"),Instant.parse("2027-01-01T11:30:00Z"),1,"policy",1))).isInstanceOf(BookingFailure.class);
  assertThat(s.schedule(ctx("staff","adjacent"),new BookingApi.Schedule("store",prefix,"PRIVATE",Instant.parse("2027-01-01T11:00:00Z"),Instant.parse("2027-01-01T12:00:00Z"),1,"policy",1))).isNotBlank();
 } @Test void revokedPermissionsUnknownDependenciesAndForeignMerchantRejectCurrentReads() {
  var s=service();var id=session();var b=s.book(ctx("one","book"),new BookingApi.Book(id,"fixture-source",1,"order"));
  assertThatThrownBy(()->s.find(ctx("stranger","read"),b)).isInstanceOf(BookingFailure.class);
  revoked=true; assertThatThrownBy(()->s.book(ctx("one","book"),new BookingApi.Book(id,"fixture-source",1,"order"))).isInstanceOf(BookingFailure.class);revoked=false;
  unavailable=true;assertThatThrownBy(()->s.find(ctx("one","read"),b)).isInstanceOf(BookingFailure.class).hasMessageNotContaining("fixture-sensitive-marker");unavailable=false;
  merchant="other";assertThatThrownBy(()->s.find(ctx("one","read"),b)).isInstanceOf(BookingFailure.class);merchant="fixture-merchant";
  assertThat(s.find(ctx("one","read"),b).state()).isEqualTo("BOOKED");
 }
 @Test void tradeFailureCannotReleaseSeatOrCommitCancellation() {
  var s=service();var id=session();var b=s.book(ctx("one","book"),new BookingApi.Book(id,"fixture-source",1,"order"));
  failTrade=true;assertThatThrownBy(()->s.change(ctx("one","cancel"),b,0,"CANCEL")).isInstanceOf(BookingFailure.class);
  assertThat(service().find(ctx("one","read"),b).state()).isEqualTo("BOOKED");
  assertThatThrownBy(()->s.book(ctx("two","book2"),new BookingApi.Book(id,"fixture-source",1,"order"))).isInstanceOf(BookingFailure.class);
  assertThat(charges.get()).isZero();
 }
 @Test void cancelAndCheckInRaceHasExactlyOneTerminalWinner() throws Exception {
  var s=service();var id=session();var b=s.book(ctx("one","book"),new BookingApi.Book(id,"fixture-source",1,"order"));
  try(var pool=Executors.newFixedThreadPool(2)) {
   var barrier=new CyclicBarrier(2);var results=new ArrayList<Future<Boolean>>();
   for(String command:List.of("CHECK_IN","CANCEL"))results.add(pool.submit(()->{barrier.await();try{s.change(ctx("one",command),b,0,command);return true;}catch(BookingFailure e){assertThat(e.status()).isEqualTo(409);return false;}}));
   assertThat(results.get(0).get()).isNotEqualTo(results.get(1).get());
   assertThat(charges.get()+consumes.get()).isEqualTo(1);
   assertThat(s.find(ctx("one","read"),b).version()).isEqualTo(1);
  }
 } @Test void sessionPaginationBindsActorAndStore() {
  var s=service();session();
  s.schedule(ctx("staff","next"),new BookingApi.Schedule("store",prefix,"GROUP",Instant.parse("2027-01-01T11:00:00Z"),Instant.parse("2027-01-01T12:00:00Z"),2,"policy",1));
  var page=s.listSessions(ctx("one","list"),"store",null,1);
  assertThat(page.items()).hasSize(1);assertThat(page.nextCursor()).isNotNull();
  assertThatThrownBy(()->s.listSessions(ctx("two","list"),"store",page.nextCursor(),1)).isInstanceOf(BookingFailure.class);
  var second=s.listSessions(ctx("one","list2"),"store",page.nextCursor(),1);
  assertThat(second.items()).hasSize(1);assertThat(second.items().getFirst().id()).isNotEqualTo(page.items().getFirst().id());
 } @Test void unapprovedPolicyIsRejectedAndTermPrivatePackageCanFulfil() {
  policyApproved=false;assertThatThrownBy(()->session()).isInstanceOf(BookingFailure.class);
  policyApproved=true;productKind="TERM";var s=service();
  var id=s.schedule(ctx("staff","private"),new BookingApi.Schedule("store",prefix,"PRIVATE",Instant.parse("2027-01-01T10:00:00Z"),Instant.parse("2027-01-01T11:00:00Z"),1,"policy",1));
  var b=s.book(ctx("one","book"),new BookingApi.Book(id,"fixture-source",1,"order"));
  var check=ctx("one","check");s.change(check,b,0,"CHECK_IN");s.change(check,b,0,"CHECK_IN");
  assertThat(consumes.get()).isEqualTo(1);assertThat(s.find(ctx("one","read"),b).state()).isEqualTo("FULFILLED");
 } @Test void sameMemberCannotOccupyTwoSeatsUsingDifferentKeys() {
  var s=service();var id=s.schedule(ctx("staff","double"),new BookingApi.Schedule("store",prefix,"GROUP",Instant.parse("2027-01-01T10:00:00Z"),Instant.parse("2027-01-01T11:00:00Z"),2,"policy",1));
  s.book(ctx("one","first"),new BookingApi.Book(id,"fixture-source",1,"order"));
  assertThatThrownBy(()->s.book(ctx("one","second"),new BookingApi.Book(id,"fixture-source",1,"order"))).isInstanceOf(BookingFailure.class);
 } @Test void reserveVersionMustBeCarriedIntoFulfilment() {
  strictVersion=true;var s=service();var id=session();
  var b=s.book(ctx("one","book"),new BookingApi.Book(id,"fixture-source",1,"order"));
  s.change(ctx("one","check"),b,0,"CHECK_IN");
  assertThat(s.find(ctx("one","read"),b).state()).isEqualTo("FULFILLED");
 }
 @Test void foreignCoachCannotBeScheduled() {
  foreignCoach=true;assertThatThrownBy(()->session()).isInstanceOf(BookingFailure.class);
 }
 @Test void inactiveActorCannotProbeWhetherBookingExists() {
  assertThatThrownBy(()->service().find(ctx("inactive","probe"),"does-not-exist")).isInstanceOf(BookingFailure.class).satisfies(e->assertThat(((BookingFailure)e).status()).isEqualTo(401));
 } @Test void swappedOrderAndRightsSourceRejectBeforeReservation() {
  swappedOrder=true;var s=service();var id=session();
  assertThatThrownBy(()->s.book(ctx("one","book"),new BookingApi.Book(id,"fixture-source",1,"order"))).isInstanceOf(BookingFailure.class);
 }
 @Test void anotherReservationMustNotStaleEarlierFulfilment() {
  strictVersion=true;var s=service();var first=session();
  var a=s.book(ctx("one","bookA"),new BookingApi.Book(first,"fixture-source",1,"order"));
  var second=s.schedule(ctx("staff","second"),new BookingApi.Schedule("store",prefix,"GROUP",Instant.parse("2027-01-02T10:00:00Z"),Instant.parse("2027-01-02T11:00:00Z"),1,"policy",1));
  var b=s.book(ctx("one","bookB"),new BookingApi.Book(second,"fixture-source",2,"order"));
  s.change(ctx("one","checkA"),a,0,"CHECK_IN");s.change(ctx("one","checkB"),b,0,"CHECK_IN");
  assertThat(sourceVersion.get()).isEqualTo(5);assertThat(consumes.get()).isEqualTo(2);
 }
 @Test void sessionQueryReturnsCoachTimeAndPolicy() {
  var s=service();var start=Instant.parse("2027-01-03T10:00:00Z");
  var id=s.schedule(ctx("staff","details"),new BookingApi.Schedule(prefix,prefix,"GROUP",start,start.plusSeconds(3600),1,"policy",1));
  var details=s.listSessions(ctx("one","details"),prefix,null,10).items().getFirst();
  assertThat(details.id()).isEqualTo(id);assertThat(details.coachId()).isEqualTo(prefix);
  assertThat(details.start()).isEqualTo(start);assertThat(details.end()).isEqualTo(start.plusSeconds(3600));assertThat(details.policyId()).isEqualTo("policy");
 } @Test void receiptConflictAfterWritesRollsBackBookingSeatAndOutbox() throws Exception {
  var s=service();var start=Instant.parse("2027-01-05T10:00:00Z");
  var a=s.schedule(ctx("staff","a"),new BookingApi.Schedule(prefix,prefix,"GROUP",start,start.plusSeconds(3600),1,"policy",1));
  var b=s.schedule(ctx("staff","b"),new BookingApi.Schedule(prefix,prefix,"GROUP",start.plusSeconds(3600),start.plusSeconds(7200),1,"policy",1));
  reserveBarrier=new CyclicBarrier(2);
  try(var pool=Executors.newFixedThreadPool(2)) {
   var results=new ArrayList<Future<Boolean>>();
   for(String id:List.of(a,b))results.add(pool.submit(()->{try{s.book(ctx("one","shared"),new BookingApi.Book(id,"fixture-source",1,"order"));return true;}catch(BookingFailure e){assertThat(e.status()).isEqualTo(409);return false;}}));
   assertThat(results.get(0).get()).isNotEqualTo(results.get(1).get());
  }
  var db=new org.springframework.jdbc.core.JdbcTemplate(BookingDatabase.connect(MysqlBookingTest.URL,System.getenv("GYM_DB_USERNAME"),System.getenv("GYM_DB_PASSWORD")));
  assertThat(db.queryForObject("SELECT SUM(reserved) FROM bkg_sessions WHERE store_id=?",Long.class,prefix)).isEqualTo(1);
  assertThat(db.queryForObject("SELECT COUNT(*) FROM bkg_bookings WHERE session_id IN (?,?)",Long.class,a,b)).isEqualTo(1);
  assertThat(db.queryForObject("SELECT COUNT(*) FROM bkg_outbox WHERE store_id=?",Long.class,prefix)).isEqualTo(3);
  assertThat(db.queryForObject("SELECT COUNT(*) FROM bkg_receipts WHERE actor_id='one' AND operation='book' AND idem_key=?",Long.class,prefix+"shared")).isEqualTo(1);
 } @Test void recordDisplayTextCannotCollideForIdempotency() {
  var s=service();var start=Instant.parse("2027-01-06T10:00:00Z");var c=ctx("staff","hash");
  var tag=prefix.substring(0,8);
  var first=new BookingApi.Schedule(tag+", coachId="+tag+"x",tag+"y","GROUP",start,start.plusSeconds(3600),1,"policy",1);
  var second=new BookingApi.Schedule(tag,tag+"x, coachId="+tag+"y","GROUP",start,start.plusSeconds(3600),1,"policy",1);
  assertThat(first.toString()).isEqualTo(second.toString());
  s.schedule(c,first);
  assertThatThrownBy(()->s.schedule(c,second)).isInstanceOf(BookingFailure.class);
 }}
