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
class HealthFoundationTest {
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
 @Test void recordsAndMeasurementAndGoalArePersistentOwnerOnly() throws Exception {
  String token=login(account()).get("sessionToken").asText();
  String other=login(account()).get("sessionToken").asText();
  for(String kind:List.of("WEIGHT","DIET","TRAINING","MEASUREMENT")){
   String path=kind.equals("MEASUREMENT")?"/v1/me/body-measurements":"/v1/me/health-records";
   Map<String,Object> payload=switch(kind){case "WEIGHT"->Map.of("weightKg","70.125");case "DIET"->Map.of("foodName","rice","portionText","one bowl");case "TRAINING"->Map.of("name","walk","durationMinutes",30);default->Map.of("heightCm","175.5","bodyFatPercent","20");};
   var body=new HashMap<String,Object>();body.put("occurredAt","2026-11-13T08:00:00.000+08:00");body.put("payload",payload);if(!kind.equals("MEASUREMENT"))body.put("kind",kind);
   String key=UUID.randomUUID().toString();var created=data(call("POST",path,body,token,key));String id=created.get("id").asText();
   assertThat(call("PATCH",path+"/"+id,Map.of("occurredAt",body.get("occurredAt"),"payload",payload,"expectedVersion",1),other,null).statusCode()).isEqualTo(404);
   assertThat(call("DELETE",path+"/"+id+"?expectedVersion=1",null,other,null).statusCode()).isEqualTo(404);
   assertThat(data(call("POST",path,body,token,key)).get("id").asText()).isEqualTo(id);
   assertThat(call("GET",path+"/"+id,null,other,null).statusCode()).isEqualTo(404);
   assertThat(call("GET",path+"/"+id,null,null,null).statusCode()).isEqualTo(401);
   assertThat(data(call("GET",path+"?limit=1",null,token,null)).get("items").size()).isEqualTo(1);
   var patch=Map.of("occurredAt","2026-11-13T01:00:00.000Z","payload",payload,"expectedVersion",1);
   assertThat(data(call("PATCH",path+"/"+id,patch,token,null)).get("version").asInt()).isEqualTo(2);
   assertThat(call("PATCH",path+"/"+id,patch,token,null).statusCode()).isEqualTo(409);
   data(call("DELETE",path+"/"+id+"?expectedVersion=2",null,token,null));
   assertThat(call("POST",path,body,token,key).statusCode()).isEqualTo(404);
  }
  String goal="/v1/me/weight-goal";
  assertThat(call("GET",goal,null,token,null).statusCode()).isEqualTo(404);
  data(call("PUT",goal,Map.of("targetWeightKg","65","expectedVersion",0),token,null));
  assertThat(call("PUT",goal,Map.of("targetWeightKg","64","expectedVersion",0),token,null).statusCode()).isEqualTo(409);
  assertThat(data(call("GET",goal,null,token,null)).get("version").asInt()).isEqualTo(1);
  data(call("DELETE",goal+"?expectedVersion=1",null,token,null));
 }

