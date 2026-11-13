package com.gym.modules.supportcore.application;
import com.gym.modules.supportcore.api.*;
import com.gym.modules.supportcore.api.SupportModels.*;
import com.gym.modules.supportcore.domain.TicketState;
import com.gym.modules.supportcore.ports.SupportPorts.*;
import javax.sql.DataSource;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DuplicateKeyException;

/** Trusted IAM context required. Port adapters must participate in the same database transaction. */
public class SupportService {
 private final JdbcTemplate db; private final TransactionTemplate tx;
 private final Authorization auth; private final Accounts accounts; private final Stores stores; private final Trade trade;
 private static final RowMapper<Ticket> M=(r,n)->new Ticket(r.getString("id"),r.getString("owner_id"),r.getString("merchant_id"),r.getString("store_id"),r.getString("kind"),r.getString("state"),r.getString("assignee_id"),r.getLong("version"));
 public SupportService(DataSource ds,Authorization auth,Accounts accounts,Stores stores,Trade trade) {
  db=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
  this.auth=Objects.requireNonNull(auth);this.accounts=Objects.requireNonNull(accounts);this.stores=Objects.requireNonNull(stores);this.trade=Objects.requireNonNull(trade);
 }
 private void context(Context c) {if(c==null)fail(401,"INVALID_ACTOR");valid(c.actorId(),100);valid(c.requestId(),100);valid(c.idempotencyKey(),100);dependency(()->{accounts.requireActive(c.actorId());return true;});}
 private static void valid(String s,int max){if(s==null||s.isBlank()||s.length()>max)fail(422,"INVALID_INPUT");}
 private static void fail(int s,String c){throw new SupportFailure(s,c);}
 private static <T> T dependency(java.util.function.Supplier<T> call){try {T v=call.get();if(v==null)fail(503,"DEPENDENCY_UNAVAILABLE");return v;}catch(SupportFailure e){throw e;}catch(RuntimeException e){throw new SupportFailure(503,"DEPENDENCY_UNAVAILABLE");}}
 private void permit(Context c,String action,Ticket t){
  var d=dependency(()->auth.authorize(c.actorId(),"support."+action,t.merchantId(),t.storeId(),t.ownerId()));
  if(d==Decision.UNAVAILABLE)fail(503,"AUTHORIZATION_UNAVAILABLE");if(d!=Decision.ALLOW)fail(403,"ACCESS_DENIED");
 }
 private Ticket load(String id,boolean lock){valid(id,36);var rows=db.query("select * from sup_ticket where id=?"+(lock?" for update":""),M,id);if(rows.isEmpty())fail(404,"NOT_FOUND");return rows.getFirst();}
 private void access(Context c,Ticket t,String action,boolean ownerOrAssignee){
  if(ownerOrAssignee&&!c.actorId().equals(t.ownerId())&&!c.actorId().equals(t.assigneeId()))fail(404,"NOT_FOUND");
  permit(c,action,t);
 }
 private String digest(Object... fields){try {var md=MessageDigest.getInstance("SHA-256");for(Object x:fields){byte[] b=x instanceof byte[] a?a:String.valueOf(x).getBytes(StandardCharsets.UTF_8);md.update(java.nio.ByteBuffer.allocate(4).putInt(b.length).array());md.update(b);}return HexFormat.of().formatHex(md.digest());}catch(Exception e){throw new IllegalStateException("DIGEST_UNAVAILABLE");}}
 private String receipt(Context c,String digest){var r=db.queryForList("select digest,result_id from sup_receipt where actor_id=? and idem_key=?",c.actorId(),c.idempotencyKey());if(r.isEmpty())return null;if(!digest.equals(r.getFirst().get("digest")))fail(409,"IDEMPOTENCY_CONFLICT");return (String)r.getFirst().get("result_id");}
 private void record(Context c,String digest,String result){db.update("insert into sup_receipt values(?,?,?,?)",c.actorId(),c.idempotencyKey(),digest,result);}
 private <T> T write(java.util.function.Supplier<T> action){try{return tx.execute(s->action.get());}catch(DuplicateKeyException e){throw new SupportFailure(409,"CONCURRENT_CONFLICT");}}
 private void audit(Context c,Ticket t,String action,String text){
  db.update("insert into sup_entry(ticket_id,action,actor_id,text,request_id) values(?,?,?,?,?)",t.id(),action,c.actorId(),text,c.requestId());
  db.update("insert into sup_outbox(event_id,type,schema_version,producer,aggregate_id,aggregate_version,merchant_id,store_id,mode,payload) values(?,?,1,'support-core',?,?,?,?,'SIMULATED',JSON_OBJECT('state',?,'action',?))",UUID.randomUUID().toString(),"SupportTicketChanged",t.id(),t.version(),t.merchantId(),t.storeId(),t.state(),action);
 }
 protected void beforeCommit() {}
 public Ticket open(Context c,Open command){return write(()->{
  context(c);valid(command.storeId(),100);valid(command.text(),10000);
  if(!Set.of("TICKET","COMPLAINT","REFUND_REVIEW").contains(command.kind()))fail(422,"INVALID_KIND");
  var store=dependency(()->stores.requireStore(command.storeId()));if(!command.storeId().equals(store.storeId()))fail(503,"STORE_MISMATCH");
  var candidate=new Ticket(UUID.randomUUID().toString(),c.actorId(),store.merchantId(),store.storeId(),command.kind(),"OPEN",null,0);permit(c,"open",candidate);
  String hash=digest("OPEN",command.storeId(),command.kind(),command.text(),command.refundReference());
  String old=receipt(c,hash);if(old!=null){var t=load(old,true);access(c,t,"read",true);return t;}
  if(command.refundReference()!=null){
   var f=dependency(()->trade.refundStatus(command.refundReference()));
   if(!c.actorId().equals(f.ownerId())||!store.storeId().equals(f.storeId())||!command.refundReference().equals(f.reference()))fail(404,"REFUND_NOT_FOUND");
   if(!Set.of("REJECTED","OVERDUE","MANUAL_REVIEW").contains(f.status()))fail(422,"REFUND_FACT_NOT_ELIGIBLE");
  } else if(command.kind().equals("REFUND_REVIEW"))fail(422,"REFUND_REFERENCE_REQUIRED");
  db.update("insert into sup_ticket values(?,?,?,?,?,'OPEN',NULL,0,?)",candidate.id(),candidate.ownerId(),candidate.merchantId(),candidate.storeId(),candidate.kind(),command.refundReference());
  audit(c,candidate,"OPEN",command.text());record(c,hash,candidate.id());beforeCommit();return candidate;
 });}
 public Ticket get(Context c,String id){return tx.execute(s->{context(c);var t=load(id,false);access(c,t,"read",true);return t;});}
 public Ticket change(Context c,Change command){return write(()->{
  context(c);valid(command.text(),10000);var t=load(command.ticketId(),true);valid(command.action(),32);
  String action=command.action();
  if(!Set.of("ASSIGN","REPLY","ESCALATE","DISPOSE","APPEAL","PROPOSE","EVIDENCE").contains(action))fail(422,"INVALID_ACTION");
  access(c,t,action.toLowerCase(Locale.ROOT),!Set.of("ASSIGN","DISPOSE").contains(action));
  if(action.equals("APPEAL")&&!c.actorId().equals(t.ownerId()))fail(403,"OWNER_REQUIRED");
  String hash=digest("CHANGE",command.ticketId(),command.expectedVersion(),action,command.text(),command.assigneeId());
  if(receipt(c,hash)!=null)return t;
  if(t.version()!=command.expectedVersion())fail(409,"VERSION_CONFLICT");
  String assignee=t.assigneeId();if(action.equals("ASSIGN")){valid(command.assigneeId(),100);dependency(()->{accounts.requireActive(command.assigneeId());return true;});assignee=command.assigneeId();}
  String state;try {state=TicketState.valueOf(t.state()).next(action).name();}catch(IllegalStateException e){throw new SupportFailure(409,"INVALID_STATE");}
  var next=new Ticket(t.id(),t.ownerId(),t.merchantId(),t.storeId(),t.kind(),state,assignee,t.version()+1);
  db.update("update sup_ticket set state=?,assignee_id=?,version=? where id=? and version=?",state,assignee,next.version(),t.id(),t.version());
  audit(c,next,action,command.text());record(c,hash,t.id());beforeCommit();return next;
 });}
 public List<Entry> history(Context c,String id,long after,int limit){return tx.execute(s->{context(c);if(after<0||limit<1||limit>100)fail(422,"INVALID_PAGE");var t=load(id,false);access(c,t,"read",true);return db.query("select * from sup_entry where ticket_id=? and sequence>? order by sequence limit ?",(r,n)->new Entry(r.getLong("sequence"),r.getString("action"),r.getString("actor_id"),r.getString("text"),r.getString("request_id")),id,after,limit);});}
 public Attachment attach(Context c,String ticketId,long version,String mediaType,byte[] bytes){return write(()->{
  context(c);var t=load(ticketId,true);access(c,t,"attachment.write",true);
  com.gym.modules.supportcore.domain.PrivateImage.validate(mediaType,bytes);
  String hash=digest("ATTACH",ticketId,version,mediaType,bytes);var old=receipt(c,hash);if(old!=null)return new Attachment(old,ticketId,mediaType,bytes.length);
  if(t.version()!=version)fail(409,"VERSION_CONFLICT");String id=UUID.randomUUID().toString();
  db.update("insert into sup_attachment values(?,?,?,?)",id,ticketId,mediaType,bytes);
  db.update("update sup_ticket set version=version+1 where id=?",ticketId);
  var next=load(ticketId,true);audit(c,next,"EVIDENCE","Private image attached");record(c,hash,id);beforeCommit();return new Attachment(id,ticketId,mediaType,bytes.length);
 });}
 public byte[] download(Context c,String attachmentId){return tx.execute(s->{context(c);valid(attachmentId,36);var r=db.queryForList("select ticket_id from sup_attachment where id=?",attachmentId);if(r.isEmpty())fail(404,"NOT_FOUND");var t=load((String)r.getFirst().get("ticket_id"),false);access(c,t,"attachment.read",true);return db.queryForObject("select content from sup_attachment where id=?",byte[].class,attachmentId);});}
}

