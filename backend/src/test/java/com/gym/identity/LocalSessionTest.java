package com.gym.identity;

import java.net.URI;
import java.net.http.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
 "gym.db.url=${GYM_TEST_DB_URL}","gym.db.schema=gym_dev_test_night001"})
@ActiveProfiles("local-dev")
class LocalSessionTest {
 @Value("${local.server.port}") int port;
 static final HttpClient HTTP=HttpClient.newHttpClient();
 static final JsonMapper JSON=JsonMapper.builder().build();
 static final String PASSWORD="local-fixture-password-003b";
 static volatile Instant now=Instant.parse("2026-11-13T00:00:00Z");
 @TestConfiguration static class TimeConfig {
  @Bean @Primary Clock testClock(){return new Clock(){public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return now;}};}
 }
 @BeforeEach void resetClock() throws Exception {now=Instant.parse("2026-11-13T00:00:00Z"); var crypto=context.getBean(com.gym.identity.local.LocalCredentialAuthenticator.class);sql("delete from iam_auth_failure where bucket_digest=?",crypto.fingerprint("source","127.0.0.1"));}
 Connection db() throws Exception{return DriverManager.getConnection(MysqlMigrationTest.url(),System.getenv("GYM_DB_USERNAME"),System.getenv("GYM_DB_PASSWORD"));}
 void sql(String sql,Object... params) throws Exception {try(var c=db();var s=c.prepareStatement(sql)){for(int i=0;i<params.length;i++)s.setObject(i+1,params[i]);s.executeUpdate();}}
 String account() throws Exception {
  String login="t_"+UUID.randomUUID().toString().replace("-","");
  Class<?> type=Class.forName("com.gym.identity.local.LocalAccountProvisioner");
  context.getBean(type).getClass().getMethod("create",String.class,char[].class).invoke(context.getBean(type),login,PASSWORD.toCharArray());
  return login;
 }
 @org.springframework.beans.factory.annotation.Autowired org.springframework.context.ApplicationContext context;
 HttpResponse<String> call(String method,String path,Object body,String bearer,String key) throws Exception {
  var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));
  if(bearer!=null)b.header("Authorization","Bearer "+bearer);
  if(key!=null)b.header("Idempotency-Key",key);
  b.header("Content-Type","application/json");
  return HTTP.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
 }
 Map<String,Object> loginBody(String login,String proof){return Map.of("credential",Map.of("kind","LOCAL_PASSWORD","login",login,"proof",proof),"entry","user-web","clientKind","WEB");}
 JsonNode data(HttpResponse<String> r)throws Exception{assertThat(r.statusCode()).as(r.body()).isIn(200,201);assertThat(r.headers().firstValue("Cache-Control").orElse("")).contains("no-store");return JSON.readTree(r.body()).get("data");}
 JsonNode login(String name)throws Exception{var t=data(call("POST","/v1/sessions",loginBody(name,PASSWORD),null,UUID.randomUUID().toString()));assertThat(t.get("generation").asInt()).isZero();return t;}
 HttpResponse<String> refresh(JsonNode t,String key)throws Exception{return call("POST","/v1/sessions/refresh",Map.of("refreshToken",t.get("refreshToken").asText(),"generation",t.get("generation").asInt()),null,key);}
 @Test void closedLoopPersistsOnlyHashesAndRejectsRevokedTokens()throws Exception{
  String name=account();var t=login(name);String access=t.get("sessionToken").asText();String id=t.get("sessionId").asText();
  var me=data(call("GET","/v1/me/contexts",null,access,null));
  assertThat(me.get("staffScopes").size()).isZero();assertThat(me.get("entitlementSources").size()).isZero();
  try(var c=db();var s=c.prepareStatement("select password_hash from iam_local_credential where login_normalized=?")){s.setString(1,name);try(var r=s.executeQuery()){r.next();assertThat(r.getString(1)).startsWith("$2a$12$").doesNotContain(PASSWORD);}}
  try(var c=db();var s=c.prepareStatement("select access_digest from iam_session where id=?")){s.setString(1,id);try(var r=s.executeQuery()){r.next();assertThat(r.getString(1)).hasSize(64).isNotEqualTo(access);}}
  var next=data(refresh(t,"refresh-1"));assertThat(next.get("sessionId").asText()).isEqualTo(id);assertThat(next.get("generation").asInt()).isEqualTo(1);
  assertThat(call("GET","/v1/me/contexts",null,access,null).statusCode()).isEqualTo(401);
  String current=next.get("sessionToken").asText();data(call("DELETE","/v1/sessions/"+id,null,current,null));
  assertThat(refresh(next,"refresh-2").statusCode()).isEqualTo(401);
  assertThat(call("GET","/v1/me/contexts",null,current,null).statusCode()).isEqualTo(401);
 }
 @Test void loginReceiptsPreventDuplicateSessionsAndDetectChangedBody()throws Exception{
  String name=account(),key=UUID.randomUUID().toString();var body=loginBody(name,PASSWORD);
  data(call("POST","/v1/sessions",body,null,key));
  var retry=call("POST","/v1/sessions",body,null,key);assertThat(retry.statusCode()).isEqualTo(409);assertThat(retry.body()).contains("REAUTH_REQUIRED");
  var changed=new HashMap<>(body);changed.put("entry","merchant");
  assertThat(call("POST","/v1/sessions",changed,null,key).body()).contains("IDEMPOTENCY_MISMATCH");
  try(var c=db();var s=c.prepareStatement("select count(*) from iam_session s join iam_local_credential l on l.account_id=s.account_id where l.login_normalized=?")){s.setString(1,name);try(var r=s.executeQuery()){r.next();assertThat(r.getInt(1)).isEqualTo(1);}}
 }
 @Test void ownerBoundaryAndClosedEpochAndTimeAreCheckedAgainstDatabase()throws Exception{
  var a=login(account());var b=login(account());String access=a.get("sessionToken").asText();
  assertThat(call("DELETE","/v1/sessions/"+b.get("sessionId").asText(),null,access,null).statusCode()).isEqualTo(404);
  sql("update iam_account set session_epoch=session_epoch+1 where id=(select account_id from iam_session where id=?)",a.get("sessionId").asText());
  assertThat(call("GET","/v1/me/contexts",null,access,null).statusCode()).isEqualTo(401);assertThat(refresh(a,"epoch").statusCode()).isEqualTo(401);
  String name=account();var closed=login(name);sql("update iam_account set status='CLOSED' where id=(select account_id from iam_session where id=?)",closed.get("sessionId").asText());
  assertThat(call("POST","/v1/sessions",loginBody(name,PASSWORD),null,"closed").statusCode()).isEqualTo(403);assertThat(refresh(closed,"closed").statusCode()).isEqualTo(401);
  var t=login(account());now=now.plusSeconds(1800);assertThat(call("GET","/v1/me/contexts",null,t.get("sessionToken").asText(),null).statusCode()).isEqualTo(401);
  var r=data(refresh(t,"at-30min"));assertThat(r.get("refreshExpiresAt").asText()).isEqualTo(t.get("refreshExpiresAt").asText());now=Instant.parse(t.get("refreshExpiresAt").asText());assertThat(refresh(r,"at-7days").statusCode()).isEqualTo(401);
 }
 @Test void refreshRetriesAndReuseAreSerializedWithoutTwoActiveLeaves()throws Exception{
  var t=login(account());try(var pool=Executors.newVirtualThreadPerTaskExecutor()){
   var latch=new CountDownLatch(1);Callable<HttpResponse<String>> job=()->{latch.await();return refresh(t,"same-key");};var x=pool.submit(job);var y=pool.submit(job);latch.countDown();
   var rx=x.get();var ry=y.get();assertThat(List.of(rx.statusCode(),ry.statusCode())).containsExactlyInAnyOrder(201,409);
   var winner=data(rx.statusCode()==201?rx:ry);
   assertThat(refresh(t,"different-key").statusCode()).isEqualTo(401);
   assertThat(call("GET","/v1/me/contexts",null,winner.get("sessionToken").asText(),null).statusCode()).isEqualTo(401);
  }
 }
 @RepeatedTest(6) void concurrentLoginAndLogoutRefreshMaintainSingleCommitBoundary()throws Exception{
  String name=account();String key=UUID.randomUUID().toString();try(var pool=Executors.newVirtualThreadPerTaskExecutor()){
   var latch=new CountDownLatch(1);Callable<HttpResponse<String>> job=()->{latch.await();return call("POST","/v1/sessions",loginBody(name,PASSWORD),null,key);};var x=pool.submit(job);var y=pool.submit(job);latch.countDown();var rx=x.get();var ry=y.get();assertThat(List.of(rx.statusCode(),ry.statusCode())).containsExactlyInAnyOrder(201,409);
   var target=data(rx.statusCode()==201?rx:ry);var actor=login(name);var go=new CountDownLatch(1);
   var logout=pool.submit(()->{go.await();return call("DELETE","/v1/sessions/"+target.get("sessionId").asText(),null,actor.get("sessionToken").asText(),null);});
   var rotating=pool.submit(()->{go.await();return refresh(target,"race");});go.countDown();assertThat(logout.get().statusCode()).isEqualTo(200);var result=rotating.get();assertThat(result.statusCode()).isIn(201,401);
   if(result.statusCode()==201) assertThat(call("GET","/v1/me/contexts",null,data(result).get("sessionToken").asText(),null).statusCode()).isEqualTo(401);
  }
 }
 @Test void authenticationErrorsDoNotEnumerateAndRateLimitHasRetryAfter()throws Exception{
  String name=account();var wrong=call("POST","/v1/sessions",loginBody(name,"incorrect-proof-123"),null,"bad");var absent=call("POST","/v1/sessions",loginBody("missing_"+UUID.randomUUID(),"incorrect-proof-123"),null,"absent");
  assertThat(wrong.statusCode()).isEqualTo(401);assertThat(absent.statusCode()).isEqualTo(401);assertThat(JSON.readTree(wrong.body()).get("code").asText()).isEqualTo("AUTHENTICATION_FAILED");assertThat(JSON.readTree(absent.body()).get("code").asText()).isEqualTo("AUTHENTICATION_FAILED");
  for(int i=1;i<5;i++)assertThat(call("POST","/v1/sessions",loginBody(name,"incorrect-proof-123"),null,"bad"+i).statusCode()).isEqualTo(401);
  var limited=call("POST","/v1/sessions",loginBody(name,PASSWORD),null,"limited");assertThat(limited.statusCode()).isEqualTo(429);assertThat(limited.headers().firstValue("Retry-After")).isPresent();
  now=now.plusSeconds(900);login(name);
 }
 @Test void malformedAndForgedRequestsCannotGrantAccess()throws Exception{
  assertThat(call("GET","/v1/me/contexts",null,"forged",null).statusCode()).isEqualTo(401);
  var body=new HashMap<>(loginBody(account(),PASSWORD));body.put("role","PLATFORM_ADMIN");assertThat(call("POST","/v1/sessions",body,null,"forged-role").statusCode()).isEqualTo(422);
  assertThat(call("POST","/v1/sessions",loginBody("valid-name",PASSWORD),null,null).statusCode()).isEqualTo(422);
  var t=login(account());assertThat(call("GET","/v1/orders?role=admin",null,t.get("sessionToken").asText(),null).statusCode()).isIn(401,403);
 }
 @Test void refreshFailuresShareSourceLimitAndIgnoreForgedForwarding()throws Exception{
  for(int i=0;i<30;i++){
   var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/v1/sessions/refresh")).header("X-Forwarded-For","192.0.2."+i).header("Content-Type","application/json").header("Idempotency-Key","rate-"+i).POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(Map.of("refreshToken","a".repeat(43),"generation",0))));
   assertThat(HTTP.send(b.build(),HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
  }
  var limit=refresh(JSON.readTree("{\"refreshToken\":\""+"a".repeat(43)+"\",\"generation\":0}"),"rate-31");assertThat(limit.statusCode()).isEqualTo(429);assertThat(limit.headers().firstValue("Retry-After")).isPresent();
 }
 @Test void disabledCredentialDoesNotRevealClosedAccount()throws Exception{
  String name=account();sql("update iam_local_credential set enabled=false where login_normalized=?",name);
  assertThat(call("POST","/v1/sessions",loginBody(name,PASSWORD),null,"disabled").body()).contains("AUTHENTICATION_FAILED");
 }
 @Test void differentKeyConcurrentRefreshRevokesWinner()throws Exception{
  var t=login(account());try(var pool=Executors.newVirtualThreadPerTaskExecutor()){
   var latch=new CountDownLatch(1);var x=pool.submit(()->{latch.await();return refresh(t,"different-1");});var y=pool.submit(()->{latch.await();return refresh(t,"different-2");});latch.countDown();var a=x.get();var b=y.get();assertThat(List.of(a.statusCode(),b.statusCode())).containsExactlyInAnyOrder(201,401);
   var win=data(a.statusCode()==201?a:b);assertThat(call("GET","/v1/me/contexts",null,win.get("sessionToken").asText(),null).statusCode()).isEqualTo(401);
   try(var c=db();var stmt=c.prepareStatement("select count(*) from iam_refresh_node n join iam_refresh_family f on f.id=n.family_id where f.session_id=? and n.consumed_at is null")){stmt.setString(1,t.get("sessionId").asText());try(var rs=stmt.executeQuery()){rs.next();assertThat(rs.getInt(1)).isEqualTo(1);}}
  }
 }
 @Test void cliCreatesIndividualAccountWithoutEchoAndRejectsDuplicates()throws Exception{
  String name="cli_"+UUID.randomUUID().toString().replace("-","");
  for(int attempt=0;attempt<2;attempt++){
   var builder=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-cp",System.getProperty("java.class.path"),"com.gym.identity.local.LocalAccountCli",name,"--password-stdin");
   builder.environment().put("GYM_DB_URL",MysqlMigrationTest.url());builder.environment().put("GYM_DB_SCHEMA","gym_dev_test_night001");builder.redirectErrorStream(true);
   var process=builder.start();try(var input=process.outputWriter()){input.write(PASSWORD+"\n");}
   String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);assertThat(process.waitFor()).isEqualTo(attempt==0?0:1);assertThat(output).doesNotContain(PASSWORD,"$2a$","jdbc:mysql","Exception");
  }
  login(name);
 } @Test void refreshIdempotencyKeyCannotRotateADifferentGeneration()throws Exception{
  var initial=login(account());var next=data(refresh(initial,"one-operation"));
  var result=refresh(next,"one-operation");assertThat(result.statusCode()).isEqualTo(409);assertThat(result.body()).contains("IDEMPOTENCY_MISMATCH");
 }
 @Test void loginNamespaceRejectsNonAsciiBeforeNormalization()throws Exception{
  assertThat(call("POST","/v1/sessions",loginBody("\u212Aelvin",PASSWORD),null,"ascii-only").statusCode()).isEqualTo(422);
 }
 @Test void reciprocalForeignLogoutAlwaysReturns404()throws Exception{
  var a=login(account());var b=login(account());try(var pool=Executors.newVirtualThreadPerTaskExecutor()){
   for(int i=0;i<10;i++){
    var latch=new CountDownLatch(1);
    var x=pool.submit(()->{latch.await();return call("DELETE","/v1/sessions/"+b.get("sessionId").asText(),null,a.get("sessionToken").asText(),null);});
    var y=pool.submit(()->{latch.await();return call("DELETE","/v1/sessions/"+a.get("sessionId").asText(),null,b.get("sessionToken").asText(),null);});latch.countDown();
    assertThat(List.of(x.get().statusCode(),y.get().statusCode())).containsExactly(404,404);
   }
  }
 } @Test void consumedLeafReplayRevokesEvenWhenKeyWasUsedByLaterGeneration()throws Exception{
  var first=login(account());var next=data(refresh(first,"chain-a"));var latest=data(refresh(next,"chain-b"));
  assertThat(refresh(first,"chain-b").statusCode()).isEqualTo(401);
  assertThat(call("GET","/v1/me/contexts",null,latest.get("sessionToken").asText(),null).statusCode()).isEqualTo(401);
 }
 @Test void refreshKeysAreScopedToAccountAcrossSessions()throws Exception{
  String name=account();var a=login(name);var b=login(name);data(refresh(a,"account-refresh-key"));
  var collision=refresh(b,"account-refresh-key");assertThat(collision.statusCode()).isEqualTo(409);assertThat(collision.body()).contains("IDEMPOTENCY_MISMATCH");
 }}