 @Test void validationAndCursorBindingAndCategories() throws Exception {
  String a=login(account()).get("sessionToken").asText(),b=login(account()).get("sessionToken").asText();String path="/v1/me/health-records";
  for(Object weight:List.of("0","-1","1.1234","10000000","1e2",70,""))assertThat(call("POST",path,Map.of("kind","WEIGHT","occurredAt","2026-11-13T00:00:00.000Z","payload",Map.of("weightKg",weight)),a,UUID.randomUUID().toString()).statusCode()).isEqualTo(422);
  for(int i=0;i<3;i++)data(call("POST",path,Map.of("kind","WEIGHT","occurredAt","2026-11-13T00:00:00.000Z","payload",Map.of("weightKg","70")),a,UUID.randomUUID().toString()));
  var page=data(call("GET",path+"?limit=1",null,a,null));String cursor=page.get("nextCursor").asText();
  var next=data(call("GET",path+"?limit=1&cursor="+cursor,null,a,null));assertThat(next.get("items").get(0).get("id").asText()).isNotEqualTo(page.get("items").get(0).get("id").asText());
  for(String uri:List.of(path+"?limit=1&cursor="+cursor,path+"?limit=1&kind=DIET&cursor="+cursor))assertThat(call("GET",uri,null,b,null).statusCode()).isEqualTo(422);
  assertThat(call("GET",path+"?limit=1&cursor="+cursor+"x",null,a,null).statusCode()).isEqualTo(422);
  assertThat(data(call("GET","/v1/me/body-measurements",null,a,null)).get("items").size()).isZero();
  assertThat(data(call("GET",path+"?kind=DIET",null,a,null)).get("items").size()).isZero();
 }
 @Test void concurrentGoalCreationAndCasHaveOneWinner()throws Exception {
  String token=login(account()).get("sessionToken").asText();
  try(var pool=Executors.newVirtualThreadPerTaskExecutor()){
   for(int version=0;version<2;version++){
    final int v=version;var gate=new CountDownLatch(1);Callable<Integer> job=()->{gate.await();return call("PUT","/v1/me/weight-goal",Map.of("targetWeightKg","65","expectedVersion",v),token,null).statusCode();};
    var a=pool.submit(job);var b=pool.submit(job);gate.countDown();assertThat(List.of(a.get(),b.get())).containsExactlyInAnyOrder(200,409);
   }
  }
 }
 @Test void revokedExpiredEpochAndClosedCannotReadOrWriteHealth()throws Exception {
  for(String state:List.of("revoked","expired","epoch","closed")){
   var t=login(account());String id=t.get("sessionId").asText(),token=t.get("sessionToken").asText();
   switch(state){case "revoked"->sql("update iam_session set revoked_at=1 where id=?",id);case "expired"->sql("update iam_session set access_expires_at=1 where id=?",id);case "epoch"->sql("update iam_account set session_epoch=session_epoch+1 where id=(select account_id from iam_session where id=?)",id);default->sql("update iam_account set status='CLOSED' where id=(select account_id from iam_session where id=?)",id);}
   assertThat(call("GET","/v1/me/health-records",null,token,null).statusCode()).isEqualTo(401);
   assertThat(call("PUT","/v1/me/weight-goal",Map.of("targetWeightKg","65","expectedVersion",0),token,null).statusCode()).isEqualTo(401);
  }
 }
 @Test void healthFailureRollsBackReceiptAndRecordTogether()throws Exception {
  String token=login(account()).get("sessionToken").asText();var health=context.getBean(com.gym.health.HealthService.class);
  var body=Map.<String,Object>of("kind","WEIGHT","occurredAt","2026-11-13T00:00:00.000Z","payload",Map.of("weightKg","70"));String key=UUID.randomUUID().toString();
  org.assertj.core.api.Assertions.assertThatThrownBy(()->health.owned("Bearer "+token,owner->{health.create(owner,"RECORD",body,key);throw new com.gym.identity.api.SessionFailure(409,"INJECTED");})).isInstanceOf(com.gym.identity.api.SessionFailure.class);
  assertThat(data(call("GET","/v1/me/health-records",null,token,null)).get("items").size()).isZero();data(call("POST","/v1/me/health-records",body,token,key));
 }

