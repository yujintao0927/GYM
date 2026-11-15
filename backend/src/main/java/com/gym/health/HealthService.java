package com.gym.health;

import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.support.TransactionTemplate;
import com.gym.identity.api.SessionFailure;
import com.gym.identity.session.SessionService;
import com.gym.identity.persistence.SessionRepository;
import com.gym.identity.local.LocalCredentialAuthenticator;
import tools.jackson.databind.json.JsonMapper;

@Service @Profile("local-dev")
public class HealthService {
 private final SessionRepository r; private final SessionService auth; private final TransactionTemplate tx;
 private final LocalCredentialAuthenticator crypto; private final Clock clock;
 private final JsonMapper json=JsonMapper.builder().build();
 public HealthService(SessionRepository r,SessionService auth,TransactionTemplate tx,LocalCredentialAuthenticator crypto,Clock clock){this.r=r;this.auth=auth;this.tx=tx;this.crypto=crypto;this.clock=clock;}
 private SessionFailure fail(int status){return new SessionFailure(status,switch(status){case 404->"RESOURCE_NOT_FOUND";case 409->"CONFLICT";default->"INVALID_INPUT";});}
 public <T> T owned(String bearer,Function<String,T> action){return tx.execute(s->action.apply(auth.authenticatedHealthActor(bearer)));}
 static void fields(Map<String,Object> b,Set<String> required,Set<String> allowed){if(!b.keySet().containsAll(required)||!allowed.containsAll(b.keySet()))throw new SessionFailure(422,"INVALID_INPUT");}
 private String text(Object o){if(!(o instanceof String s))throw fail(422);return s;}
 private String decimal(Object o,boolean zero,boolean percent){String s=text(o);if(!s.matches("[0-9]{1,7}(\\.[0-9]{1,3})?"))throw fail(422);var d=new BigDecimal(s);if(d.signum()<0||(!zero&&d.signum()==0)||percent&&d.compareTo(new BigDecimal("100"))>0)throw fail(422);return d.stripTrailingZeros().toPlainString();}
 private long time(Object o){try {String s=text(o);if(!s.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]{1,3})?(Z|[+-][0-9]{2}:[0-9]{2})") )throw fail(422);var t=OffsetDateTime.parse(s);if(t.getNano()%1000000!=0)throw fail(422);return t.toInstant().toEpochMilli();}catch(Exception e){throw fail(422);}}
 private String trimmed(Object o,int max){String s=text(o).strip();if(s.isEmpty()||s.codePointCount(0,s.length())>max)throw fail(422);return s;}
 private long version(Object o){if(!(o instanceof Integer||o instanceof Long)||((Number)o).longValue()<0)throw fail(422);return ((Number)o).longValue();}
 @SuppressWarnings("unchecked") private SortedMap<String,Object> payload(String kind,Object raw){
  if(!(raw instanceof Map<?,?>))throw fail(422);var p=(Map<String,Object>)raw;var out=new TreeMap<String,Object>();
  switch(kind){
   case "WEIGHT" -> {fields(p,Set.of("weightKg"),Set.of("weightKg"));out.put("weightKg",decimal(p.get("weightKg"),false,false));}
   case "DIET" -> {fields(p,Set.of("foodName","portionText"),Set.of("foodName","portionText"));out.put("foodName",trimmed(p.get("foodName"),120));out.put("portionText",trimmed(p.get("portionText"),200));}
   case "TRAINING" -> {fields(p,Set.of("name","durationMinutes"),Set.of("name","durationMinutes"));long n=version(p.get("durationMinutes"));if(n==0||n>Integer.MAX_VALUE)throw fail(422);out.put("name",trimmed(p.get("name"),120));out.put("durationMinutes",n);}
   case "MEASUREMENT" -> {fields(p,Set.of(),Set.of("heightCm","bodyFatPercent","waistCm","hipCm","chestCm"));if(p.isEmpty())throw fail(422);p.forEach((k,v)->out.put(k,decimal(v,k.equals("bodyFatPercent"),k.equals("bodyFatPercent"))));}
   default -> throw fail(422);
  }return out;
 }
 private String encode(Object o){return json.writeValueAsString(o);}
 private long n(Map<String,Object> row,String k){return r.number(row,k);}
 private Map<String,Object> record(String owner,String category,String id){var row=r.row("select * from hlt_record where account_id=? and id=? for update",owner,id);if(row==null||category.equals("MEASUREMENT")!=row.get("kind").equals("MEASUREMENT"))throw fail(404);return row;}
 private Map<String,Object> dto(Map<String,Object> row){var m=new LinkedHashMap<String,Object>();m.put("id",row.get("id"));if(!row.get("kind").equals("MEASUREMENT"))m.put("kind",row.get("kind"));m.put("payload",json.readTree((String)row.get("payload")));m.put("version",row.get("version"));for(String k:List.of("occurred_at","created_at","updated_at"))m.put(switch(k){case "occurred_at"->"occurredAt";case "created_at"->"createdAt";default->"updatedAt";},Instant.ofEpochMilli(n(row,k)).toString());return m;}
 public Object get(String owner,String cat,String id){return dto(record(owner,cat,id));}
 public Object create(String owner,String cat,Map<String,Object> b,String key){
  var allowed=cat.equals("MEASUREMENT")?Set.of("occurredAt","payload"):Set.of("kind","occurredAt","payload");fields(b,allowed,allowed);
  String kind=cat.equals("MEASUREMENT")?cat:text(b.get("kind"));if(!cat.equals("MEASUREMENT")&&!Set.of("WEIGHT","DIET","TRAINING").contains(kind))throw fail(422);
  long at=time(b.get("occurredAt"));String p=encode(payload(kind,b.get("payload")));
  if(key==null||!key.matches("[A-Za-z0-9._:-]{1,128}"))throw fail(422);
  String kd=crypto.fingerprint("health-key",key),fp=crypto.fingerprint("health-request",kind,Long.toString(at),p);
  var receipt=r.row("select * from hlt_receipt where account_id=? and operation=? and key_digest=? for update",owner,cat,kd);
  if(receipt!=null){if(!fp.equals(receipt.get("request_fingerprint")))throw fail(409);return get(owner,cat,(String)receipt.get("record_id"));}
  String id=UUID.randomUUID().toString();long now=clock.millis();
  r.db.update("insert into hlt_record(id,account_id,kind,occurred_at,payload,version,created_at,updated_at) values(?,?,?,?,?,1,?,?)",id,owner,kind,at,p,now,now);
  r.db.update("insert into hlt_receipt values(?,?,?,?,?)",owner,cat,kd,fp,id);return get(owner,cat,id);
 }
 public Object patch(String owner,String cat,String id,Map<String,Object> b){fields(b,Set.of("occurredAt","payload","expectedVersion"),Set.of("occurredAt","payload","expectedVersion"));var row=record(owner,cat,id);long v=version(b.get("expectedVersion"));if(v!=n(row,"version"))throw fail(409);long at=time(b.get("occurredAt"));String p=encode(payload((String)row.get("kind"),b.get("payload")));r.db.update("update hlt_record set occurred_at=?,payload=?,version=version+1,updated_at=? where id=? and account_id=?",at,p,clock.millis(),id,owner);return get(owner,cat,id);}
 public Object delete(String owner,String cat,String id,long expected){var row=record(owner,cat,id);if(expected!=n(row,"version"))throw fail(409);r.db.update("delete from hlt_record where id=? and account_id=?",id,owner);return Map.of("id",id,"deleted",true);}
 public long expected(String v){try{if(v==null||!v.matches("[0-9]{1,18}"))throw fail(422);return Long.parseLong(v);}catch(Exception e){throw fail(422);}}
 public Object list(String owner,String cat,Map<String,String> q){
  if(!Set.of("kind","from","to","limit","cursor").containsAll(q.keySet())||cat.equals("MEASUREMENT")&&q.containsKey("kind"))throw fail(422);
  String kind=q.getOrDefault("kind","");if(!kind.isEmpty()&&!Set.of("WEIGHT","DIET","TRAINING").contains(kind))throw fail(422);
  long from=q.containsKey("from")?time(q.get("from")):Long.MIN_VALUE,to=q.containsKey("to")?time(q.get("to")):Long.MAX_VALUE;
  long limit=q.containsKey("limit")?expected(q.get("limit")):20;if(limit<1||limit>100||from>=to)throw fail(422);
  String binding=crypto.fingerprint("health-filter",owner,cat,kind,Long.toString(from),Long.toString(to),Long.toString(limit));
  long asOf=clock.millis(),last=Long.MIN_VALUE;String id="";
  if(q.containsKey("cursor")){try{String[] parts=new String(Base64.getUrlDecoder().decode(q.get("cursor")),StandardCharsets.UTF_8).split("\\|",-1);if(parts.length!=4)throw fail(422);String raw=parts[0]+"|"+parts[1]+"|"+parts[2];if(!crypto.fingerprint("health-cursor",binding,raw).equals(parts[3]))throw fail(422);asOf=Long.parseLong(parts[0]);last=Long.parseLong(parts[1]);id=parts[2];UUID.fromString(id);}catch(Exception e){throw fail(422);}}
  String sql="select * from hlt_record where account_id=? and "+(cat.equals("MEASUREMENT")?"kind='MEASUREMENT'":"kind<>'MEASUREMENT'")+" and occurred_at>=? and occurred_at<? and created_at<=? and (occurred_at>? or (occurred_at=? and id>?))";
  var args=new ArrayList<Object>(List.of(owner,from,to,asOf,last,last,id));if(!kind.isEmpty()){sql+=" and kind=?";args.add(kind);}sql+=" order by occurred_at,id limit ?";args.add(limit+1);
  var rows=r.db.queryForList(sql,args.toArray());boolean more=rows.size()>limit;if(more)rows.removeLast();String next=null;if(more){var end=rows.getLast();String raw=asOf+"|"+n(end,"occurred_at")+"|"+end.get("id");next=Base64.getUrlEncoder().withoutPadding().encodeToString((raw+"|"+crypto.fingerprint("health-cursor",binding,raw)).getBytes(StandardCharsets.UTF_8));}
  var result=new LinkedHashMap<String,Object>();result.put("items",rows.stream().map(this::dto).toList());result.put("nextCursor",next);result.put("asOf",Instant.ofEpochMilli(asOf).toString());return result;
 }
 public Object goal(String owner){var row=r.row("select * from hlt_weight_goal where account_id=? for update",owner);if(row==null)throw fail(404);var m=new LinkedHashMap<String,Object>();m.put("targetWeightKg",((BigDecimal)row.get("target_weight_kg")).stripTrailingZeros().toPlainString());m.put("targetDate",row.get("target_date")==null?null:row.get("target_date").toString());m.put("version",row.get("version"));m.put("updatedAt",Instant.ofEpochMilli(n(row,"updated_at")).toString());return m;}
 public Object putGoal(String owner,Map<String,Object> b){fields(b,Set.of("targetWeightKg","expectedVersion"),Set.of("targetWeightKg","expectedVersion","targetDate"));String weight=decimal(b.get("targetWeightKg"),false,false);String date=null;if(b.containsKey("targetDate")){try{date=LocalDate.parse(text(b.get("targetDate"))).toString();if(!date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))throw fail(422);}catch(Exception e){throw fail(422);}}long v=version(b.get("expectedVersion"));var row=r.row("select * from hlt_weight_goal where account_id=? for update",owner);if(v!=(row==null?0:n(row,"version")))throw fail(409);if(row==null)r.db.update("insert into hlt_weight_goal values(?,?,?,?,?)",owner,weight,date,1,clock.millis());else r.db.update("update hlt_weight_goal set target_weight_kg=?,target_date=?,version=version+1,updated_at=? where account_id=?",weight,date,clock.millis(),owner);return goal(owner);}
 public Object deleteGoal(String owner,long v){var row=r.row("select * from hlt_weight_goal where account_id=? for update",owner);if(row==null)throw fail(404);if(v!=n(row,"version"))throw fail(409);r.db.update("delete from hlt_weight_goal where account_id=?",owner);return Map.of("deleted",true);}
}
