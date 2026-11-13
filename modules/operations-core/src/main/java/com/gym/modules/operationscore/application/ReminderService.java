package com.gym.modules.operationscore.application;
import java.time.*;import java.util.*;
import com.gym.modules.operationscore.api.*;import com.gym.modules.operationscore.api.Models.*;import com.gym.modules.operationscore.ports.Dependencies.*;import com.gym.modules.operationscore.persistence.Repository;
public final class ReminderService {
 private final Repository r;private final Access access;private final Facts facts;private final Policies policies;private final Clock clock;
 public ReminderService(Repository r,Access a,Facts f,Policies p,Clock c){this.r=r;access=a;facts=f;policies=p;clock=c;}
 public String evaluate(Context c,String store,String account,String kind,String policyId,long version){return r.transaction(()->{
  access.check(c,"operations.remind",store,account);
  return Commands.run(r,c,Commands.hash("remind",store,account,kind,policyId,version),()->{
  if(!Set.of("EXPIRY","SILENCE","STORE_EXPIRY").contains(kind))throw new Failure(422,"INVALID_KIND");
  Policy p;ContactFact f;try{p=policies.get(policyId,version);f=facts.require(account,store);}catch(RuntimeException e){throw new Failure(503,"DEPENDENCY_UNAVAILABLE");}
  if(p==null||p.approval()==null||p.approval()==Approval.UNAPPROVED)throw new Failure(422,"POLICY_UNAPPROVED");
  if(!policyId.equals(p.id())||version!=p.version()||p.expiryLead()==null||p.silence()==null||p.frequency()==null||p.expiryLead().isNegative()||p.silence().isNegative()||p.frequency().isNegative()||p.frequency().isZero())throw new Failure(422,"INVALID_POLICY");
  if(f==null||!account.equals(f.accountId())||!store.equals(f.storeId()))throw new Failure(503,"FACT_UNAVAILABLE");
  if(!f.consent()||f.unsubscribed())return null;
  Instant now=clock.instant();boolean due=kind.equals("SILENCE")?f.lastVisit()!=null&&!f.lastVisit().plus(p.silence()).isAfter(now):f.expiresAt()!=null&&!f.expiresAt().minus(p.expiryLead()).isAfter(now);
  if(!due)return null;
  r.db.update("insert into ops_contact_lock values(?,?) on duplicate key update account_id=account_id",store,account);
  r.row("select account_id from ops_contact_lock where store_id=? and account_id=? for update",store,account);
  // Facts must be local read adapters; recheck after acquiring the frequency fence.
  try{f=facts.require(account,store);}catch(RuntimeException e){throw new Failure(503,"FACT_UNAVAILABLE");}
  if(f==null||!account.equals(f.accountId())||!store.equals(f.storeId()))throw new Failure(503,"FACT_UNAVAILABLE");if(!f.consent()||f.unsubscribed())return null;
  var existing=r.row("select id from ops_message where store_id=? and account_id=? and kind=? and policy_id=? and policy_version=? and fact_version=? for update",store,account,kind,policyId,version,f.version());if(existing!=null)return (String)existing.get("id");
  if(r.row("select id from ops_message where store_id=? and account_id=? and created_at>? order by created_at desc,id limit 1 for update",store,account,now.minus(p.frequency()).toEpochMilli())!=null)return null;
  due=kind.equals("SILENCE")?f.lastVisit()!=null&&!f.lastVisit().plus(p.silence()).isAfter(now):f.expiresAt()!=null&&!f.expiresAt().minus(p.expiryLead()).isAfter(now);if(!due)return null;
  String id=UUID.randomUUID().toString();r.db.update("insert into ops_message values(?,?,?,?,?,?,?,?)",id,store,account,kind,policyId,version,f.version(),clock.millis());return id;
 });});}
 public List<Message> messages(Context c,Cursor cursor,int limit){return r.transaction(()->{
  access.check(c,"operations.messages",null,c.actorId());if(limit<1||limit>100)throw new Failure(422,"INVALID_PAGE");if(cursor!=null&&(!c.actorId().equals(cursor.actorId())||cursor.storeId()!=null||!"messages".equals(cursor.filter())))throw new Failure(404,"NOT_FOUND");
  return r.db.queryForList("select id,kind,created_at from ops_message where account_id=? and id>? order by id limit ?",c.actorId(),cursor==null?"":cursor.after(),limit).stream().map(x->new Message((String)x.get("id"),(String)x.get("kind"),Instant.ofEpochMilli(((Number)x.get("created_at")).longValue()))).toList();
 });}
}
