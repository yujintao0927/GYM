package com.gym.modules.supportcore;
import com.gym.modules.supportcore.api.*;
import com.gym.modules.supportcore.api.SupportModels.*;
import com.gym.modules.supportcore.application.SupportService;
import com.gym.modules.supportcore.persistence.SupportDatabase;
import com.gym.modules.supportcore.ports.SupportPorts.*;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;
class MysqlSupportTest {
 static DriverManagerDataSource ds;
 SupportService service; String actor; volatile Decision decision=Decision.ALLOW;
 Context ctx(String key) {return new Context(actor,"fixture-request",key);}
 @BeforeAll static void migrate() throws Exception {
  try(var server=java.sql.DriverManager.getConnection("jdbc:mysql://localhost:3306/?connectTimeout=3000&socketTimeout=5000",System.getenv("GYM_DB_USERNAME"),System.getenv("GYM_DB_PASSWORD"));var statement=server.createStatement()) {
   statement.execute("CREATE DATABASE IF NOT EXISTS gym_dev_test_biz005 CHARACTER SET utf8mb4");
  }
  ds=new DriverManagerDataSource("jdbc:mysql://localhost:3306/gym_dev_test_biz005",System.getenv("GYM_DB_USERNAME"),System.getenv("GYM_DB_PASSWORD"));
  SupportDatabase.migrate(ds); SupportDatabase.migrate(ds);
 }
 @BeforeEach void setup() {
  actor="fixture-"+UUID.randomUUID();
  service=new SupportService(ds,(a,b,c,d,e)->decision,a->{if(a==null)throw new SupportFailure(401,"INACTIVE");},s->new Store(s,"merchant-fixture","ACTIVE",1),r->new RefundFact(r,actor,"store-fixture","REJECTED",1));
 }
 Ticket open(String key) {return service.open(ctx(key),new Open("store-fixture","COMPLAINT","synthetic text",null));}
 @Test void replayConflictAndReconnect() {
  var t=open("open"); assertEquals(t,open("open"));
  assertThrows(SupportFailure.class,()->service.open(ctx("open"),new Open("store-fixture","TICKET","other",null)));
  setupServiceAgain(); assertEquals(t,service.get(ctx("read"),t.id()));
  assertEquals(1,service.history(ctx("history"),t.id(),0,20).size());
 }
 void setupServiceAgain(){service=new SupportService(ds,(a,b,c,d,e)->decision,a->{},s->new Store(s,"merchant-fixture","ACTIVE",1),r->null);}
 @Test void revokedUnknownAndForeignOwnerDenied() {
  var t=open("open"); decision=Decision.DENY;
  assertThrows(SupportFailure.class,()->service.get(ctx("x"),t.id()));
  decision=Decision.UNAVAILABLE;
  assertEquals(503,assertThrows(SupportFailure.class,()->service.get(ctx("x"),t.id())).status);
  decision=Decision.ALLOW; actor="other";
  assertEquals(404,assertThrows(SupportFailure.class,()->service.get(ctx("x"),t.id())).status);
 }
 @Test void dispositionAppealAndPolicyRecommendation() {
  var t=open("open");
  t=service.change(ctx("propose"),new Change(t.id(),0,"PROPOSE","restriction pending policy",null));
  assertEquals("OPEN",t.state());
  t=service.change(ctx("dispose"),new Change(t.id(),1,"DISPOSE","reviewed evidence",null));
  t=service.change(ctx("appeal"),new Change(t.id(),2,"APPEAL","request review",null));
  assertEquals("APPEALED",t.state());assertEquals(4,service.history(ctx("h"),t.id(),0,20).size());
 }
 @Test void attachmentIsolationAndInvalidFileRollback() {
  var t=open("open");var jdbc=new JdbcTemplate(ds);
  long before=jdbc.queryForObject("select count(*) from sup_attachment",Long.class);
  assertThrows(SupportFailure.class,()->service.attach(ctx("bad"),t.id(),0,"image/png",new byte[]{1,2}));
  assertEquals(before,jdbc.queryForObject("select count(*) from sup_attachment",Long.class));
  byte[] png=png();
  var a=service.attach(ctx("img"),t.id(),0,"image/png",png);
  assertArrayEquals(png,service.download(ctx("d"),a.id()));actor="other";
  assertThrows(SupportFailure.class,()->service.download(ctx("d"),a.id()));
 }
 @Test void concurrentVersionOnlyOneWins() throws Exception {
  var t=open("open");var pool=Executors.newFixedThreadPool(2);var barrier=new CyclicBarrier(2);
  try {
   Callable<Boolean> c=()->{barrier.await();try { service.change(ctx(UUID.randomUUID().toString()),new Change(t.id(),0,"ESCALATE","review",null));return true;}catch(SupportFailure e){assertEquals(409,e.status);return false;}};
   var x=pool.submit(c);var y=pool.submit(c);assertNotEquals(x.get(),y.get());
   assertEquals(1,service.get(ctx("r"),t.id()).version());assertEquals(2,service.history(ctx("h"),t.id(),0,20).size());
  }finally {pool.shutdownNow();}
 }
 static byte[] png() {
  try {var output=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(1,1,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",output);return output.toByteArray();}catch(Exception e){throw new AssertionError(e);}
 }
 @Test void imageFormatAndDimensionsAreBoundedBeforeDecode() {
  var t=open("open");byte[] image=png();
  assertThrows(SupportFailure.class,()->service.attach(ctx("wrong-type"),t.id(),0,"image/jpeg",image));
  java.nio.ByteBuffer.wrap(image,16,8).putInt(50000).putInt(50000);
  var crc=new java.util.zip.CRC32();crc.update(image,12,17);java.nio.ByteBuffer.wrap(image,29,4).putInt((int)crc.getValue());
  assertThrows(SupportFailure.class,()->service.attach(ctx("huge"),t.id(),0,"image/png",image));
 }
 @Test void attachmentFailureRollsBackAllWritesAndRetryWorks() {
  var t=open("open");var jdbc=new JdbcTemplate(ds);
  long entries=jdbc.queryForObject("select count(*) from sup_entry where ticket_id=?",Long.class,t.id());
  long events=jdbc.queryForObject("select count(*) from sup_outbox where aggregate_id=?",Long.class,t.id());
  var failing=new SupportService(ds,(a,b,c,d,e)->Decision.ALLOW,a->{},s->null,r->null){@Override protected void beforeCommit(){throw new IllegalStateException("INJECTED");}};
  assertThrows(IllegalStateException.class,()->failing.attach(ctx("file-failure"),t.id(),0,"image/png",png()));
  assertEquals(0,jdbc.queryForObject("select count(*) from sup_attachment where ticket_id=?",Integer.class,t.id()));
  assertEquals(0,service.get(ctx("read"),t.id()).version());
  assertEquals(entries,jdbc.queryForObject("select count(*) from sup_entry where ticket_id=?",Long.class,t.id()));
  assertEquals(events,jdbc.queryForObject("select count(*) from sup_outbox where aggregate_id=?",Long.class,t.id()));
  assertEquals(0,jdbc.queryForObject("select count(*) from sup_receipt where actor_id=? and idem_key='file-failure'",Integer.class,actor));
  assertNotNull(service.attach(ctx("file-failure"),t.id(),0,"image/png",png()));
 }
 @Test void assignedAgentReadAndReassignmentRevokesAccess() {
  var t=open("open");String owner=actor;
  t=service.change(ctx("assign"),new Change(t.id(),0,"ASSIGN","fixture assignment","agent-a"));
  actor="agent-a";assertEquals(t,service.get(ctx("read"),t.id()));
  actor=owner;t=service.change(ctx("reassign"),new Change(t.id(),1,"ASSIGN","fixture reassignment","agent-b"));
  actor="agent-a";String id=t.id();assertThrows(SupportFailure.class,()->service.get(ctx("r"),id));
 }
 @Test void refundReferencesFailClosed() {
  service=new SupportService(ds,(a,b,c,d,e)->Decision.ALLOW,a->{},s->new Store(s,"merchant-fixture","ACTIVE",1),r->new RefundFact(r,"foreign","store-fixture","REJECTED",1));
  assertEquals(404,assertThrows(SupportFailure.class,()->service.open(ctx("ref"),new Open("store-fixture","REFUND_REVIEW","fixture","refund-fixture"))).status);
  service=new SupportService(ds,(a,b,c,d,e)->Decision.ALLOW,a->{},s->null,r->null);
  assertEquals(503,assertThrows(SupportFailure.class,()->open("unknown")).status);
 }
 @Test void scopedAuthorizationAndPrivateOutbox() {
  service=new SupportService(ds,(a,b,m,s,o)->s.equals("store-fixture")?Decision.ALLOW:Decision.DENY,a->{},s->new Store(s,"merchant-fixture","ACTIVE",1),r->null);
  assertEquals(403,assertThrows(SupportFailure.class,()->service.open(ctx("foreign"),new Open("other-store","TICKET","private fixture",null))).status);
  var t=open("open");service.change(ctx("reply"),new Change(t.id(),0,"REPLY","private reply",null));
  var first=service.history(ctx("h"),t.id(),0,1);assertEquals(1,first.size());
  var second=service.history(ctx("h2"),t.id(),first.getFirst().sequence(),1);assertEquals("REPLY",second.getFirst().action());
  var payloads=new JdbcTemplate(ds).queryForList("select payload from sup_outbox where aggregate_id=?",String.class,t.id());
  assertEquals(2,payloads.size());assertTrue(payloads.stream().noneMatch(x->x.contains("private")||x.contains("synthetic")));
 }
 @Test void databaseGuardRejectsBeforeMigration() throws Exception {
  var connection=org.mockito.Mockito.mock(java.sql.Connection.class);
  var source=org.mockito.Mockito.mock(javax.sql.DataSource.class);
  org.mockito.Mockito.when(source.getConnection()).thenReturn(connection);
  org.mockito.Mockito.when(connection.getCatalog()).thenReturn("unapproved-schema");
  assertThrows(IllegalStateException.class,()->SupportDatabase.migrate(source));
  org.mockito.Mockito.verify(connection,org.mockito.Mockito.never()).createStatement();
 }
 @Test void defaultDenyAndDatabaseGuard() {
  service=new SupportService(ds,com.gym.modules.supportcore.ports.SupportPorts.denyAll(),a->{},s->new Store(s,"merchant-fixture","ACTIVE",1),r->null);
  assertEquals(403,assertThrows(SupportFailure.class,()->open("denied")).status);
 }
 @Test void outboxFailureRollsBackTicketAndReceipt() {
  var jdbc=new JdbcTemplate(ds);long count=jdbc.queryForObject("select count(*) from sup_ticket",Long.class);
  var failing=new SupportService(ds,(a,b,c,d,e)->Decision.ALLOW,a->{},s->new Store(s,"merchant-fixture","ACTIVE",1),r->null) {
   @Override protected void beforeCommit(){throw new IllegalStateException("INJECTED");}
  };
  assertThrows(IllegalStateException.class,()->failing.open(ctx("rollback"),new Open("store-fixture","TICKET","fixture",null)));
  assertEquals(count,jdbc.queryForObject("select count(*) from sup_ticket",Long.class));
  assertEquals(0,jdbc.queryForObject("select count(*) from sup_receipt where actor_id=?",Integer.class,actor));
 }
}