 @Test void concurrentCreateAndUpdateAndLogoutHaveSingleBoundary()throws Exception {
  String name=account();var t=login(name);String token=t.get("sessionToken").asText();String path="/v1/me/health-records";
  var body=Map.<String,Object>of("kind","WEIGHT","occurredAt","2026-11-13T00:00:00.000Z","payload",Map.of("weightKg","70"));String key=UUID.randomUUID().toString();
  try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
   var gate=new CountDownLatch(1);Callable<HttpResponse<String>> create=()->{gate.await();return call("POST",path,body,token,key);};var x=pool.submit(create);var y=pool.submit(create);gate.countDown();var a=data(x.get());var b=data(y.get());assertThat(a.get("id").asText()).isEqualTo(b.get("id").asText());
   assertThat(data(call("GET",path,null,token,null)).get("items").size()).isEqualTo(1);
   var patch=Map.of("occurredAt",body.get("occurredAt"),"payload",body.get("payload"),"expectedVersion",1);String id=a.get("id").asText();var cas=new CountDownLatch(1);Callable<Integer> update=()->{cas.await();return call("PATCH",path+"/"+id,patch,token,null).statusCode();};var u=pool.submit(update);var v=pool.submit(update);cas.countDown();assertThat(List.of(u.get(),v.get())).containsExactlyInAnyOrder(200,409);
   var changed=new HashMap<>(body);changed.put("payload",Map.of("weightKg","71"));assertThat(call("POST",path,changed,token,key).statusCode()).isEqualTo(409);
   var stop=new CountDownLatch(1);var write=pool.submit(()->{stop.await();return call("POST",path,body,token,UUID.randomUUID().toString()).statusCode();});var logout=pool.submit(()->{stop.await();return call("DELETE","/v1/sessions/"+t.get("sessionId").asText(),null,token,null).statusCode();});stop.countDown();assertThat(logout.get()).isEqualTo(200);assertThat(write.get()).isIn(201,401);assertThat(call("GET",path,null,token,null).statusCode()).isEqualTo(401);
  }
 }
 @Test void malformedFieldsDatesAndPayloadsAreRejected()throws Exception {
  String token=login(account()).get("sessionToken").asText();String path="/v1/me/body-measurements";
  for(Object payload:List.of(Map.of(),Map.of("heightCm","0"),Map.of("bodyFatPercent","100.001"),Map.of("heightCm","170","accountId","forged"),Map.of("waistCm",80)))assertThat(call("POST",path,Map.of("occurredAt","2026-11-13T00:00:00.000Z","payload",payload),token,UUID.randomUUID().toString()).statusCode()).isEqualTo(422);
  for(String at:List.of("2026-11-13T00:00Z","2026-11-13","2026-11-13T01:00:00","2026-11-13T01:00:00.0001Z","invalid"))assertThat(call("POST",path,Map.of("occurredAt",at,"payload",Map.of("heightCm","170")),token,UUID.randomUUID().toString()).statusCode()).isEqualTo(422);
  assertThat(call("PUT","/v1/me/weight-goal",Map.of("targetWeightKg","65","targetDate","2026-02-30","expectedVersion",0),token,null).statusCode()).isEqualTo(422);
  assertThat(call("POST",path,Map.of("occurredAt","2026-11-13T00:00:00Z","payload",Map.of("heightCm","170")),token,null).statusCode()).isEqualTo(422);
 }

 @Test void accountFenceForcesBothLogoutOrderings()throws Exception {
  var health=context.getBean(com.gym.health.HealthService.class);var sessions=context.getBean(com.gym.identity.session.SessionService.class);var tx=context.getBean(org.springframework.transaction.support.TransactionTemplate.class);
  var body=Map.<String,Object>of("kind","WEIGHT","occurredAt","2026-11-13T00:00:00.000Z","payload",Map.of("weightKg","70"));
  try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
   for(boolean writeFirst:List.of(true,false)) {
    String name=account();var t=login(name);String bearer="Bearer "+t.get("sessionToken").asText(),id=t.get("sessionId").asText();var locked=new CountDownLatch(1);var release=new CountDownLatch(1);var contenderStarted=new CountDownLatch(1);
    var first=pool.submit(()->tx.execute(st->{if(writeFirst)health.owned(bearer,o->health.create(o,"RECORD",body,UUID.randomUUID().toString()));else sessions.logout(bearer,id);locked.countDown();try{if(!release.await(10,TimeUnit.SECONDS))throw new IllegalStateException();}catch(InterruptedException e){throw new RuntimeException(e);}return true;}));
    assertThat(locked.await(10,TimeUnit.SECONDS)).isTrue();
    var second=pool.submit(()->{contenderStarted.countDown();try {if(writeFirst)sessions.logout(bearer,id);else health.owned(bearer,o->health.create(o,"RECORD",body,UUID.randomUUID().toString()));return 200;}catch(com.gym.identity.api.SessionFailure e){return e.status;}});
    assertThat(contenderStarted.await(10,TimeUnit.SECONDS)).isTrue();release.countDown();assertThat(first.get(10,TimeUnit.SECONDS)).isTrue();assertThat(second.get(10,TimeUnit.SECONDS)).isEqualTo(writeFirst?200:401);
    String next=login(name).get("sessionToken").asText();assertThat(data(call("GET","/v1/me/health-records",null,next,null)).get("items").size()).isEqualTo(writeFirst?1:0);
   }
  }
 }
 @Test void decimalProjectionStoresExactValueAndInfrastructureFailureRollsBack()throws Exception {
  String token=login(account()).get("sessionToken").asText();var health=context.getBean(com.gym.health.HealthService.class);var jdbc=context.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
  var body=Map.<String,Object>of("kind","WEIGHT","occurredAt","2026-11-13T00:00:00.000Z","payload",Map.of("weightKg","70.125"));
  org.assertj.core.api.Assertions.assertThatThrownBy(()->health.owned("Bearer "+token,o->{health.create(o,"RECORD",body,"rollback");jdbc.execute("select missing_synthetic_column from hlt_record");return true;})).isInstanceOf(org.springframework.dao.DataAccessException.class);
  assertThat(data(call("GET","/v1/me/health-records",null,token,null)).get("items").size()).isZero();
  String id=data(call("POST","/v1/me/health-records",body,token,"rollback")).get("id").asText();
  assertThat(jdbc.queryForObject("select weight_kg from hlt_record where id=?",java.math.BigDecimal.class,id)).isEqualByComparingTo("70.125");
 }
}
