package com.gym.modules.rankingcore;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.gym.modules.rankingcore.persistence.Database;
import com.gym.modules.rankingcore.application.RankingService;
import com.gym.modules.rankingcore.ports.*;
import com.gym.modules.rankingcore.api.Failure;
import java.time.*;
import java.util.*;
class RankingMysqlTest {
 @Test void braceletReplayPolicyGateDailyDedupAndOptIn(){
 var db=MysqlFixture.open();
 String u=UUID.randomUUID().toString();var d=new Dependencies(){public void active(String s){}public Decision authorize(String a,String b,Store s,String o){return Decision.ALLOW;}public void requireSimulatedDevice(String owner,String device){if(!device.equals("fixture-device")||!owner.equals(u))throw new Failure(404,"DEVICE_NOT_FOUND");}public Right right(String a,String s,String c){return new Right("fixture",1);}};
 var t=Instant.parse("2026-11-13T00:00:00Z");var s=new RankingService(db,d,()->null,Clock.fixed(t.plusSeconds(3600),ZoneOffset.UTC));
 assertThrows(Failure.class,()->s.bracelet(u,UUID.randomUUID().toString(),u,"unbound-device",t,t.plusSeconds(2)));
 assertThrows(Failure.class,()->s.bracelet(u,"visit:reserved:0",u,"fixture-device",t,t.plusSeconds(2)));
 assertThrows(Failure.class,()->s.bracelet(u,UUID.randomUUID().toString(),"other-subject","fixture-device",t,t.plusSeconds(2)));
 String e=UUID.randomUUID().toString();assertEquals("PENDING_POLICY",s.bracelet(u,e,u,"fixture-device",t,t.plusSeconds(60)));
 assertEquals("PENDING_POLICY",s.bracelet(u,e,u,"fixture-device",t,t.plusSeconds(60)));
 assertThrows(Failure.class,()->s.bracelet(u,e,u,"fixture-device",t,t.plusSeconds(61)));
 assertThrows(Failure.class,()->s.board(u,"PERSONAL",null,"COUNTRY",null,null,null,10,0));
 var fixture=new RankingPolicy.Config(RankingPolicy.Status.CONFIGURED_FIXTURE,1000L,ZoneOffset.UTC,1);
 var f=new RankingService(db,d,()->fixture,Clock.fixed(t.plusSeconds(3600),ZoneOffset.UTC));
 assertEquals("VALID",f.bracelet(u,UUID.randomUUID().toString(),u,"fixture-device",t,t.plusSeconds(2)));
 assertTrue(f.board(u,"PERSONAL",null,"COUNTRY",null,null,null,100,0).stream().noneMatch(r->r.nickname().equals("fixture-"+u)));
 f.participate(u,"PERSONAL",null,true,"fixture-"+u,-1);
 f.bracelet(u,UUID.randomUUID().toString(),u,"fixture-device",t,t.plusSeconds(3));
 var rows=f.board(u,"PERSONAL",null,"COUNTRY",null,null,null,10,0);assertEquals(1,rows.stream().filter(r->r.nickname().equals("fixture-"+u)).findFirst().orElseThrow().days());
 }
 @Test void regionNeedsConsentAndMonthlyChangeLimit(){var db=MysqlFixture.open();String u=UUID.randomUUID().toString();var d=new Dependencies(){public void active(String x){}public Decision authorize(String a,String b,Store s,String o){return Decision.ALLOW;}public void requireSimulatedDevice(String owner,String device){if(!device.equals("fixture-device")||!owner.equals(u))throw new Failure(404,"DEVICE_NOT_FOUND");}public Right right(String a,String s,String c){return new Right("fixture",1);}};var clock=Clock.fixed(Instant.parse("2026-11-13T00:00:00Z"),ZoneOffset.UTC);var fixture=new RankingPolicy.Config(RankingPolicy.Status.CONFIGURED_FIXTURE,1000L,ZoneOffset.UTC,1);var s=new RankingService(db,d,()->fixture,clock);var regions=new RegionResolver(){public Region resolve(String actor,String ref){if(!ref.equals("fixture-consent"))throw new Failure(403,"CONSENT_REQUIRED");return new Region("P1","C1","D1");}};assertThrows(Failure.class,()->s.region(u,"refused",regions));assertEquals("D1",s.region(u,"fixture-consent",regions).district());assertThrows(Failure.class,()->s.region(u,"fixture-consent",regions));}

 @Test void storeVisitAndAppealProjection(){var db=MysqlFixture.open();String u=UUID.randomUUID().toString();var d=new Dependencies(){public void active(String s){}public Store store(String s){return new Store(s,"fixture-merchant",1);}public Decision authorize(String a,String b,Store s,String owner){return Decision.ALLOW;}public Right right(String a,String s,String cap){return new Right("fixture-right",1);}public String consume(java.sql.Connection c,Right r,String a,String s,String k,Instant at){return "fixture-consumption";}};Instant now=Instant.parse("2026-11-13T00:00:00Z");var v=new com.gym.modules.rankingcore.application.VisitService(db,d,Clock.fixed(now,ZoneOffset.UTC));var visit=v.enter(u,u,"fixture-store",UUID.randomUUID().toString(),null,"fixture",1);v.correct(u,visit.id(),0,now.plusSeconds(10),"fixture missing exit");var ranking=new RankingService(db,d,()->new RankingPolicy.Config(RankingPolicy.Status.CONFIGURED_FIXTURE,1000,ZoneOffset.UTC,1),Clock.fixed(now.plusSeconds(20),ZoneOffset.UTC));assertEquals("VALID",ranking.visit(u,visit.id()));assertEquals("VALID",ranking.visit(u,visit.id()));ranking.participate(u,"STORE","fixture-store",true,"fixture-"+u,-1);assertTrue(ranking.board(u,"STORE","fixture-store","COUNTRY",null,null,null,100,0).stream().anyMatch(r->r.nickname().equals("fixture-"+u)&&r.days()==1));String appeal=ranking.appeal(u,"visit:"+visit.id()+":1","fixture evidence");assertEquals("PENDING_REVIEW",ranking.appealRead(u,appeal).state());ranking.appealReview(u,appeal,"fixture-private-evidence");assertEquals("REVIEWED",ranking.appealRead(u,appeal).state());}

}
