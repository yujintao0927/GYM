package com.gym.identity.session;

import java.time.*;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import com.gym.identity.api.SessionFailure;
import com.gym.identity.local.LocalCredentialAuthenticator;
import com.gym.identity.persistence.SessionRepository;

@Service @Profile("local-dev")
public class SessionService {
 private final SessionRepository r;private final LocalCredentialAuthenticator crypto;private final Clock clock;
 public SessionService(SessionRepository r,LocalCredentialAuthenticator crypto,Clock clock){this.r=r;this.crypto=crypto;this.clock=clock;}
 private long n(Map<String,Object> row,String key){return r.number(row,key);}
 private SessionFailure invalid(){return new SessionFailure(401,"SESSION_INVALID");}
 private String key(String key){if(key==null||!key.matches("[A-Za-z0-9._:-]{1,128}"))throw new SessionFailure(422,"INVALID_INPUT");return crypto.fingerprint("key",key);}
 private Map<String,Object> account(String id){return r.row("select * from iam_account where id=? for update",id);}
 private boolean active(Map<String,Object> a,Map<String,Object> s){return a!=null&&s!=null&&"ACTIVE".equals(a.get("status"))&&n(a,"session_epoch")==n(s,"account_epoch")&&s.get("revoked_at")==null&&clock.millis()<n(s,"absolute_expires_at");}
 private Map<String,Object> session(String id){return r.row("select * from iam_session where id=? for update",id);}
 private void revoke(String id){r.db.update("update iam_session set revoked_at=?,version=version+1 where id=?",clock.millis(),id);r.db.update("update iam_refresh_family set revoked_at=? where session_id=?",clock.millis(),id);}
 private String bucket(String domain,String value){return crypto.fingerprint(domain,value);}
 private void checkRate(String digest,int limit){
  long now=clock.millis();r.db.update("insert into iam_auth_failure values(?,?,0) on duplicate key update bucket_digest=bucket_digest",digest,now);
  var b=r.row("select * from iam_auth_failure where bucket_digest=? for update",digest);
  if(now-n(b,"window_start")>=900000){r.db.update("update iam_auth_failure set failure_count=0,window_start=? where bucket_digest=?",now,digest);return;}
  if(n(b,"failure_count")>=limit)throw new SessionFailure(429,"AUTH_RATE_LIMITED",Math.max(1,(n(b,"window_start")+900000-now+999)/1000));
 }
 private Map<String,Object> tokens(String id,String access,String refresh,long expires,long absolute,long generation){
  return Map.of("sessionId",id,"sessionToken",access,"refreshToken",refresh,"expiresAt",Instant.ofEpochMilli(expires).toString(),"refreshExpiresAt",Instant.ofEpochMilli(absolute).toString(),"authnSource","LOCAL_DEV","generation",generation);
 }
 public Map<String,Object> login(String login,String proof,String entry,String clientKind,String suppliedKey,String source){
  String name=crypto.login(login);crypto.password(proof);String kd=key(suppliedKey);
  if(entry==null||!Set.of("user-web","user-mini","merchant","coach","platform-admin","platform-support").contains(entry)||clientKind==null||!Set.of("WEB","MINI_PROGRAM").contains(clientKind))throw new SessionFailure(422,"INVALID_INPUT");
  String fp=crypto.fingerprint("login",name,proof,entry,clientKind);
  return r.transaction(()->{
   String ip=bucket("source",source),who=bucket("account",name);checkRate(ip,30);checkRate(who,5);
   var credential=r.row("select * from iam_local_credential where login_normalized=?",name);
   boolean match=crypto.verify(proof,credential==null?null:(String)credential.get("password_hash"));
   if(!match||credential==null||!Boolean.TRUE.equals(credential.get("enabled"))){
    r.db.update("update iam_auth_failure set failure_count=failure_count+1 where bucket_digest in (?,?)",ip,who);throw new SessionFailure(401,"AUTHENTICATION_FAILED");
   }
   String accountId=(String)credential.get("account_id");var a=account(accountId);
   // Re-read after Account fence so a credential change cannot pass a stale check.
   var current=r.row("select * from iam_local_credential where account_id=? for update",accountId);
   if(current==null||!Objects.equals(current.get("password_hash"),credential.get("password_hash"))||!Boolean.TRUE.equals(current.get("enabled")))throw new SessionFailure(401,"AUTHENTICATION_FAILED");
   if(!"ACTIVE".equals(a.get("status")))throw new SessionFailure(403,"ACCOUNT_CLOSED");
   var receipt=r.row("select * from iam_login_receipt where account_id=? and key_digest=? for update",accountId,kd);
   if(receipt!=null)throw new SessionFailure(409,fp.equals(receipt.get("request_fingerprint"))?"REAUTH_REQUIRED":"IDEMPOTENCY_MISMATCH");
   long now=clock.millis(),expires=now+1800000,absolute=now+604800000;String id=UUID.randomUUID().toString(),family=UUID.randomUUID().toString(),access=crypto.token(),refresh=crypto.token();
   r.db.update("insert into iam_session(id,account_id,account_epoch,access_digest,access_expires_at,absolute_expires_at) values(?,?,?,?,?,?)",id,accountId,n(a,"session_epoch"),crypto.digest(access),expires,absolute);
   r.db.update("insert into iam_refresh_family(id,session_id,absolute_expires_at) values(?,?,?)",family,id,absolute);
   r.db.update("insert into iam_refresh_node(digest,family_id,generation) values(?,?,0)",crypto.digest(refresh),family);
   r.db.update("insert into iam_login_receipt values(?,?,?,?,?)",accountId,kd,fp,id,now);
   return tokens(id,access,refresh,expires,absolute,0);
  });
 }
 public Map<String,Object> refresh(String token,long generation,String suppliedKey,String source){
  return r.transaction(()->{
   String ip=bucket("source",source);checkRate(ip,30);
   try{return rotate(token,generation,suppliedKey);}catch(SessionFailure failure){
    if(failure.status==401)r.db.update("update iam_auth_failure set failure_count=failure_count+1 where bucket_digest=?",ip);
    throw failure;
   }
  });
 }
 private Map<String,Object> rotate(String token,long generation,String suppliedKey){
  String kd=key(suppliedKey);if(token==null||!token.matches("[A-Za-z0-9_-]{43}")||generation<0)throw invalid();String digest=crypto.digest(token),fp=crypto.fingerprint("refresh",token,Long.toString(generation));
  return r.transaction(()->{
   var lookup=r.row("select s.account_id,s.id from iam_refresh_node n join iam_refresh_family f on n.family_id=f.id join iam_session s on f.session_id=s.id where n.digest=?",digest);if(lookup==null)throw invalid();
   var a=account((String)lookup.get("account_id"));String id=(String)lookup.get("id");var s=session(id);
   var f=r.row("select * from iam_refresh_family where session_id=? for update",id);
   var node=r.row("select * from iam_refresh_node where digest=? for update",digest);
   if(!active(a,s)||f==null||f.get("revoked_at")!=null||clock.millis()>=n(f,"absolute_expires_at"))throw invalid();
   if(node.get("consumed_at")!=null){
    if(kd.equals(node.get("consumed_key")))throw new SessionFailure(409,fp.equals(node.get("request_fingerprint"))?"REAUTH_REQUIRED":"IDEMPOTENCY_MISMATCH");
    revoke(id);throw invalid();
   }
   // Account fence serializes receipts across all of this actor's sessions.
   var priorKey=r.row("select n.digest from iam_session s join iam_refresh_family f on f.session_id=s.id join iam_refresh_node n on n.family_id=f.id where s.account_id=? and n.consumed_key=? for update",a.get("id"),kd);
   if(priorKey!=null)throw new SessionFailure(409,"IDEMPOTENCY_MISMATCH");
   if(n(node,"generation")!=generation)throw invalid();
   long now=clock.millis(),absolute=n(f,"absolute_expires_at"),expires=Math.min(now+1800000,absolute);String access=crypto.token(),refresh=crypto.token();
   r.db.update("update iam_refresh_node set consumed_at=?,consumed_key=?,request_fingerprint=? where digest=?",now,kd,fp,digest);
   r.db.update("insert into iam_refresh_node(digest,family_id,generation) values(?,?,?)",crypto.digest(refresh),f.get("id"),generation+1);
   r.db.update("update iam_session set access_digest=?,access_expires_at=?,version=version+1 where id=?",crypto.digest(access),expires,id);
   return tokens(id,access,refresh,expires,absolute,generation+1);
  });
 }
 private Map<String,Object> authenticate(String bearer){
  if(bearer==null||!bearer.matches("Bearer [A-Za-z0-9_-]{43}"))throw invalid();String digest=crypto.digest(bearer.substring(7));
  var lookup=r.row("select id,account_id from iam_session where access_digest=?",digest);if(lookup==null)throw invalid();
  var a=account((String)lookup.get("account_id"));var s=session((String)lookup.get("id"));
  if(!active(a,s)||!digest.equals(s.get("access_digest"))||clock.millis()>=n(s,"access_expires_at"))throw invalid();return s;
 }
 /** Called only inside the caller's transaction; holds Account then Session locks until completion. */
 public String authenticatedHealthActor(String bearer){
  if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Transaction required");
  return (String)authenticate(bearer).get("account_id");
 }
 public Map<String,Object> contexts(String bearer){return r.transaction(()->{var s=authenticate(bearer);return Map.of("accountId",s.get("account_id"),"staffScopes",List.of(),"entitlementSources",List.of(),"asOf",clock.instant().toString());});}
 public Map<String,Object> logout(String bearer,String id){return r.transaction(()->{
  var actor=authenticate(bearer);var target=r.row("select account_id from iam_session where id=?",id);
  if(target==null||!actor.get("account_id").equals(target.get("account_id")))throw new SessionFailure(404,"RESOURCE_NOT_FOUND");
  session(id);r.row("select * from iam_refresh_family where session_id=? for update",id);revoke(id);return Map.of("sessionId",id,"revoked",true);
 });}
}
