package com.gym.modules.operationscore;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import com.gym.modules.operationscore.api.*;
import com.gym.modules.operationscore.api.Models.*;
import com.gym.modules.operationscore.ports.Dependencies.*;
import com.gym.modules.operationscore.persistence.*;
import com.gym.modules.operationscore.application.*;
class OperationsTest {
 static Repository r; static String prefix=UUID.randomUUID().toString();
 final AtomicReference<Decision> decision=new AtomicReference<>(Decision.ALLOW);
 final String store="s"+UUID.randomUUID(), actor="u"+UUID.randomUUID();
 final Instant now=Instant.parse("2026-11-13T10:00:00Z");
 Access access; EquipmentService equipment; ProjectionService projection; ReminderService reminders;
 @BeforeAll static void database()throws Exception{r=new Repository(Database.migrate(System.getenv("GYM_TEST_DB_URL"),System.getenv("GYM_DB_USERNAME"),System.getenv("GYM_DB_PASSWORD")));}
 @BeforeEach void setup(){
  access=new Access((a,b,c,d,e)->decision.get(),s->new Store(s,s.startsWith("other")?"other":"merchant","ACTIVE",1),a->new Account(a,1));
  equipment=new EquipmentService(r,access,(a,s,c)->decision.get(),(a,s,i)->i.startsWith("private-")?Decision.ALLOW:Decision.DENY,Clock.fixed(now,ZoneOffset.UTC));
  projection=new ProjectionService(r,access,s->new MetricPolicy("fixture-snapshot-v1",1,Approval.CONFIGURED_FIXTURE),Clock.fixed(now,ZoneOffset.UTC));
 }
 Context c(String key){return new Context(actor,"req",key);}
 String create(){return equipment.create(c(UUID.randomUUID().toString()),store,"Rack");}
 @Test void repairHistoryPrivateImagesAndReplay(){
  String id=create();String report=equipment.report(c("report"),id,"Cable broken","private-fixture");
  assertEquals(report,equipment.report(c("report"),id,"Cable broken","private-fixture"));
  failCode(409,()->equipment.report(c("report"),id,"Different","private-fixture"));
  failCode(403,()->equipment.report(c("bad-image"),id,"Broken","https-public"));
  equipment.change(c("fault"),id,0,"FAULT","inspection");equipment.change(c("repair"),id,1,"MAINTENANCE","repair");equipment.change(c("done"),id,2,"AVAILABLE","tested");
  assertEquals(4,equipment.history(c("q"),id,null,100).size());assertEquals(3,equipment.get(c("q"),id).version());
  assertEquals(3,r.count("select count(*) from ops_outbox where aggregate_id=? and type='EquipmentStateChanged'",id));
 }
 @Test void currentAuthorizationUnknownDependencyAndCursorScope(){String id=create();decision.set(Decision.DENY);failCode(403,()->equipment.get(c("q"),id));decision.set(Decision.UNAVAILABLE);failCode(503,()->equipment.change(c("f"),id,0,"FAULT","x"));decision.set(Decision.ALLOW);assertEquals(0,equipment.get(c("q"),id).version());failCode(404,()->equipment.list(c("q"),store,new Cursor(actor,"other","equipment",""),10));}
 @Test void optimisticConcurrencyHasOneWinner()throws Exception{String id=create();var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);try{var jobs=new ArrayList<Future<Boolean>>();for(int n=0;n<2;n++){final int k=n;jobs.add(pool.submit(()->{start.await();try{equipment.change(c("race"+k),id,0,"FAULT","x");return true;}catch(Failure f){assertEquals(409,f.status);return false;}}));}start.countDown();int wins=0;for(var job:jobs)if(job.get())wins++;assertEquals(1,wins);assertEquals(1,equipment.get(c("q"),id).version());}finally{pool.shutdownNow();}}
 Event event(String id,String type,String aggregate,long version,long amount,boolean present,boolean reliable){return new Event(id,type,1,type.equals("VISIT")?"ranking-core":"trade-core",aggregate,version,now,"merchant",store,"SIMULATED",actor,amount,present,reliable);}
 @Test void projectionDeduplicatesMoneyAndLabelsAttendance(){String order=UUID.randomUUID().toString();var paid=event(UUID.randomUUID().toString(),"RECEIVED",order,1,10000,false,true);projection.accept(c("p"),paid);projection.accept(c("p"),paid);projection.accept(c("r"),event(UUID.randomUUID().toString(),"REFUNDED",UUID.randomUUID().toString(),1,3000,false,true));projection.accept(c("v"),event(UUID.randomUUID().toString(),"VISIT",UUID.randomUUID().toString(),1,0,true,false));var m=projection.metrics(c("q"),store,now.minusSeconds(1),ZoneOffset.UTC,"SIMULATED");assertEquals(10000,m.receivedMinor());assertEquals(3000,m.refundedMinor());assertEquals(7000,m.netMinor());assertEquals(1,m.todayVisitors());assertEquals(1,m.presentCount());assertTrue(m.estimated());failCode(409,()->projection.accept(c("x"),event(paid.eventId(),"RECEIVED",order,1,10001,false,true)));}
 @Test void reconnectAndRepeatMigrationPreserveRows()throws Exception{String id=create();var ds=Database.migrate(System.getenv("GYM_TEST_DB_URL"),System.getenv("GYM_DB_USERNAME"),System.getenv("GYM_DB_PASSWORD"));assertEquals(1,new Repository(ds).count("select count(*) from ops_equipment where id=?",id));assertThrows(IllegalArgumentException.class,()->Database.migrate("jdbc:mysql://localhost:3306/gym_dev","x","x"));}
 @Test void transactionFailureLeavesNoStateReceiptOrOutbox(){String id=create();assertThrows(RuntimeException.class,()->r.transaction(()->{equipment.change(c("rollback"),id,0,"FAULT","x");throw new IllegalStateException("injected");}));assertEquals(0,equipment.get(c("q"),id).version());assertEquals(0,r.count("select count(*) from ops_outbox where aggregate_id=? and type='EquipmentStateChanged'",id));equipment.change(c("rollback"),id,0,"FAULT","x");assertEquals(1,equipment.get(c("q"),id).version());}
 @Test void policyConsentUnsubscribeFrequencyAndOwnMessages(){
  var fact=new AtomicReference<>(new ContactFact(actor,store,1,now.plusSeconds(60),now.minusSeconds(1000),true,false));
  var policy=new AtomicReference<>(new Policy("fixture",1,Approval.UNAPPROVED,Duration.ofSeconds(100),Duration.ofSeconds(500),Duration.ofSeconds(300)));
  reminders=new ReminderService(r,access,(a,s)->fact.get(),(id,v)->policy.get(),Clock.fixed(now,ZoneOffset.UTC));
  failCode(422,()->reminders.evaluate(c("e"),store,actor,"EXPIRY","fixture",1));assertEquals(0,reminders.messages(c("q"),null,10).size());
  policy.set(new Policy("fixture",1,Approval.CONFIGURED_FIXTURE,Duration.ofSeconds(100),Duration.ofSeconds(500),Duration.ofSeconds(300)));
  String id=reminders.evaluate(c("e"),store,actor,"EXPIRY","fixture",1);assertNotNull(id);failCode(409,()->reminders.evaluate(c("e"),store,actor,"SILENCE","fixture",1));assertEquals(id,reminders.evaluate(c("e"),store,actor,"EXPIRY","fixture",1));
  assertNull(reminders.evaluate(c("silent"),store,actor,"SILENCE","fixture",1));assertEquals(1,reminders.messages(c("q"),null,10).size());
  fact.set(new ContactFact(actor,store,2,now.plusSeconds(30),now.minusSeconds(1000),true,true));assertNull(reminders.evaluate(c("unsub"),store,actor,"EXPIRY","fixture",1));
  fact.set(new ContactFact(actor,store,3,now.plusSeconds(30),now.minusSeconds(1000),false,false));assertNull(reminders.evaluate(c("noconsent"),store,actor,"SILENCE","fixture",1));
  assertEquals(0,reminders.messages(new Context("other","req","k"),null,10).size());
 }
 @Test void futureVisitCannotAppearInCurrentStatistics(){var future=new Event(UUID.randomUUID().toString(),"VISIT",1,"ranking-core",UUID.randomUUID().toString(),1,now.plusSeconds(3600),"merchant",store,"SIMULATED",actor,0,true,true);projection.accept(c("future"),future);var m=projection.metrics(c("q"),store,now.minusSeconds(1),ZoneOffset.UTC,"SIMULATED");assertEquals(0,m.presentCount());assertEquals(0,m.todayVisitors());}
 @Test void scopeIsResolvedFromPersistedEquipment(){String foreign=UUID.randomUUID().toString();r.db.update("insert into ops_equipment values(?,?,'Foreign','AVAILABLE',0)",foreign,"other-"+store);var restricted=new Access((a,b,m,s,o)->"merchant".equals(m)?Decision.ALLOW:Decision.DENY,s->new Store(s,s.startsWith("other")?"other":"merchant","ACTIVE",1),a->new Account(a,1));var service=new EquipmentService(r,restricted,(a,s,c)->Decision.ALLOW,(a,s,i)->Decision.ALLOW,Clock.fixed(now,ZoneOffset.UTC));failCode(403,()->service.get(c("foreign"),foreign));assertEquals(0,r.count("select count(*) from ops_report where equipment_id=?",foreign));}
 @Test void metricDefinitionsHaveNoRuntimeDefault(){var service=new ProjectionService(r,access);failCode(422,()->service.metrics(c("q"),store,now.minusSeconds(1),now,ZoneOffset.UTC,"SIMULATED"));}
 @Test void reminderRechecksDueFactAfterFence(){var calls=new java.util.concurrent.atomic.AtomicInteger();var service=new ReminderService(r,access,(a,st)->new ContactFact(a,st,2,now.plusSeconds(calls.incrementAndGet()==1?30:10000),now,true,false),(id,v)->new Policy(id,v,Approval.CONFIGURED_FIXTURE,Duration.ofSeconds(100),Duration.ofSeconds(500),Duration.ofSeconds(300)),Clock.fixed(now,ZoneOffset.UTC));assertNull(service.evaluate(c("refreshed"),store,actor,"EXPIRY","fixture",1));assertEquals(0,service.messages(c("q"),null,10).size());}
 @Test void sourceSnapshotsRejectSameVersionConflictAndIgnoreOlder(){String id=UUID.randomUUID().toString();projection.accept(c("v2"),event(UUID.randomUUID().toString(),"RECEIVED",id,2,10000,false,true));projection.accept(c("v1"),event(UUID.randomUUID().toString(),"RECEIVED",id,1,5000,false,true));failCode(409,()->projection.accept(c("conflict"),event(UUID.randomUUID().toString(),"RECEIVED",id,2,20000,false,true)));assertEquals(10000,projection.metrics(c("q"),store,now.minusSeconds(1),ZoneOffset.UTC,"SIMULATED").receivedMinor());}
 @Test void reportReadReauthorizesPrivateReference(){String id=create();equipment.report(c("report"),id,"Cable broken","private-fixture");assertEquals(1,equipment.reports(c("q"),id,null,10).size());var deniedImages=new EquipmentService(r,access,(a,s,cap)->Decision.ALLOW,(a,s,i)->Decision.DENY,Clock.fixed(now,ZoneOffset.UTC));failCode(403,()->deniedImages.reports(c("q"),id,null,10));decision.set(Decision.DENY);failCode(403,()->equipment.reports(c("q"),id,null,10));}
 @Test void databaseExceptionsNeverExposeSql(){String id=create();Failure f=assertThrows(Failure.class,()->r.transaction(()->{r.db.update("insert into ops_equipment values(?,?,'secret fixture','AVAILABLE',0)",id,store);return null;}));assertEquals(409,f.status);assertFalse(f.getMessage().contains("secret"));assertNull(f.getCause());}
 @Test void unsupportedHistoricalSnapshotsAreExplicit(){failCode(422,()->projection.metrics(c("q"),store,now.minusSeconds(10),now.minusSeconds(1),ZoneOffset.UTC,"SIMULATED"));}
 @Test void defaultDenyAndHistoryCursorRemainScoped(){String id=create();var deny=new Access(null,s->new Store(s,"merchant","ACTIVE",1),a->new Account(a,1));var service=new EquipmentService(r,deny,(a,s,c)->Decision.ALLOW,(a,s,i)->Decision.ALLOW,Clock.fixed(now,ZoneOffset.UTC));failCode(403,()->service.get(c("q"),id));failCode(404,()->equipment.history(c("q"),id,new Cursor("other",id,"history",""),10));failCode(422,()->equipment.history(c("q"),id,null,101));assertEquals(1,equipment.history(c("q"),id,null,1).size());}
 @Test void concurrentSameKeyCannotCreateTwoArchives()throws Exception{var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);try{var jobs=new ArrayList<Future<String>>();for(int n=0;n<2;n++)jobs.add(pool.submit(()->{start.await();try{return equipment.create(c("same-key"),store,"Same");}catch(Failure f){assertEquals(409,f.status);return null;}}));start.countDown();String winner=null;for(var j:jobs){String result=j.get();if(result!=null){if(winner!=null)assertEquals(winner,result);winner=result;}}assertNotNull(winner);assertEquals(winner,equipment.create(c("same-key"),store,"Same"));assertEquals(1,r.count("select count(*) from ops_equipment where store_id=?",store));}finally{pool.shutdownNow();}}
 @Test void frequencyFenceSerializesDifferentReminderKinds()throws Exception{reminders=new ReminderService(r,access,(a,s)->new ContactFact(a,s,1,now.plusSeconds(30),now.minusSeconds(1000),true,false),(id,v)->new Policy(id,v,Approval.CONFIGURED_FIXTURE,Duration.ofSeconds(100),Duration.ofSeconds(500),Duration.ofSeconds(300)),Clock.fixed(now,ZoneOffset.UTC));var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);try{var jobs=new ArrayList<Future<String>>();for(String kind:List.of("EXPIRY","SILENCE"))jobs.add(pool.submit(()->{start.await();try{return reminders.evaluate(c(kind),store,actor,kind,"fixture",1);}catch(Failure f){assertEquals(409,f.status);return null;}}));start.countDown();for(var j:jobs)j.get();assertEquals(1,reminders.messages(c("q"),null,10).size());}finally{pool.shutdownNow();}}
 @Test void migrationEvidence(){var history=r.row("select version,checksum,success from flyway_schema_history where version='6'");assertNotNull(history);assertEquals("6",history.get("version"));System.out.println("V6_CHECKSUM="+history.get("checksum"));System.out.println("MYSQL_VERSION="+r.db.queryForObject("select version()",String.class));}
 static void failCode(int code,Runnable work){assertEquals(code,assertThrows(Failure.class,work::run).status);}
}
