package com.gym.modules.bookingcore.application;
import com.gym.modules.bookingcore.api.*;
import com.gym.modules.bookingcore.api.BookingApi.*;
import com.gym.modules.bookingcore.ports.BookingPorts;
import com.gym.modules.bookingcore.domain.BookingRules;
import javax.sql.DataSource;
import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
/** Library application boundary; no HTTP or authentication bypass. */
public final class BookingService {
 private final JdbcTemplate db; private final TransactionTemplate tx; private final BookingPorts ports;
 public BookingService(DataSource source,BookingPorts ports){this.db=new JdbcTemplate(source);this.tx=new TransactionTemplate(new DataSourceTransactionManager(source));this.ports=Objects.requireNonNull(ports);
  try(var c=source.getConnection()){com.gym.modules.bookingcore.persistence.BookingDatabase.validateUrl(c.getMetaData().getURL());}catch(java.sql.SQLException e){throw new BookingFailure(503,"DATABASE_UNAVAILABLE");}}
 private <T>T transaction(Supplier<T> work){
  try{return tx.execute(s->work.get());}catch(BookingFailure e){throw e;}
  catch(org.springframework.dao.DuplicateKeyException e){throw error(409,"CONCURRENT_CONFLICT");}
  catch(IllegalArgumentException e){throw error(422,"INVALID_INPUT");}
  catch(RuntimeException e){throw error(503,"DEPENDENCY_UNAVAILABLE");}
 }
 private BookingFailure error(int status,String code){return new BookingFailure(status,code);}
 private void context(Context c){
  if(c==null||c.actorId()==null||c.requestId()==null||c.key()==null||c.key().isBlank()||c.key().length()>64)throw error(422,"INVALID_CONTEXT");
  if(!"SIMULATED".equals(c.mode()))throw error(422,"MODE_UNAPPROVED");
  ports.requireActive(c.actorId());
 }
 private BookingPorts.Store authorize(Context c,String action,String store,String owner){
  context(c);var scope=ports.requireStore(store);
  if(scope==null||!store.equals(scope.storeId()))throw error(503,"STORE_UNAVAILABLE");
  var decision=ports.authorize(c.actorId(),action,scope.merchantId(),store,owner);
  if(decision==BookingPorts.Decision.UNAVAILABLE)throw error(503,"AUTH_UNAVAILABLE");
  if(decision!=BookingPorts.Decision.ALLOW)throw error(403,"ACCESS_DENIED");
  return scope;
 }
 private Map<String,Object> row(String sql,Object...args){var rows=db.queryForList(sql,args);if(rows.isEmpty())throw error(404,"NOT_FOUND");return rows.getFirst();}
 private String str(Map<String,Object> r,String key){return Objects.toString(r.get(key),null);}
 private long num(Map<String,Object> r,String key){return ((Number)r.get(key)).longValue();}
 private String hash(Object body){try{var digest=MessageDigest.getInstance("SHA-256");encode(digest,body);return HexFormat.of().formatHex(digest.digest());}catch(Exception e){throw new IllegalStateException("HASH_UNAVAILABLE");}}
 private void encode(MessageDigest digest,Object value) throws ReflectiveOperationException {
  if(value==null){digest.update((byte)0);return;}digest.update((byte)1);
  if(value.getClass().isRecord()) {
   for(var component:value.getClass().getRecordComponents())encode(digest,component.getAccessor().invoke(value));
  }else{
   byte[] bytes=value.toString().getBytes(StandardCharsets.UTF_8);
   digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());digest.update(bytes);
  }
 }
 private String replay(Context c,String op,Object body){var rows=db.queryForList("SELECT * FROM bkg_receipts WHERE actor_id=? AND operation=? AND idem_key=?",c.actorId(),op,c.key());if(rows.isEmpty())return null;var r=rows.getFirst();if(!hash(body).equals(str(r,"body_hash")))throw error(409,"IDEMPOTENCY_MISMATCH");return str(r,"result_id");}
 private void receipt(Context c,String op,Object body,String result){db.update("INSERT INTO bkg_receipts VALUES(?,?,?,?,?)",c.actorId(),op,c.key(),hash(body),result);}
 private void event(String type,String id,long version,Map<String,Object> session){db.update("INSERT INTO bkg_outbox(event_id,event_type,aggregate_id,aggregate_version,merchant_id,store_id,payload) VALUES(?,?,?,?,?,?,JSON_OBJECT('bookingId',?))",UUID.randomUUID().toString(),type,id,version,str(session,"merchant_id"),str(session,"store_id"),id);}
 public String schedule(Context c,Schedule command){return transaction(()->{context(c);
  var scope=authorize(c,"booking.schedule",command.storeId(),null);
  if(!"ACTIVE".equals(scope.status()))throw error(422,"STORE_UNAVAILABLE");
  var policy=ports.getPolicy(command.policyId(),command.policyVersion());
  if(policy==null)throw error(503,"POLICY_UNAVAILABLE");
  if(policy==BookingPorts.PolicyStatus.UNAPPROVED)throw error(422,"POLICY_UNAPPROVED");
  var coach=ports.requireCoach(command.coachId(),command.storeId());
  if(coach==null||!command.coachId().equals(coach.accountId())||!command.storeId().equals(coach.storeId())||!scope.merchantId().equals(coach.merchantId())||!"ACTIVE".equals(coach.status()))throw error(404,"COACH_NOT_FOUND");
  ports.requireActive(coach.accountId());
  BookingRules.overlaps(command.start(),command.end(),command.start(),command.end());
  if(command.capacity()<1||!("GROUP".equals(command.kind())||"PRIVATE".equals(command.kind()))||("PRIVATE".equals(command.kind())&&command.capacity()!=1))throw error(422,"INVALID_SESSION");
  db.update("INSERT IGNORE INTO bkg_coach_locks(coach_id) VALUES(?)",command.coachId());
  row("SELECT * FROM bkg_coach_locks WHERE coach_id=? FOR UPDATE",command.coachId());
  String old=replay(c,"schedule",command);if(old!=null)return old;
  if(db.queryForObject("SELECT COUNT(*) FROM bkg_sessions WHERE coach_id=? AND starts_at<? AND ends_at>?",Long.class,command.coachId(),Timestamp.from(command.end()),Timestamp.from(command.start()))>0)throw error(409,"COACH_OVERLAP");
  String id=UUID.randomUUID().toString();
  db.update("INSERT INTO bkg_sessions(id,merchant_id,store_id,coach_id,kind,starts_at,ends_at,capacity,policy_id,policy_version) VALUES(?,?,?,?,?,?,?,?,?,?)",id,scope.merchantId(),scope.storeId(),command.coachId(),command.kind(),Timestamp.from(command.start()),Timestamp.from(command.end()),command.capacity(),command.policyId(),command.policyVersion());
  event("SessionScheduled",id,0,row("SELECT * FROM bkg_sessions WHERE id=?",id));
  receipt(c,"schedule",command,id);return id;
 });}
 public String book(Context c,Book command){return transaction(()->{context(c);
  var s=row("SELECT * FROM bkg_sessions WHERE id=? FOR UPDATE",command.sessionId());
  var scope=authorize(c,"booking.reserve",str(s,"store_id"),c.actorId());
  if(!scope.merchantId().equals(str(s,"merchant_id")))throw error(404,"NOT_FOUND");
  String old=replay(c,"book",command);if(old!=null)return old;
  if(db.queryForObject("SELECT COUNT(*) FROM bkg_bookings WHERE session_id=? AND owner_id=? AND state IN ('BOOKED','FULFILLED')",Long.class,command.sessionId(),c.actorId())>0)throw error(409,"ALREADY_BOOKED");
  if(num(s,"reserved")>=num(s,"capacity"))throw error(409,"CAPACITY_FULL");
  if(!"ACTIVE".equals(scope.status()))throw error(422,"STORE_UNAVAILABLE");
  var policy=ports.getPolicy(str(s,"policy_id"),num(s,"policy_version"));
  if(policy==null)throw error(503,"POLICY_UNAVAILABLE");
  if(policy==BookingPorts.PolicyStatus.UNAPPROVED)throw error(422,"POLICY_UNAPPROVED");
  var purchase=ports.requirePurchase(command.orderId(),c.actorId(),scope.storeId(),command.sessionId(),str(s,"coach_id"),str(s,"kind"),str(s,"policy_id"),num(s,"policy_version"));
  if(purchase==null||!command.orderId().equals(purchase.orderId())||!c.actorId().equals(purchase.ownerId())||!scope.storeId().equals(purchase.storeId())||!str(s,"policy_id").equals(purchase.policyId())||num(s,"policy_version")!=purchase.policyVersion())throw error(404,"NOT_FOUND");
  if(!Set.of("COUNT","TERM").contains(purchase.kind()))throw error(422,"POLICY_UNAPPROVED");
  var rights=ports.resolve(c.actorId(),scope.storeId(),"COURSE_BOOKING");
  if(rights==null)throw error(503,"RIGHTS_UNAVAILABLE");
  if(rights.stream().noneMatch(r->r.sourceId().equals(command.sourceId())&&r.version()==command.expectedRightsVersion()&&r.orderId().equals(command.orderId())&&r.productId().equals(purchase.productId())&&(r.coachId()==null?!"PRIVATE".equals(str(s,"kind")):r.coachId().equals(str(s,"coach_id")))))throw error(403,"RIGHTS_DENIED");
  String id=UUID.randomUUID().toString();var ref=ports.reserve(command.sourceId(),1,command.expectedRightsVersion(),id);
  if(ref==null||ref.reference()==null||ref.version()<0)throw error(503,"RIGHTS_UNAVAILABLE");
  db.update("INSERT INTO bkg_bookings(id,session_id,owner_id,state,source_id,rights_version,order_id,product_kind,policy_id,policy_version,disclosed,accepted,cancellation_minor,reservation_ref) VALUES(?,?,?,'BOOKED',?,?,?,?,?,?,?,?,?,?)",id,command.sessionId(),c.actorId(),command.sourceId(),ref.version(),command.orderId(),purchase.kind(),purchase.policyId(),purchase.policyVersion(),purchase.disclosedBeforePayment(),purchase.accepted(),BookingRules.cancellationCharge(purchase.cancellationMinor(),purchase.disclosedBeforePayment(),purchase.accepted()),ref.reference());
  db.update("UPDATE bkg_sessions SET reserved=reserved+1,version=version+1 WHERE id=?",command.sessionId());
  event("BookingReserved",id,0,s);receipt(c,"book",command,id);return id;
 });}
 public String change(Context c,String id,long version,String command){return transaction(()->{context(c);
  // Lock session before booking, same order as reserve. Preliminary read grants no authority.
  var preliminary=row("SELECT session_id FROM bkg_bookings WHERE id=?",id);
  var s=row("SELECT * FROM bkg_sessions WHERE id=? FOR UPDATE",str(preliminary,"session_id"));
  var b=row("SELECT * FROM bkg_bookings WHERE id=? FOR UPDATE",id);
  var scope=authorize(c,"booking."+command,str(s,"store_id"),str(b,"owner_id"));
  if(!scope.merchantId().equals(str(s,"merchant_id")))throw error(404,"NOT_FOUND");
  String body=id+":"+version+":"+command;String old=replay(c,"change",body);if(old!=null)return old;
  if(num(b,"version")!=version)throw error(409,"VERSION_CONFLICT");
  String next;try{next=BookingRules.transition(str(b,"state"),command);}catch(IllegalStateException e){throw error(409,"BOOKING_TERMINAL");}
  // Only a transaction-bound local Trade adapter may create the fee order.
  String charge=null;
  if(command.equals("CANCEL")&&num(b,"cancellation_minor")>0) {
   charge=ports.createOrder(new BookingPorts.CancellationOrder(id,str(b,"order_id"),str(b,"owner_id"),str(b,"policy_id"),num(b,"policy_version"),num(b,"cancellation_minor"),"SIMULATED",id+":cancellation"));
   if(charge==null)throw error(503,"TRADE_UNAVAILABLE");
  }
  var currentRights=ports.resolve(str(b,"owner_id"),str(s,"store_id"),"COURSE_RESERVATION_MUTATION");
  if(currentRights==null)throw error(503,"RIGHTS_UNAVAILABLE");
  var current=currentRights.stream().filter(r->r.sourceId().equals(str(b,"source_id"))&&r.orderId().equals(str(b,"order_id"))).findFirst().orElseThrow(()->error(403,"RIGHTS_DENIED"));
  var ref=command.equals("CANCEL")?ports.release(str(b,"reservation_ref"),1,current.version(),id+":release"):ports.consume(str(b,"reservation_ref"),1,current.version(),id+":fulfil");
  if(ref==null||ref.reference()==null||ref.version()<0)throw error(503,"RIGHTS_UNAVAILABLE");
  db.update("UPDATE bkg_bookings SET state=?,version=version+1,consumption_ref=?,charge_ref=?,rights_version=? WHERE id=?",next,ref.reference(),charge,ref.version(),id);
  if(command.equals("CANCEL"))db.update("UPDATE bkg_sessions SET reserved=reserved-1,version=version+1 WHERE id=?",str(b,"session_id"));
  if(charge!=null)db.update("UPDATE bkg_bookings SET reconciliation='FEE_ORDER_CREATED' WHERE id=?",id);
  event("Booking"+next,id,version+1,s);receipt(c,"change",body,id);return id;
 });}
 public View find(Context c,String id){return transaction(()->{context(c);
  var b=row("SELECT * FROM bkg_bookings WHERE id=?",id);var s=row("SELECT * FROM bkg_sessions WHERE id=?",str(b,"session_id"));
  var scope=authorize(c,"booking.read",str(s,"store_id"),str(b,"owner_id"));
  if(!scope.merchantId().equals(str(s,"merchant_id")))throw error(404,"NOT_FOUND");
  return new View(id,str(b,"session_id"),str(b,"owner_id"),str(b,"state"),num(b,"version"),str(b,"reconciliation"));
 });}
 public SessionPage listSessions(Context c,String store,String cursor,int limit){return transaction(()->{context(c);
  var scope=authorize(c,"booking.list",store,c.actorId());
  if(limit<1||limit>100)throw error(422,"INVALID_PAGE");
  String after="";
  if(cursor!=null){
   String[] parts=new String(Base64.getUrlDecoder().decode(cursor),StandardCharsets.UTF_8).split("[|]",-1);
   if(parts.length!=4||!parts[0].equals("sessions")||!parts[1].equals(c.actorId())||!parts[2].equals(store))throw error(422,"CURSOR_SCOPE");
   after=parts[3];
  }
  var rows=db.queryForList("SELECT * FROM bkg_sessions WHERE store_id=? AND merchant_id=? AND id>? ORDER BY id LIMIT ?",store,scope.merchantId(),after,limit+1);
  boolean more=rows.size()>limit;
  var items=rows.stream().limit(limit).map(r->new SessionView(str(r,"id"),str(r,"kind"),(int)num(r,"capacity"),(int)(num(r,"capacity")-num(r,"reserved")),num(r,"version"),str(r,"coach_id"),((Timestamp)r.get("starts_at")).toInstant(),((Timestamp)r.get("ends_at")).toInstant(),str(r,"policy_id"),num(r,"policy_version"))).toList();
  String next=more?Base64.getUrlEncoder().withoutPadding().encodeToString(("sessions|"+c.actorId()+"|"+store+"|"+items.getLast().id()).getBytes(StandardCharsets.UTF_8)):null;
  return new SessionPage(items,next);
 });}}
