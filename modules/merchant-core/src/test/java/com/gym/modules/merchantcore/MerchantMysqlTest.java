package com.gym.modules.merchantcore;
import com.gym.modules.merchantcore.api.*;
import com.gym.modules.merchantcore.api.MerchantApi.*;
import com.gym.modules.merchantcore.application.MerchantService;
import com.gym.modules.merchantcore.domain.EmployeeGrant.Role;
import com.gym.modules.merchantcore.persistence.*;
import com.gym.modules.merchantcore.ports.MerchantPorts.*;
import java.util.*;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;
class MerchantMysqlTest {
 static DriverManagerDataSource ds;
 MerchantService service; String actor="fixture-actor", merchant,store; Set<String> denied=new HashSet<>();
 Context c(){return new Context(actor,UUID.randomUUID().toString(),UUID.randomUUID().toString());}
 @BeforeAll static void migrate() throws Exception {
  String url=System.getenv("GYM_BIZ001_DB_URL"); MerchantDatabase.validateUrl(url);
  ds=new DriverManagerDataSource(url+"?createDatabaseIfNotExist=true",System.getenv("GYM_DB_USERNAME"),System.getenv("GYM_DB_PASSWORD"));
  var f=MerchantDatabase.migrate(ds);
  var metadata=new org.springframework.jdbc.core.JdbcTemplate(ds); assertEquals("utf8mb4",metadata.queryForObject("SELECT DEFAULT_CHARACTER_SET_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME=DATABASE()",String.class));
  System.out.println("BIZ001_EVIDENCE "+metadata.queryForList("SELECT version,checksum,success FROM flyway_schema_history")); assertEquals(0,f.migrate().migrationsExecuted); assertEquals("3",f.info().applied()[0].getVersion().toString());
 }
 @BeforeEach void setup(){
  service=new MerchantService(ds,(a,b,m,s,o)->denied.contains(a)?Decision.DENY:Decision.ALLOW,
   a->new ActiveAccount(a,1),new GrantPolicy(){public PolicyMetadata metadata(){return new PolicyMetadata("fixture-grants",1,PolicyStatus.CONFIGURED_FIXTURE);}public Decision mayGrant(String a,Grant g){return g.accountId().equals(a)?Decision.DENY:Decision.ALLOW;}},
   (a,b)->denied.contains(a)?Decision.DENY:Decision.ALLOW,Clock.fixed(Instant.parse("2026-11-13T00:00:00Z"),ZoneOffset.UTC),true);
  merchant=service.submitMerchant(c(),"Fixture Gym","private-evidence-reference").id();
  service.reviewMerchant(c(),merchant,0,true);
  store=service.createStore(c(),merchant,"Fixture Store").id();
 }
 @Test void storeVersionIdempotencyIsolationAndReconnection(){
  var key=c(); var first=service.updateStore(key,store,0,"Changed");
  assertEquals(first,service.updateStore(key,store,0,"Changed"));
  assertEquals(409,assertThrows(MerchantFailure.class,()->service.updateStore(key,store,0,"Different")).status());
  assertEquals(409,assertThrows(MerchantFailure.class,()->service.updateStore(c(),store,0,"Old")).status());
  assertEquals("Changed",service.store(c(),store).name());
  denied.add(actor); assertEquals(403,assertThrows(MerchantFailure.class,()->service.store(c(),store)).status());
 }
 @Test void explicitGrantRevokeCurrentDecisionAndNewStore(){
  var grant=service.grant(c(),merchant,"fixture-staff",Role.COACH,Set.of(store));
  assertEquals(Decision.ALLOW,service.currentStaffDecision("fixture-staff",merchant,store,Role.COACH));
  var second=service.createStore(c(),merchant,"New");
  assertEquals(Decision.DENY,service.currentStaffDecision("fixture-staff",merchant,second.id(),Role.COACH));
  service.revoke(c(),grant.id(),0);
  assertEquals(Decision.DENY,service.currentStaffDecision("fixture-staff",merchant,store,Role.COACH));
  assertEquals(403,assertThrows(MerchantFailure.class,()->service.grant(c(),merchant,actor,Role.OWNER,Set.of(store))).status());
 }
 @Test void serviceEndpointsPersistAndInvalidWriteRollsBack(){
  var end=Instant.parse("2026-11-13T00:00:00Z");
  service.setTerm(c(),store,0,end,end.plusSeconds(15));
  assertEquals(end,service.store(c(),store).end());
  assertEquals(422,assertThrows(MerchantFailure.class,()->service.setTerm(c(),store,1,end,end.minusSeconds(1))).status());
  assertEquals(1,service.store(c(),store).version());
 }
 @Test void paginationBindsActorAndMerchantAndRechecksPermission(){
  service.createStore(c(),merchant,"Second");
  var first=service.stores(c(),merchant,1,null); assertEquals(1,first.items().size()); assertNotNull(first.nextCursor());
  var next=service.stores(c(),merchant,1,first.nextCursor());assertNotEquals(first.items().getFirst().id(),next.items().getFirst().id());
  actor="different-fixture"; assertEquals(422,assertThrows(MerchantFailure.class,()->service.stores(c(),merchant,1,first.nextCursor())).status());
 }
 @Test void deletedStoreReplaysAndCannotBeRead(){
  var context=c(); var result=service.deleteStore(context,store,0);
  assertEquals(result,service.deleteStore(context,store,0));
  assertEquals(404,assertThrows(MerchantFailure.class,()->service.store(c(),store)).status());
 }
 @Test void concurrentUpdatesHaveOneWinner() throws Exception {
  var gate=new java.util.concurrent.CyclicBarrier(2);var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
  try {
   java.util.concurrent.Callable<Integer> job=()->{gate.await();try{service.updateStore(c(),store,0,"Concurrent");return 200;}catch(MerchantFailure f){return f.status();}};
   var a=executor.submit(job);var b=executor.submit(job);var results=new ArrayList<>(List.of(a.get(15,java.util.concurrent.TimeUnit.SECONDS),b.get(15,java.util.concurrent.TimeUnit.SECONDS)));Collections.sort(results);assertEquals(List.of(200,409),results);
   assertEquals(1,service.store(c(),store).version());
  }finally{executor.shutdownNow();}
 }
 @Test void outboxFailureRollsBackSourceAuditAndReceipt(){
  var db=new org.springframework.jdbc.core.JdbcTemplate(ds);String trigger="mch_fault_"+UUID.randomUUID().toString().replace("-","");var context=c();
  long audits=db.queryForObject("SELECT COUNT(*) FROM mch_audit WHERE aggregate_id=?",Long.class,store);
  db.execute("CREATE TRIGGER "+trigger+" BEFORE INSERT ON mch_outbox FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture injected failure'");
  try {assertEquals(503,assertThrows(MerchantFailure.class,()->service.updateStore(context,store,0,"Must rollback")).status());}
  finally{db.execute("DROP TRIGGER "+trigger);}
  assertEquals(0,service.store(c(),store).version());assertEquals(audits,db.queryForObject("SELECT COUNT(*) FROM mch_audit WHERE aggregate_id=?",Long.class,store));
  assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM mch_receipt WHERE actor=? AND operation_key=?",Long.class,context.actorId(),context.key()));
  assertEquals(1,service.updateStore(context,store,0,"Must rollback").version());
 }
 @Test void unknownDependenciesAndUnapprovedGrantFailClosed(){
  var unavailable=new MerchantService(ds,(a,b,m,s,o)->Decision.UNAVAILABLE,a->new ActiveAccount(a,1),com.gym.modules.merchantcore.ports.MerchantPorts.unapprovedGrantPolicy(),(a,b)->Decision.ALLOW,Clock.systemUTC(),true);
  var failure=assertThrows(MerchantFailure.class,()->unavailable.store(c(),store));assertEquals(503,failure.status());assertEquals("AUTHORIZATION_UNAVAILABLE",failure.getMessage());assertFalse(failure.getMessage().contains(store));
  var policyDenied=new MerchantService(ds,(a,b,m,s,o)->Decision.ALLOW,a->new ActiveAccount(a,1),com.gym.modules.merchantcore.ports.MerchantPorts.unapprovedGrantPolicy(),(a,b)->Decision.ALLOW,Clock.systemUTC(),true);
  assertEquals("POLICY_UNAPPROVED",assertThrows(MerchantFailure.class,()->policyDenied.grant(c(),merchant,"staff",Role.COACH,Set.of(store))).getMessage());
 } @Test void scopeSnapshotSurvivesCallerMutationDuringPolicy(){
  var second=service.createStore(c(),merchant,"Not in original scope").id();var mutable=new HashSet<>(Set.of(store));
  var attacker=new MerchantService(ds,(a,b,m,s,o)->Decision.ALLOW,a->new ActiveAccount(a,1),new GrantPolicy(){public PolicyMetadata metadata(){return new PolicyMetadata("fixture-grants",1,PolicyStatus.CONFIGURED_FIXTURE);}public Decision mayGrant(String a,Grant g){mutable.add(second);return Decision.ALLOW;}},(a,b)->Decision.ALLOW,Clock.systemUTC(),true);
  var grant=attacker.grant(c(),merchant,"fixture-target",Role.COACH,mutable);
  var db=new org.springframework.jdbc.core.JdbcTemplate(ds);assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM mch_grant_store WHERE grant_id=? AND store_id=?",Long.class,grant.id(),second));
 }
 @Test void crossMerchantGrantDeniedWithoutWrites(){
  var other=service.submitMerchant(c(),"Other","fixture-ref");service.reviewMerchant(c(),other.id(),0,true);var otherStore=service.createStore(c(),other.id(),"Foreign");var context=c();
  assertEquals(404,assertThrows(MerchantFailure.class,()->service.grant(context,merchant,"fixture-staff",Role.COACH,Set.of(otherStore.id()))).status());
  var db=new org.springframework.jdbc.core.JdbcTemplate(ds);assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM mch_receipt WHERE actor=? AND operation_key=?",Long.class,context.actorId(),context.key()));
 }
 @Test void inactiveAccountsAndMissingPlatformSourceFailClosed(){
  var noPlatform=new MerchantService(ds,(a,b,m,s,o)->Decision.ALLOW,a->new ActiveAccount(a,1),new GrantPolicy(){public PolicyMetadata metadata(){return new PolicyMetadata("fixture-grants",1,PolicyStatus.CONFIGURED_FIXTURE);}public Decision mayGrant(String a,Grant g){return Decision.ALLOW;}},(a,b)->Decision.UNAVAILABLE,Clock.systemUTC(),true);
  assertEquals(503,assertThrows(MerchantFailure.class,()->noPlatform.grant(c(),merchant,"fixture-staff",Role.COACH,Set.of(store))).status());
  var closed=new MerchantService(ds,(a,b,m,s,o)->Decision.ALLOW,a->null,com.gym.modules.merchantcore.ports.MerchantPorts.unapprovedGrantPolicy(),(a,b)->Decision.ALLOW,Clock.systemUTC(),true);
  assertEquals(401,assertThrows(MerchantFailure.class,()->closed.store(c(),store)).status());
 } @Test void dependencySecretsAndInvalidScopeNeverEscape(){
  var secrets=new MerchantService(ds,(a,b,m,s,o)->Decision.ALLOW,a->{throw new MerchantFailure(503,"fixture-secret-account");},com.gym.modules.merchantcore.ports.MerchantPorts.unapprovedGrantPolicy(),(a,b)->Decision.ALLOW,Clock.systemUTC(),true);
  var failure=assertThrows(MerchantFailure.class,()->secrets.store(c(),store));assertEquals("ACCOUNT_UNAVAILABLE",failure.getMessage());assertNull(failure.getCause());
  var badPolicy=new MerchantService(ds,(a,b,m,s,o)->Decision.ALLOW,a->new ActiveAccount(a,1),new GrantPolicy(){public PolicyMetadata metadata(){throw new IllegalStateException("fixture-secret-policy");}public Decision mayGrant(String a,Grant g){return Decision.ALLOW;}},(a,b)->Decision.ALLOW,Clock.systemUTC(),true);
  assertEquals("POLICY_UNAVAILABLE",assertThrows(MerchantFailure.class,()->badPolicy.grant(c(),merchant,"fixture-staff",Role.COACH,Set.of(store))).getMessage());
  assertEquals(422,assertThrows(MerchantFailure.class,()->service.grant(c(),merchant,"fixture-staff",Role.COACH,Set.of())).status());
 }
 @Test void oldReceiptDoesNotBypassCurrentRevocation(){
  var context=c();service.updateStore(context,store,0,"Receipt");denied.add(actor);
  assertEquals(403,assertThrows(MerchantFailure.class,()->service.updateStore(context,store,0,"Receipt")).status());
 }
 @Test void scopedProviderUsesPersistedOwnersAndBlocksForeignReadWriteDelete(){
  var scoped=new MerchantService(ds,(a,b,m,s,o)->merchant.equals(m)&&actor.equals(o)?Decision.ALLOW:Decision.DENY,a->new ActiveAccount(a,1),com.gym.modules.merchantcore.ports.MerchantPorts.unapprovedGrantPolicy(),(a,b)->Decision.DENY,Clock.systemUTC(),true);
  assertEquals(store,scoped.store(c(),store).id());
  var foreign=service.submitMerchant(c(),"Foreign","fixture-ref");service.reviewMerchant(c(),foreign.id(),0,true);var fs=service.createStore(c(),foreign.id(),"Foreign");
  assertEquals(403,assertThrows(MerchantFailure.class,()->scoped.store(c(),fs.id())).status());assertEquals(403,assertThrows(MerchantFailure.class,()->scoped.updateStore(c(),fs.id(),0,"Denied")).status());assertEquals(403,assertThrows(MerchantFailure.class,()->scoped.deleteStore(c(),fs.id(),0)).status());assertEquals(0,service.store(c(),fs.id()).version());
 }
 @Test void persistedGrantDoesNotSurviveClosedOrUnavailableAccount(){
  service.grant(c(),merchant,"fixture-staff",Role.COACH,Set.of(store));
  var closed=new MerchantService(ds,(a,b,m,s,o)->Decision.ALLOW,a->null,com.gym.modules.merchantcore.ports.MerchantPorts.unapprovedGrantPolicy(),(a,b)->Decision.DENY,Clock.systemUTC(),true);
  assertEquals(Decision.DENY,closed.currentStaffDecision("fixture-staff",merchant,store,Role.COACH));
  var missing=new MerchantService(ds,(a,b,m,s,o)->Decision.ALLOW,a->{throw new IllegalStateException("fixture-secret");},com.gym.modules.merchantcore.ports.MerchantPorts.unapprovedGrantPolicy(),(a,b)->Decision.DENY,Clock.systemUTC(),true);
  assertEquals(Decision.UNAVAILABLE,missing.currentStaffDecision("fixture-staff",merchant,store,Role.COACH));
 } @Test void fixturePolicyCannotEnterDefaultRuntimeBoundary(){
  var fixture=new GrantPolicy(){public PolicyMetadata metadata(){return new PolicyMetadata("fixture",1,PolicyStatus.CONFIGURED_FIXTURE);}public Decision mayGrant(String a,Grant g){return Decision.ALLOW;}};
  var runtime=new MerchantService(ds,(a,b,m,s,o)->Decision.ALLOW,a->new ActiveAccount(a,1),fixture,(a,b)->Decision.ALLOW,Clock.systemUTC());
  assertEquals("POLICY_UNAPPROVED",assertThrows(MerchantFailure.class,()->runtime.grant(c(),merchant,"fixture-staff",Role.COACH,Set.of(store))).getMessage());
 } @Test void directoryAndGrantQueryReturnPersistedScopedSnapshots(){
  assertEquals("PENDING_POLICY",service.requireStore(store).status());assertEquals(merchant,service.requireStore(store).merchantId());
  var end=Instant.parse("2026-11-13T00:00:00Z");service.setTerm(c(),store,0,end,end.plusSeconds(15));assertEquals("GRACE",service.requireStore(store).status());
  var granted=service.grant(c(),merchant,"fixture-staff",Role.COACH,Set.of(store));assertEquals(Set.of(store),service.employeeGrant(c(),granted.id()).stores());
  service.revoke(c(),granted.id(),0);assertTrue(service.employeeGrant(c(),granted.id()).revoked());denied.add(actor);assertEquals(403,assertThrows(MerchantFailure.class,()->service.employeeGrant(c(),granted.id())).status());
 }
 @Test void policyProviderExceptionHasSafeBoundary(){
  var broken=new MerchantService(ds,(a,b,m,s,o)->Decision.ALLOW,a->new ActiveAccount(a,1),new GrantPolicy(){public PolicyMetadata metadata(){throw new IllegalStateException("fixture-secret-policy");}public Decision mayGrant(String a,Grant g){return Decision.ALLOW;}},(a,b)->Decision.ALLOW,Clock.systemUTC(),true);
  assertEquals("POLICY_UNAVAILABLE",assertThrows(MerchantFailure.class,()->broken.grant(c(),merchant,"fixture-staff",Role.COACH,Set.of(store))).getMessage());
 } @Test void fixtureSubsetPolicyAndIndependentReconnectHaveRealAssertions(){
  var second=service.createStore(c(),merchant,"Outside delegated set").id();
  var subset=new MerchantService(ds,(a,b,m,s,o)->Decision.ALLOW,a->new ActiveAccount(a,1),new GrantPolicy(){public PolicyMetadata metadata(){return new PolicyMetadata("fixture-subset",1,PolicyStatus.CONFIGURED_FIXTURE);}public Decision mayGrant(String a,Grant g){return !g.accountId().equals(a)&&g.role()==Role.COACH&&Set.of(store).containsAll(g.stores())?Decision.ALLOW:Decision.DENY;}},(a,b)->Decision.ALLOW,Clock.systemUTC(),true);
  var granted=subset.grant(c(),merchant,"fixture-staff",Role.COACH,Set.of(store));
  assertEquals(403,assertThrows(MerchantFailure.class,()->subset.grant(c(),merchant,"fixture-staff",Role.COACH,Set.of(second))).status());
  assertEquals(403,assertThrows(MerchantFailure.class,()->subset.grant(c(),merchant,"fixture-staff",Role.OWNER,Set.of(store))).status());
  var separate=new DriverManagerDataSource(System.getenv("GYM_BIZ001_DB_URL"),System.getenv("GYM_DB_USERNAME"),System.getenv("GYM_DB_PASSWORD"));
  var db=new org.springframework.jdbc.core.JdbcTemplate(separate);assertEquals(merchant,db.queryForObject("SELECT merchant_id FROM mch_store WHERE id=?",String.class,store));
  var payload=db.queryForObject("SELECT payload FROM mch_outbox WHERE aggregate_id=? AND event_type='staff.grant'",String.class,granted.id());assertTrue(payload.contains("CONFIGURED_FIXTURE"));assertTrue(payload.contains("fixture-subset"));assertFalse(payload.contains("private-evidence"));
 } @Test void rejectedMerchantCannotCreateStoreAndReviewIsFinal(){
  var pending=service.submitMerchant(c(),"Rejected Fixture","fixture-private-ref");assertEquals("PENDING",service.merchant(c(),pending.id()).state());
  service.reviewMerchant(c(),pending.id(),0,false);assertEquals("REJECTED",service.merchant(c(),pending.id()).state());
  assertEquals(422,assertThrows(MerchantFailure.class,()->service.createStore(c(),pending.id(),"Blocked")).status());
  assertEquals(409,assertThrows(MerchantFailure.class,()->service.reviewMerchant(c(),pending.id(),1,true)).status());
 }
 @Test void ownUnknownFixtureTableBlocksMigrationAndIsRemoved() throws Exception {
  var db=new org.springframework.jdbc.core.JdbcTemplate(ds);String table="mch_fixture_unknown_"+UUID.randomUUID().toString().replace("-","");db.execute("CREATE TABLE "+table+" (id INT PRIMARY KEY)");
  try{assertEquals("UNKNOWN_DATABASE_OBJECT_REVIEW_REQUIRED",assertThrows(IllegalStateException.class,()->MerchantDatabase.migrate(ds)).getMessage());}
  finally{db.execute("DROP TABLE "+table);}
  assertEquals(0,MerchantDatabase.migrate(ds).migrate().migrationsExecuted);
 }}
