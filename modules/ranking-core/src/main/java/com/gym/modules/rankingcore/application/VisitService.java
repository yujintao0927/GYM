package com.gym.modules.rankingcore.application;
import com.gym.modules.rankingcore.api.*;
import com.gym.modules.rankingcore.ports.Dependencies;
import com.gym.modules.rankingcore.persistence.Database;
import java.sql.*;
import java.time.*;
import java.util.*;
import static com.gym.modules.rankingcore.persistence.Database.*;
/** All public operations recheck current authorization; no HTTP or runtime fixture. */
public final class VisitService  {
    private final Database db;
    private final Dependencies ports;
    private final Clock clock;
    public VisitService(Database db,Dependencies ports,Clock clock) {
        this.db=db;
        this.ports=ports;
        this.clock=clock;
    }
    private Dependencies.Store guard(String actor,String subject,String store,String action) {
        ports.active(actor);
        ports.active(subject);
        var s=ports.store(store);
        if(s==null||!s.id().equals(store))throw new Failure(503,"STORE_UNAVAILABLE");
        var decision=ports.authorize(actor,action,s,subject);
        if(decision!=Dependencies.Decision.ALLOW)throw new Failure(decision==Dependencies.Decision.UNAVAILABLE?503:403,"ACCESS_DENIED");
        return s;
    }
    private void text(String s) {
        if(s==null||s.isBlank()||s.length()>120)throw new Failure(422,"INVALID_INPUT");
    }
    public String challenge(String actor,String store,Instant expiresAt) {
        text(actor);
        text(store);
        return db.transaction(c-> {
            guard(actor,actor,store,"visit.challenge");if(expiresAt==null||!expiresAt.isAfter(clock.instant()))throw new Failure(422,"INVALID_EXPIRY");String id=UUID.randomUUID().toString();execute(c,"INSERT INTO vis_challenge(id,subject_id,store_id,expires_at) VALUES (?,?,?,?)",id,actor,store,expiresAt.toEpochMilli());return id;
        }
        );
    }
    public Visit enter(String actor,String subject,String store,String key,String challenge,String manualReason,long expectedRightVersion) {
        text(actor);
        text(subject);
        text(store);
        text(key);
        if((challenge==null)==(manualReason==null))throw new Failure(422,"ENTRY_SOURCE_REQUIRED");
        if(manualReason!=null)text(manualReason);
        return db.transaction(c-> {
            var s=guard(actor,subject,store,manualReason==null?"visit.enter.qr":"visit.enter.manual");
            lock(c,"idem:"+actor+":"+key);lock(c,subject+":"+store);String fingerprint=java.util.Base64.getEncoder().encodeToString((subject.length()+":"+subject+store.length()+":"+store+Objects.toString(challenge,"").length()+":"+Objects.toString(challenge,"")+Objects.toString(manualReason,"").length()+":"+Objects.toString(manualReason,"")+":"+expectedRightVersion).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String receipt=scalar(c,"SELECT body FROM rnk_receipt WHERE actor_id=? AND idem_key=? FOR UPDATE",actor,key);
            if(receipt!=null) {
                if(!receipt.substring(0,receipt.indexOf("#")).equals(fingerprint))throw new Failure(409,"IDEMPOTENCY_CONFLICT");return receiptDecode(receipt.substring(receipt.indexOf("#")+1));
            }
            if(scalar(c,"SELECT id FROM vis_visit WHERE subject_id=? AND store_id=? AND left_at IS NULL",subject,store)!=null)throw new Failure(409,"ALREADY_IN");
            Instant now=clock.instant();
            if(challenge!=null) {
                String valid=scalar(c,"SELECT id FROM vis_challenge WHERE id=? AND subject_id=? AND store_id=? AND used=FALSE AND expires_at>? FOR UPDATE",challenge,subject,store,now.toEpochMilli());if(valid==null)throw new Failure(409,"CHALLENGE_INVALID");
            }
            var right=ports.right(subject,store,"ENTRY");if(right==null)throw new Failure(503,"RIGHTS_UNAVAILABLE");if(right.version()!=expectedRightVersion)throw new Failure(409,"RIGHT_VERSION_CONFLICT");
            String id=UUID.randomUUID().toString();String consumed=ports.consume(c,right,subject,store,id,now);if(consumed==null)throw new Failure(503,"RIGHTS_UNAVAILABLE");
            execute(c,"INSERT INTO vis_visit(id,subject_id,store_id,merchant_id,entered_at,source,actor_id,reason,consumption_id) VALUES (?,?,?,?,?,?,?,?,?)",id,subject,store,s.merchantId(),now.toEpochMilli(),challenge==null?"MANUAL":"QR",actor,manualReason,consumed);
            if(challenge!=null)execute(c,"UPDATE vis_challenge SET used=TRUE WHERE id=?",challenge);
            execute(c,"INSERT INTO rnk_receipt(actor_id,idem_key,body,resource_id) VALUES (?,?,?,?)",actor,key,fingerprint+"#"+receiptEncode(get(c,id)),id);
            event(c,id,s,"VisitEntered",0,now);return get(c,id);
        }
        );
    }
    public Visit read(String actor,String id) {
        return db.transaction(c-> {
            Visit v=get(c,id);guard(actor,v.subjectId(),v.storeId(),"visit.read");return v;
        }
        );
    }
    public Visit exit(String actor,String id,String key,long expectedVersion) {
        text(key);
        return db.transaction(c-> {
            Visit initial=peek(c,id);guard(actor,initial.subjectId(),initial.storeId(),"visit.exit");lock(c,"idem:"+actor+":"+key);lock(c,initial.subjectId()+":"+initial.storeId());Visit v=get(c,id);
            String body="exit|"+id+"|"+expectedVersion;String old=scalar(c,"SELECT body FROM rnk_receipt WHERE actor_id=? AND idem_key=? FOR UPDATE",actor,key);if(old!=null) {
                if(!old.substring(0,old.indexOf("#")).equals(body))throw new Failure(409,"IDEMPOTENCY_CONFLICT");return receiptDecode(old.substring(old.indexOf("#")+1));
            }
            if(v.version()!=expectedVersion||v.leftAt()!=null)throw new Failure(409,"VERSION_CONFLICT");
            Instant at=clock.instant();execute(c,"UPDATE vis_visit SET left_at=?,version=version+1 WHERE id=?",at.toEpochMilli(),id);
            execute(c,"INSERT INTO rnk_receipt(actor_id,idem_key,body,resource_id) VALUES (?,?,?,?)",actor,key,body+"#"+receiptEncode(get(c,id)),id);
            event(c,id,new Dependencies.Store(v.storeId(),v.merchantId(),0),"VisitExited",v.version()+1,at);return get(c,id);
        }
        );
    }
    public Visit correct(String actor,String id,long expectedVersion,Instant leftAt,String reason) {
        text(reason);
        return db.transaction(c-> {
            Visit initial=peek(c,id);guard(actor,initial.subjectId(),initial.storeId(),"visit.correct");lock(c,initial.subjectId()+":"+initial.storeId());Visit v=get(c,id);if(v.version()!=expectedVersion)throw new Failure(409,"VERSION_CONFLICT");if(leftAt==null||leftAt.isBefore(v.enteredAt()))throw new Failure(422,"INVALID_CORRECTION");execute(c,"UPDATE vis_visit SET left_at=?,version=version+1 WHERE id=?",leftAt.toEpochMilli(),id);String event=UUID.randomUUID().toString();execute(c,"INSERT INTO rnk_outbox(event_id,type,schema_version,producer,aggregate_id,aggregate_version,occurred_at,merchant_id,store_id,mode,payload) VALUES (?,'VisitCorrected',1,'ranking-core',?,?,?,?,?,'SIMULATED',JSON_OBJECT('actorId',?,'reason',?,'previousLeftAt',?,'leftAt',?))",event,id,v.version()+1,clock.millis(),v.merchantId(),v.storeId(),actor,reason,v.leftAt()==null?null:v.leftAt().toEpochMilli(),leftAt.toEpochMilli());return get(c,id);
        }
        );
    }
    public List<Visit> list(String actor,String subject,String store,String afterId,int limit) {
        if(limit<1||limit>100)throw new Failure(422,"INVALID_PAGE");
        return db.transaction(c-> {
            guard(actor,subject,store,"visit.read");if(afterId!=null&&scalar(c,"SELECT id FROM vis_visit WHERE id=? AND subject_id=? AND store_id=?",afterId,subject,store)==null)throw new Failure(404,"INVALID_CURSOR");var result=new ArrayList<Visit>();try(var q=c.prepareStatement("SELECT id FROM vis_visit WHERE subject_id=? AND store_id=? AND id>? ORDER BY id LIMIT ?")) {
                bind(q,subject,store,Objects.toString(afterId,""),limit);try(var r=q.executeQuery()) {
                    while(r.next())result.add(get(c,r.getString(1)));
                }
            }
            return List.copyOf(result);
        }
        );
    }
    private Visit get(Connection c,String id)throws SQLException {
        return load(c,id,true);
    }
    private Visit peek(Connection c,String id)throws SQLException {
        return load(c,id,false);
    }
    private Visit load(Connection c,String id,boolean locked)throws SQLException {
        try(var q=c.prepareStatement("SELECT * FROM vis_visit WHERE id=?"+(locked?" FOR UPDATE":""))) {
            q.setString(1,id);
            try(var r=q.executeQuery()) {
                if(!r.next())throw new Failure(404,"NOT_FOUND");
                Long left=(Long)r.getObject("left_at");
                return new Visit(id,r.getString("subject_id"),r.getString("store_id"),r.getString("merchant_id"),Instant.ofEpochMilli(r.getLong("entered_at")),left==null?null:Instant.ofEpochMilli(left),r.getString("source"),r.getString("consumption_id"),r.getLong("version"));
            }
        }
    }
    private String receiptEncode(Visit v) {
        return java.util.stream.Stream.of(v.id(),v.subjectId(),v.storeId(),v.merchantId(),v.enteredAt().toString(),v.leftAt()==null?"":v.leftAt().toString(),v.source(),v.consumptionId(),Long.toString(v.version())).map(x->Base64.getEncoder().encodeToString(x.getBytes(java.nio.charset.StandardCharsets.UTF_8))).collect(java.util.stream.Collectors.joining(";"));
    }
    private Visit receiptDecode(String encoded) {
        String[] a=Arrays.stream(encoded.split(";",-1)).map(x->new String(Base64.getDecoder().decode(x),java.nio.charset.StandardCharsets.UTF_8)).toArray(String[]::new);
        return new Visit(a[0],a[1],a[2],a[3],Instant.parse(a[4]),a[5].isEmpty()?null:Instant.parse(a[5]),a[6],a[7],Long.parseLong(a[8]));
    }
    private void event(Connection c,String id,Dependencies.Store s,String type,long version,Instant now)throws SQLException {
        execute(c,"INSERT INTO rnk_outbox(event_id,type,schema_version,producer,aggregate_id,aggregate_version,occurred_at,merchant_id,store_id,mode,payload) VALUES (?,?,1,'ranking-core',?,?,?,?,?,'SIMULATED',JSON_OBJECT('visitId',?,'subjectId',?,'source',?,'enteredAt',?,'leftAt',?))",UUID.randomUUID().toString(),type,id,version,now.toEpochMilli(),s.merchantId(),s.id(),id,get(c,id).subjectId(),get(c,id).source(),get(c,id).enteredAt().toEpochMilli(),get(c,id).leftAt()==null?null:get(c,id).leftAt().toEpochMilli());
    }
}
