package com.gym.modules.rankingcore.application;
import com.gym.modules.rankingcore.persistence.Database;
import com.gym.modules.rankingcore.ports.*;
import com.gym.modules.rankingcore.api.Failure;
import com.gym.modules.rankingcore.domain.RankingRules;
import static com.gym.modules.rankingcore.persistence.Database.*;
import java.sql.*;
import java.time.*;
import java.util.*;
public final class RankingService  {
    public record Row(String nickname,long days,long rank) {
    }
    private final Database db;
    private final Dependencies deps;
    private final RankingPolicy policy;
    private final Clock clock;
    public RankingService(Database db,Dependencies deps,RankingPolicy policy,Clock clock) {
        this.db=db;
        this.deps=deps;
        this.policy=policy;
        this.clock=clock;
    }
    private void guard(String actor,String owner,String store,String action,boolean rights) {
        deps.active(actor);
        deps.active(owner);
        var s=store==null?null:deps.store(store);
        if(store!=null&&(s==null||!store.equals(s.id())))throw new Failure(503,"STORE_UNAVAILABLE");
        var d=deps.authorize(actor,action,s,owner);
        if(d!=Dependencies.Decision.ALLOW)throw new Failure(d==Dependencies.Decision.UNAVAILABLE?503:403,"ACCESS_DENIED");
        if(rights&&deps.right(owner,store,store==null?"PERSONAL_RANKING":"STORE_RANKING")==null)throw new Failure(503,"RIGHTS_UNAVAILABLE");
    }
    private void text(String s) {
        if(s==null||s.isBlank()||s.length()>120)throw new Failure(422,"INVALID_INPUT");
    }
    private String scope(String type,String store) {
        if(!Set.of("PERSONAL","STORE").contains(type)||(type.equals("STORE")!=(store!=null)))throw new Failure(422,"INVALID_SCOPE");
        return type+":"+Objects.toString(store,"");
    }
    private RankingPolicy.Config configured() {
        var p=policy.get();
        return p==null||p.status()==RankingPolicy.Status.UNAPPROVED?null:p;
    }
    public String bracelet(String actor,String event,String owner,String device,Instant start,Instant end) {
        text(event);
        if(event.startsWith("visit:")||event.startsWith("region:"))throw new Failure(422,"RESERVED_EVENT_NAMESPACE");
        text(owner);
        text(device);
        if(start==null||end==null||end.isBefore(start)||end.isAfter(clock.instant()))throw new Failure(422,"INVALID_EVENT");
        return db.transaction(c-> {
            guard(actor,owner,null,"ranking.bracelet.ingest",false);deps.requireSimulatedDevice(owner,device);lock(c,"event:"+event);
            String body=owner.length()+":"+owner+device.length()+":"+device+":"+start+":"+end;
            String old=scalar(c,"SELECT body FROM rnk_raw WHERE event_id=?",event);if(old!=null) {
                if(!old.equals(body))throw new Failure(409,"EVENT_CONFLICT");return scalar(c,"SELECT qualification FROM rnk_raw WHERE event_id=?",event);
            }
            var p=configured();String q=new RankingRules().qualify(p==null?null:p.thresholdMillis(),Duration.between(start,end).toMillis());
            execute(c,"INSERT INTO rnk_raw(event_id,subject_id,source,scope_key,body,qualification,started_at,ended_at) VALUES (?,?,'SIMULATED_BRACELET','PERSONAL:',?,?,?,?)",event,owner,body,q,start.toEpochMilli(),end.toEpochMilli());
            if(q.equals("VALID"))execute(c,"INSERT IGNORE INTO rnk_day(subject_id,scope_key,valid_date) VALUES (?,'PERSONAL:',?)",owner,start.atZone(p.zone()).toLocalDate().toString());
            execute(c,"INSERT INTO rnk_outbox(event_id,type,schema_version,producer,aggregate_id,aggregate_version,occurred_at,mode,payload) VALUES (?,'BraceletObserved',1,'ranking-core',?,0,?,'SIMULATED',JSON_OBJECT('sourceEventId',?,'subjectId',?,'source','SIMULATED_BRACELET','qualification',?))",UUID.randomUUID().toString(),UUID.randomUUID().toString(),clock.millis(),event,owner,q);
            return q;
        }
        );
    }
    public void participate(String actor,String type,String store,boolean enabled,String nickname,long expectedVersion) {
        String sk=scope(type,store);
        final String name=nickname==null?deps.username(actor):nickname;
        text(name);
        db.transaction(c-> {
            guard(actor,actor,store,"ranking.participate",true);lock(c,"participant:"+actor+":"+sk);String version=scalar(c,"SELECT version FROM rnk_participant WHERE subject_id=? AND scope_key=?",actor,sk);if(version==null) {
                if(expectedVersion!=-1)throw new Failure(409,"VERSION_CONFLICT");execute(c,"INSERT INTO rnk_participant(subject_id,scope_key,enabled,nickname) VALUES (?,?,?,?)",actor,sk,enabled,name);
            }
            else {
                if(Long.parseLong(version)!=expectedVersion)throw new Failure(409,"VERSION_CONFLICT");execute(c,"UPDATE rnk_participant SET enabled=?,nickname=?,version=version+1 WHERE subject_id=? AND scope_key=?",enabled,name,actor,sk);
            }
            return null;
        }
        );
    }
    /** Explicit half-open date window; null/null = all time. Policy must approve the caller's window upstream. */
    public List<Row> board(String actor,String type,String store,String regionLevel,String regionCode,LocalDate from,LocalDate until,int limit,int offset) {
        String sk=scope(type,store);
        if(limit<1||limit>100||offset<0||(from==null)!=(until==null)||(from!=null&&!from.isBefore(until)))throw new Failure(422,"INVALID_PAGE");
        if(!Set.of("COUNTRY","PROVINCE","CITY","DISTRICT").contains(regionLevel))throw new Failure(422,"INVALID_REGION");
        if(!"COUNTRY".equals(regionLevel)) {
            if(!"PERSONAL".equals(type))throw new Failure(422,"INVALID_REGION");
            text(regionCode);
        }
        return db.transaction(c-> {
            guard(actor,actor,store,"ranking.board",true);var p=configured();if(p==null||(p.status()!=RankingPolicy.Status.CONFIGURED_FIXTURE&&(!policy.approvesWindow(from,until)||policy.tieMode()==null)))throw new Failure(422,"PENDING_POLICY");
            String filter=from==null?"":" AND d.valid_date>=? AND d.valid_date<?";if(!"COUNTRY".equals(regionLevel))filter+=" AND EXISTS (SELECT 1 FROM rnk_participant reg WHERE reg.subject_id=p.subject_id AND reg.scope_key=? AND reg.nickname=?)";
            String sql="SELECT p.nickname,count(d.valid_date) AS days FROM rnk_participant p JOIN rnk_day d ON p.subject_id=d.subject_id AND p.scope_key=d.scope_key WHERE p.scope_key=? AND p.enabled=TRUE"+filter+" GROUP BY p.subject_id,p.nickname ORDER BY days DESC,p.subject_id";
            var all=new ArrayList<Row>();try(var q=c.prepareStatement(sql)) {
                q.setString(1,sk);if(from!=null) {
                    q.setString(2,from.toString());q.setString(3,until.toString());
                }
                if(!"COUNTRY".equals(regionLevel)) {
                    int index=from==null?2:4;q.setString(index,"REGION:"+regionLevel);q.setString(index+1,regionCode);
                }
                try(var r=q.executeQuery()) {
                    long last=-1,rank=0;while(r.next()) {
                        long days=r.getLong("days");if(days!=last)rank=policy.tieMode()==RankingPolicy.TieMode.DENSE?rank+1:all.size()+1;all.add(new Row(r.getString("nickname"),days,rank));last=days;
                    }
                }
            }
            if(all.size()<p.minimumParticipants())throw new Failure(422,"INSUFFICIENT_PARTICIPANTS");return List.copyOf(all.subList(Math.min(offset,all.size()),Math.min(offset+limit,all.size())));
        }
        );
    }
    public RegionResolver.Region region(String actor,String authorizationReference,RegionResolver resolver) {
        text(authorizationReference);
        guard(actor,actor,null,"ranking.region",true);
        var p=configured();
        if(p==null||p.status()!=RankingPolicy.Status.CONFIGURED_FIXTURE)throw new Failure(422,"PENDING_POLICY");
        var region=resolver.resolve(actor,authorizationReference);
        if(region==null)throw new Failure(503,"REGION_UNAVAILABLE");
        return db.transaction(c-> {
            guard(actor,actor,null,"ranking.region",true);lock(c,"region:"+actor);String event="region:"+actor+":"+clock.instant().atZone(p.zone()).toLocalDate().withDayOfMonth(1);if(scalar(c,"SELECT event_id FROM rnk_raw WHERE event_id=?",event)!=null)throw new Failure(409,"REGION_CHANGE_LIMIT");for(String[] part:new String[][] {
                {
                    "PROVINCE",region.province()
                }
                , {
                    "CITY",region.city()
                }
                , {
                    "DISTRICT",region.district()
                }
            }
            )execute(c,"INSERT INTO rnk_participant(subject_id,scope_key,enabled,nickname) VALUES (?,?,FALSE,?) ON DUPLICATE KEY UPDATE nickname=VALUES(nickname),version=version+1",actor,"REGION:"+part[0],part[1]);execute(c,"INSERT INTO rnk_raw(event_id,subject_id,source,scope_key,body,qualification,started_at,ended_at) VALUES (?,?,'REGION','REGION:','','PENDING_POLICY',?,?)",event,actor,clock.millis(),clock.millis());return region;
        }
        );
    }
    public String visit(String actor,String visitId) {
        return db.transaction(c-> {
            String owner=scalar(c,"SELECT subject_id FROM vis_visit WHERE id=?",visitId);if(owner==null)throw new Failure(404,"NOT_FOUND");String store=scalar(c,"SELECT store_id FROM vis_visit WHERE id=?",visitId);guard(actor,owner,store,"ranking.visit.ingest",false);lock(c,owner+":"+store);String version=scalar(c,"SELECT version FROM vis_visit WHERE id=? FOR UPDATE",visitId);String event="visit:"+visitId+":"+version;String old=scalar(c,"SELECT qualification FROM rnk_raw WHERE event_id=? AND source='VISIT' AND subject_id=? FOR UPDATE",event,owner);if(old!=null)return old;String left=scalar(c,"SELECT left_at FROM vis_visit WHERE id=? FOR UPDATE",visitId);String entered=scalar(c,"SELECT entered_at FROM vis_visit WHERE id=? FOR UPDATE",visitId);var p=configured();String q=left==null?"PENDING_POLICY":new RankingRules().qualify(p==null?null:p.thresholdMillis(),Long.parseLong(left)-Long.parseLong(entered));String sk="STORE:"+store;execute(c,"UPDATE rnk_raw SET qualification='SUPERSEDED' WHERE source='VISIT' AND event_id LIKE ?", "visit:"+visitId+":%");execute(c,"INSERT INTO rnk_raw(event_id,subject_id,source,scope_key,body,qualification,started_at,ended_at) VALUES (?,?,'VISIT',?,?,?, ?,?)",event,owner,sk,visitId,q,Long.parseLong(entered),left==null?Long.parseLong(entered):Long.parseLong(left));if(p!=null) {
                execute(c,"DELETE FROM rnk_day WHERE subject_id=? AND scope_key=?",owner,sk);try(var stmt=c.prepareStatement("SELECT started_at FROM rnk_raw WHERE subject_id=? AND scope_key=? AND qualification='VALID' AND source='VISIT' FOR UPDATE")) {
                    bind(stmt,owner,sk);try(var r=stmt.executeQuery()) {
                        while(r.next())execute(c,"INSERT IGNORE INTO rnk_day(subject_id,scope_key,valid_date) VALUES (?,?,?)",owner,sk,Instant.ofEpochMilli(r.getLong(1)).atZone(p.zone()).toLocalDate().toString());
                    }
                }
            }
            return q;
        }
        );
    }
    public record Appeal(String id,String eventId,String subjectId,String reason,String state) {
    }
    public Appeal appealRead(String actor,String id) {
        return db.transaction(c-> {
            try(var q=c.prepareStatement("SELECT * FROM rnk_appeal WHERE id=?")) {
                bind(q,id);try(var r=q.executeQuery()) {
                    if(!r.next())throw new Failure(404,"NOT_FOUND");guard(actor,r.getString("subject_id"),null,"ranking.appeal.read",false);return new Appeal(id,r.getString("event_id"),r.getString("subject_id"),r.getString("reason"),r.getString("state"));
                }
            }
        }
        );
    }
    /** Review records evidence only. Altering scores requires a separately approved policy. */
    public void appealReview(String actor,String id,String evidence) {
        text(evidence);
        db.transaction(c-> {
            String owner=scalar(c,"SELECT subject_id FROM rnk_appeal WHERE id=?",id);if(owner==null)throw new Failure(404,"NOT_FOUND");guard(actor,owner,null,"ranking.appeal.review",false);lock(c,"appeal:"+id);if(!"PENDING_REVIEW".equals(scalar(c,"SELECT state FROM rnk_appeal WHERE id=? FOR UPDATE",id)))throw new Failure(409,"APPEAL_STATE_CONFLICT");execute(c,"UPDATE rnk_appeal SET state='REVIEWED' WHERE id=?",id);execute(c,"INSERT INTO rnk_outbox(event_id,type,schema_version,producer,aggregate_id,aggregate_version,occurred_at,mode,payload) VALUES (?,'AppealReviewed',1,'ranking-core',?,1,?,'SIMULATED',JSON_OBJECT('actorId',?,'evidenceRef',?))",UUID.randomUUID().toString(),id,clock.millis(),actor,evidence);return null;
        }
        );
    }
    public String appeal(String actor,String event,String reason) {
        text(reason);
        return db.transaction(c-> {
            String owner=scalar(c,"SELECT subject_id FROM rnk_raw WHERE event_id=?",event);if(owner==null)throw new Failure(404,"NOT_FOUND");guard(actor,owner,null,"ranking.appeal",false);String id=UUID.randomUUID().toString();execute(c,"INSERT INTO rnk_appeal(id,event_id,subject_id,reason,created_at) VALUES (?,?,?,?,?)",id,event,owner,reason,clock.millis());return id;
        }
        );
    }
}
