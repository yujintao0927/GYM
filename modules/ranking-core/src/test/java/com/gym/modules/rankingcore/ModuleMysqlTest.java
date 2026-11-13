package com.gym.modules.rankingcore;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.gym.modules.rankingcore.persistence.Database;
import com.gym.modules.rankingcore.application.VisitService;
import com.gym.modules.rankingcore.ports.Dependencies;
import com.gym.modules.rankingcore.api.Failure;
import java.time.*;
import java.util.*;
class ModuleMysqlTest {
 static Database db;
 static final String subject=UUID.randomUUID().toString();
 static final Instant now=Instant.parse("2026-11-13T10:00:00Z");
 @BeforeAll static void setup(){db=MysqlFixture.open();}
 Dependencies deps(boolean allow){return new Dependencies(){
 public void active(String id){if(id==null)throw new Failure(401,"SUBJECT_INVALID");}
 public Store store(String id){return new Store(id,"merchant",1);}
 public Decision authorize(String a,String action,Store store,String owner){return allow?Decision.ALLOW:Decision.DENY;}
 public Right right(String owner,String store,String capability){if(store!=null&&!store.equals("store"))throw new Failure(404,"NOT_FOUND");return new Right("right",7);}
 public String consume(java.sql.Connection c,Right r,String owner,String store,String key,Instant at){return "fixture-consumption";}
 };}
 VisitService service(boolean allow){return new VisitService(db,deps(allow),Clock.fixed(now,ZoneOffset.UTC));}
 @Test void migrationReplaysAndGuardRejectsWrongDatabase(){assertEquals(0,db.migrate());assertThrows(IllegalArgumentException.class,()->Database.open("jdbc:mysql://localhost:3306/gym_dev", "x","x"));}
 @Test void manualEntryReplayExitAndReconnection(){var s=service(true);String key=UUID.randomUUID().toString();var v=s.enter(subject,subject,"store",key,null,"fixture front desk",7);assertEquals(v,s.enter(subject,subject,"store",key,null,"fixture front desk",7));assertThrows(Failure.class,()->s.enter(subject,subject,"store",UUID.randomUUID().toString(),null,"other",7));assertThrows(Failure.class,()->service(false).read(subject,v.id()));var out=s.exit(subject,v.id(),UUID.randomUUID().toString(),0);assertNotNull(out.leftAt());var re=MysqlFixture.open();assertEquals(out,new VisitService(re,deps(true),Clock.fixed(now,ZoneOffset.UTC)).read(subject,v.id()));}
 @Test void crossStoreDeniedWithoutReceipt(){assertThrows(Failure.class,()->service(true).enter(subject,subject,"foreign",UUID.randomUUID().toString(),null,"fixture",7));}
 @Test void rollbackAfterRightsFailureLeavesNoVisit(){String user=UUID.randomUUID().toString();var d=new Dependencies(){public void active(String s){} public Store store(String s){return new Store(s,"m",1);}public Decision authorize(String a,String b,Store s,String o){return Decision.ALLOW;}public Right right(String a,String s,String c){return new Right("r",1);}public String consume(java.sql.Connection c,Right r,String a,String s,String k,Instant i){throw new Failure(503,"DEPENDENCY_UNAVAILABLE");}};assertThrows(Failure.class,()->new VisitService(db,d,Clock.fixed(now,ZoneOffset.UTC)).enter(user,user,"store",UUID.randomUUID().toString(),null,"fixture",1));db.transaction(c->{try(var q=c.prepareStatement("SELECT count(*) FROM vis_visit WHERE subject_id=?")){q.setString(1,user);try(var r=q.executeQuery()){r.next();assertEquals(0,r.getInt(1));}}return null;});}
 @Test void concurrentEntryConsumesOnce() throws Exception {
 String user=UUID.randomUUID().toString();var ready=new java.util.concurrent.CountDownLatch(2);var go=new java.util.concurrent.CountDownLatch(1);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
 try{var tasks=new java.util.ArrayList<java.util.concurrent.Future<Boolean>>();for(int n=0;n<2;n++)tasks.add(pool.submit(()->{ready.countDown();go.await();try{service(true).enter(user,user,"store",UUID.randomUUID().toString(),null,"fixture",7);return true;}catch(Failure f){assertEquals(409,f.status());return false;}}));assertTrue(ready.await(5,java.util.concurrent.TimeUnit.SECONDS));go.countDown();int won=0;for(var f:tasks)if(f.get(10,java.util.concurrent.TimeUnit.SECONDS))won++;assertEquals(1,won);}finally{pool.shutdownNow();}
 }
 @Test void qrBoundToStoreAndOneUse(){String user=UUID.randomUUID().toString();var s=service(true);String qr=s.challenge(user,"store",now.plusSeconds(60));var v=s.enter(user,user,"store",UUID.randomUUID().toString(),qr,null,7);s.exit(user,v.id(),UUID.randomUUID().toString(),0);assertThrows(Failure.class,()->s.enter(user,user,"store",UUID.randomUUID().toString(),qr,null,7));}
 @Test void correctionRequiresVersionAndReason(){String user=UUID.randomUUID().toString();var s=service(true);var v=s.enter(user,user,"store",UUID.randomUUID().toString(),null,"fixture",7);var fixed=s.correct(user,v.id(),0,now.plusSeconds(5),"fixture missing exit");assertEquals(now.plusSeconds(5),fixed.leftAt());assertThrows(Failure.class,()->s.correct(user,v.id(),0,now.plusSeconds(6),"fixture stale"));}

