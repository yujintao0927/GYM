package com.gym.modules.operationscore.application;
import com.gym.modules.operationscore.ports.Dependencies.*;
import java.time.*;import java.util.*;
import com.gym.modules.operationscore.api.*;import com.gym.modules.operationscore.api.Models.*;import com.gym.modules.operationscore.domain.Rules;import com.gym.modules.operationscore.persistence.Repository;
public final class ProjectionService {
 private final Repository r;private final Access access;private final MetricPolicies policies;private final Clock clock;
 public ProjectionService(Repository r,Access a){this(r,a,s->null,Clock.systemUTC());}
 public ProjectionService(Repository r,Access a,MetricPolicies p,Clock clock){this.r=r;access=a;policies=p;this.clock=clock;}
 public void accept(Context c,Event e){r.transaction(()->{
  var store=access.check(c,"operations.project",e.storeId(),null);
  if(!store.merchantId().equals(e.merchantId()))throw new Failure(404,"NOT_FOUND");
  Commands.text(e.eventId(),64);Commands.text(e.aggregateId(),64);Commands.text(e.accountId(),64);
  String producer=switch(e.type()){case "RECEIVED","REFUNDED","NEW","RENEWAL","MEMBER"->"trade-core";case "VISIT"->"ranking-core";default->throw new Failure(422,"UNKNOWN_EVENT");};
  if(!producer.equals(e.producer())||e.schemaVersion()!=1||!"SIMULATED".equals(e.mode())||e.version()<1||e.amountMinor()<0||e.occurredAt()==null)throw new Failure(422,"INVALID_EVENT");
  String fp=Commands.hash(e);var inbox=r.row("select * from ops_inbox where event_id=?",e.eventId());if(inbox!=null){if(!fp.equals(inbox.get("fingerprint")))throw new Failure(409,"EVENT_CONFLICT");return null;}
  var prior=r.row("select * from ops_fact where producer=? and aggregate_id=? for update",producer,e.aggregateId());
  if(prior!=null){if(!e.storeId().equals(prior.get("store_id"))||!e.accountId().equals(prior.get("account_id"))||!e.type().equals(prior.get("type")))throw new Failure(409,"AGGREGATE_CONFLICT");long v=((Number)prior.get("version")).longValue();if(e.version()==v&&!Commands.hash(e.type(),e.amountMinor(),e.present(),e.reliable(),e.occurredAt()).equals(prior.get("fingerprint")))throw new Failure(409,"VERSION_CONFLICT");if(e.version()<=v){r.db.update("insert into ops_inbox values(?,?)",e.eventId(),fp);return null;}}
  String body=Commands.hash(e.type(),e.amountMinor(),e.present(),e.reliable(),e.occurredAt());
  if(prior==null)r.db.update("insert into ops_fact values(?,?,?,?,?,?,?,?,?,?,?,?,?)",producer,e.aggregateId(),e.version(),body,e.type(),e.merchantId(),e.storeId(),e.accountId(),e.amountMinor(),e.present(),e.reliable(),e.occurredAt().toEpochMilli(),e.mode());
  else r.db.update("update ops_fact set version=?,fingerprint=?,amount_minor=?,present=?,reliable=?,occurred_at=? where producer=? and aggregate_id=?",e.version(),body,e.amountMinor(),e.present(),e.reliable(),e.occurredAt().toEpochMilli(),producer,e.aggregateId());
  r.db.update("insert into ops_inbox values(?,?)",e.eventId(),fp);return null;
 });}
 /** Facts are cumulative snapshots per stable source aggregate, never deltas. */
 public Metrics metrics(Context c,String store,Instant from,Instant to,ZoneId zone,String mode){return r.transaction(()->{access.check(c,"operations.metrics",store,null);throw new Failure(422,"HISTORICAL_SNAPSHOT_UNSUPPORTED");});}
 public Metrics metrics(Context c,String store,Instant from,ZoneId zone,String mode){return r.transaction(()->{Instant to=clock.instant().plusNanos(1);
  access.check(c,"operations.metrics",store,null);MetricPolicy policy;try{policy=policies.get(store);}catch(RuntimeException e){throw new Failure(503,"POLICY_UNAVAILABLE");}if(policy==null||policy.approval()==null||policy.approval()==Approval.UNAPPROVED)throw new Failure(422,"POLICY_UNAPPROVED");if(from==null||to==null||zone==null||!from.isBefore(to)||!"SIMULATED".equals(mode))throw new Failure(422,"EXPLICIT_METRIC_SCOPE_REQUIRED");
  long received=0,refund=0,newMembers=0,renewals=0;Set<String> members=new HashSet<>(),today=new HashSet<>(),present=new HashSet<>();boolean estimated=false;
  LocalDate day=to.minusNanos(1).atZone(zone).toLocalDate();
  for(var row:r.db.queryForList("select * from ops_fact where store_id=? and mode=?",store,mode)){
   String type=(String)row.get("type"),account=(String)row.get("account_id");Instant time=Instant.ofEpochMilli(((Number)row.get("occurred_at")).longValue());if(!time.isBefore(to))continue;boolean in=!time.isBefore(from);boolean active=Boolean.TRUE.equals(row.get("present"));
   if(type.equals("MEMBER")&&active)members.add(account);
   if(type.equals("VISIT")){if(time.atZone(zone).toLocalDate().equals(day))today.add(account);if(active)present.add(account);if(!Boolean.TRUE.equals(row.get("reliable")))estimated=true;}
   if(in){long amount=((Number)row.get("amount_minor")).longValue();switch(type){case "RECEIVED"->received=Math.addExact(received,amount);case "REFUNDED"->refund=Math.addExact(refund,amount);case "NEW"->newMembers++;case "RENEWAL"->renewals++;default->{}}}
  }
  return new Metrics(received,refund,Rules.net(received,refund),newMembers,renewals,members.size(),today.size(),present.size(),estimated,mode);
 });}
}