 @Test void concurrentExitAndCorrectionHaveOneWinner() throws Exception {String user=UUID.randomUUID().toString();var service=service(true);var visit=service.enter(user,user,"store",UUID.randomUUID().toString(),null,"fixture",7);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);var start=new java.util.concurrent.CountDownLatch(1);try{var a=pool.submit(()->{start.await();try{service.exit(user,visit.id(),UUID.randomUUID().toString(),0);return true;}catch(Failure f){assertEquals(409,f.status());return false;}});var b=pool.submit(()->{start.await();try{service.correct(user,visit.id(),0,now.plusSeconds(1),"fixture correction");return true;}catch(Failure f){assertEquals(409,f.status());return false;}});start.countDown();assertNotEquals(a.get(10,java.util.concurrent.TimeUnit.SECONDS),b.get(10,java.util.concurrent.TimeUnit.SECONDS));assertEquals(1,service.read(user,visit.id()).version());}finally{pool.shutdownNow();}}
 @Test void receiptRemainsOriginalAfterExit(){String user=UUID.randomUUID().toString();var s=service(true);String key=UUID.randomUUID().toString();var visit=s.enter(user,user,"store",key,null,"fixture",7);s.exit(user,visit.id(),UUID.randomUUID().toString(),0);assertEquals(visit,s.enter(user,user,"store",key,null,"fixture",7));assertFalse(s.list(user,user,"store",null,10).isEmpty());}

 @Test void recordsDatabaseAndMigrationEvidence(){db.transaction(c->{System.out.println("RNK_EVIDENCE MySQL="+Database.scalar(c,"SELECT VERSION()")+" V8-checksum="+Database.scalar(c,"SELECT checksum FROM flyway_schema_history WHERE version='8'"));return null;});}

 @Test void sameKeyDifferentBodyConflicts(){String user=UUID.randomUUID().toString();String key=UUID.randomUUID().toString();service(true).enter(user,user,"store",key,null,"fixture first",7);Failure failure=assertThrows(Failure.class,()->service(true).enter(user,user,"store",key,null,"fixture altered",7));assertEquals(409,failure.status());}
 @Test void expiredChallengeLeavesNoEntry(){String user=UUID.randomUUID().toString();String challenge=service(true).challenge(user,"store",now.plusSeconds(1));var later=new VisitService(db,deps(true),Clock.fixed(now.plusSeconds(2),ZoneOffset.UTC));Failure failure=assertThrows(Failure.class,()->later.enter(user,user,"store",UUID.randomUUID().toString(),challenge,null,7));assertEquals(409,failure.status());assertTrue(later.list(user,user,"store",null,10).isEmpty());}
 @Test void unavailableAuthorizationFailsClosedWithNoWrites(){String user=UUID.randomUUID().toString();var d=new Dependencies(){public void active(String s){}public Store store(String s){return new Store(s,"other-merchant",1);}public Decision authorize(String a,String action,Store store,String owner){return Decision.UNAVAILABLE;}};Failure failure=assertThrows(Failure.class,()->new VisitService(db,d,Clock.fixed(now,ZoneOffset.UTC)).enter(user,user,"store",UUID.randomUUID().toString(),null,"fixture",1));assertEquals(503,failure.status());assertEquals("ACCESS_DENIED",failure.getMessage());assertTrue(service(true).list(user,user,"store",null,10).isEmpty());}
 @Test void deniedMerchantCannotCreateVisit(){String user=UUID.randomUUID().toString();Failure failure=assertThrows(Failure.class,()->service(false).enter(user,user,"other-store",UUID.randomUUID().toString(),null,"fixture",7));assertEquals(403,failure.status());assertTrue(service(true).list(user,user,"other-store",null,10).isEmpty());}

}
