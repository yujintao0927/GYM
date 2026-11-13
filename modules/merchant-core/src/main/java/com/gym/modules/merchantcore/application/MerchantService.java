package com.gym.modules.merchantcore.application;
import com.gym.modules.merchantcore.api.*;
import com.gym.modules.merchantcore.api.MerchantApi.*;
import com.gym.modules.merchantcore.domain.*;
import com.gym.modules.merchantcore.domain.EmployeeGrant.Role;
import com.gym.modules.merchantcore.ports.MerchantPorts.*;
import com.gym.modules.merchantcore.persistence.MerchantRepository;
import java.time.*; import java.util.*; import java.util.function.*; import java.nio.charset.StandardCharsets; import java.security.*; import javax.sql.DataSource;
/** Local-transaction use cases. Port implementations must join this transaction and never perform network I/O under its lock. */
public final class MerchantService implements StoreDirectory {
 private final MerchantRepository repo; private final AuthorizationPort auth; private final AccountDirectory accounts;
 private final boolean allowFixturePolicies; private final GrantPolicy policy; private final PlatformPermissionSource platform; private final Clock clock;
 public MerchantService(DataSource ds,AuthorizationPort a,AccountDirectory b,GrantPolicy g,PlatformPermissionSource p,Clock c){this(ds,a,b,g,p,c,false);}
 public MerchantService(DataSource ds,AuthorizationPort a,AccountDirectory b,GrantPolicy g,PlatformPermissionSource p,Clock c,boolean allowFixturePolicies){this.allowFixturePolicies=allowFixturePolicies;
  repo=new MerchantRepository(Objects.requireNonNull(ds));auth=Objects.requireNonNull(a);accounts=Objects.requireNonNull(b);policy=Objects.requireNonNull(g);platform=Objects.requireNonNull(p);clock=Objects.requireNonNull(c);
 }
 private static String text(String s,int limit){if(s==null||s.isBlank()||s.length()>limit)throw new MerchantFailure(422,"INVALID_INPUT");return s;}
 private void identity(Context c){if(c==null)throw new MerchantFailure(401,"SUBJECT_INVALID");text(c.actorId(),64);text(c.requestId(),64);text(c.key(),100);active(c.actorId());}
 private void active(String actor){
  ActiveAccount account;try{account=accounts.requireActive(actor);}catch(MerchantFailure e){throw new MerchantFailure(e.status()==401?401:503,e.status()==401?"SUBJECT_INVALID":"ACCOUNT_UNAVAILABLE");}catch(RuntimeException e){throw new MerchantFailure(503,"ACCOUNT_UNAVAILABLE");}
  if(account==null||!actor.equals(account.accountId()))throw new MerchantFailure(401,"SUBJECT_INVALID");
 }
 private static void allowed(Supplier<Decision> check){Decision d;try{d=check.get();}catch(RuntimeException e){throw new MerchantFailure(503,"AUTHORIZATION_UNAVAILABLE");}if(d==Decision.UNAVAILABLE||d==null)throw new MerchantFailure(503,"AUTHORIZATION_UNAVAILABLE");if(d!=Decision.ALLOW)throw new MerchantFailure(403,"ACTION_DENIED");}
 private void permit(Context c,String action,String merchant,String store,String owner){allowed(()->auth.authorize(c.actorId(),action,merchant,store,owner));}
 private void platform(Context c,String action){allowed(()->platform.authorize(c.actorId(),action));}
 private static void version(long actual,long expected){if(actual!=expected)throw new MerchantFailure(409,"VERSION_CONFLICT");}
 private static String id(){return UUID.randomUUID().toString();}
 private static String hash(Object... parts){try{var digest=MessageDigest.getInstance("SHA-256");for(Object p:parts){byte[] bytes=String.valueOf(p).getBytes(StandardCharsets.UTF_8);digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());digest.update(bytes);}return HexFormat.of().formatHex(digest.digest());}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private Result write(Context c,String action,String fingerprint,Runnable guard,Supplier<Result> change,Function<Result,String> merchant,Function<Result,String> store){return write(c,action,fingerprint,guard,change,merchant,store,()->"{\"changed\":true}");}
 private Result write(Context c,String action,String fingerprint,Runnable guard,Supplier<Result> change,Function<Result,String> merchant,Function<Result,String> store,Supplier<String> eventPayload){
  return repo.transaction(()->{
   identity(c);guard.run();var receipt=repo.rows("SELECT * FROM mch_receipt WHERE actor=? AND operation_key=?",c.actorId(),c.key());
   String fp=hash(action,fingerprint);
   if(!receipt.isEmpty()){var r=receipt.getFirst();if(!fp.equals(r.get("fingerprint")))throw new MerchantFailure(409,"IDEMPOTENCY_MISMATCH");return new Result((String)r.get("result_id"),MerchantRepository.n(r,"result_version"));}
   Result result=change.get();String now=clock.instant().toString();
   repo.update("INSERT INTO mch_audit VALUES(?,?,?,?,?,?)",id(),c.actorId(),action,result.id(),c.requestId(),now);
   repo.update("INSERT INTO mch_outbox VALUES(?,?,1,'merchant-core',?,?,?,?,?,'SIMULATED',?)",id(),action,result.id(),result.version(),now,merchant.apply(result),store.apply(result),eventPayload.get());
   repo.update("INSERT INTO mch_receipt VALUES(?,?,?,?,?)",c.actorId(),c.key(),fp,result.id(),result.version());return result;
  });
 }
 public Result submitMerchant(Context c,String name,String evidence){text(name,160);text(evidence,160);
  return write(c,"merchant.submit",hash(name,evidence),()->permit(c,"merchant.submit",null,null,c.actorId()),()->{
   String id=id();repo.update("INSERT INTO mch_merchant VALUES(?,?,?,?,'PENDING',0)",id,c.actorId(),name,evidence);return new Result(id,0);
  },Result::id,r->null);
 }
 public Result reviewMerchant(Context c,String merchant,long expected,boolean approve){
  return write(c,"merchant.review",hash(merchant,expected,approve),()->{repo.merchant(merchant);platform(c,"merchant.review");permit(c,"merchant.review",null,null,null);},()->{
   var m=repo.merchant(merchant);version(m.version(),expected);if(!m.state().equals("PENDING"))throw new MerchantFailure(409,"REVIEW_ALREADY_FINAL");
   repo.update("UPDATE mch_merchant SET review_state=?,version=version+1 WHERE id=?",approve?"APPROVED":"REJECTED",merchant);return new Result(merchant,expected+1);
  },Result::id,r->null);
 }
 public Merchant merchant(Context c,String id){return repo.transaction(()->{identity(c);var m=repo.merchant(id);permit(c,"merchant.read",m.id(),null,m.applicant());return m;});}
 public Result createStore(Context c,String merchant,String name){text(name,160);
  return write(c,"store.create",hash(merchant,name),()->{var m=repo.merchant(merchant);permit(c,"store.create",m.id(),null,m.applicant());if(!m.state().equals("APPROVED"))throw new MerchantFailure(422,"MERCHANT_NOT_APPROVED");},()->{
   String id=id();repo.update("INSERT INTO mch_store(id,merchant_id,name,version) VALUES(?,?,?,0)",id,merchant,name);return new Result(id,0);
  },r->merchant,Result::id);
 }
 public Result updateStore(Context c,String id,long expected,String name){text(name,160);
  return write(c,"store.update",hash(id,expected,name),()->storePermit(c,id,"store.update"),()->{var s=repo.store(id);version(s.version(),expected);repo.update("UPDATE mch_store SET name=?,version=version+1 WHERE id=?",name,id);return new Result(id,expected+1);},r->repo.store(id).merchantId(),Result::id);
 }
 public Result deleteStore(Context c,String id,long expected){
  String[] merchant={null};return write(c,"store.delete",hash(id,expected),()->{var target=repo.storeIncludingDeleted(id);merchant[0]=target.merchantId();permit(c,"store.delete",target.merchantId(),id,repo.merchant(target.merchantId()).applicant());},()->{
   var s=repo.store(id);version(s.version(),expected);repo.update("UPDATE mch_store SET deleted=TRUE,version=version+1 WHERE id=?",id);return new Result(id,expected+1);
  },r->merchant[0],Result::id);
 }
 private Store storePermit(Context c,String id,String action){var s=repo.store(id);permit(c,action,s.merchantId(),s.id(),repo.merchant(s.merchantId()).applicant());return s;}
 public Store store(Context c,String id){return repo.transaction(()->{identity(c);return storePermit(c,id,"store.read");});}
 public Result setTerm(Context c,String id,long expected,Instant end,Instant grace){ServiceTerm term;try{term=new ServiceTerm(end,grace);}catch(IllegalArgumentException e){throw new MerchantFailure(422,"INVALID_SERVICE_ENDPOINTS");}
  return write(c,"store.term",hash(id,expected,end,grace),()->{storePermit(c,id,"store.term");platform(c,"store.term");},()->{
   var s=repo.store(id);version(s.version(),expected);repo.update("UPDATE mch_store SET end_at=?,grace_end=?,version=version+1 WHERE id=?",term.end().toString(),term.graceEnd().toString(),id);return new Result(id,expected+1);
  },r->repo.store(id).merchantId(),Result::id);
 }
 public Result grant(Context c,String merchant,String account,Role role,Set<String> stores){
  EmployeeGrant valid;try{valid=new EmployeeGrant(account,merchant,role,stores,false,0);}catch(IllegalArgumentException|NullPointerException e){throw new MerchantFailure(422,"INVALID_GRANT");} PolicyMetadata[] grantedPolicy={null};var proposed=new Grant(id(),merchant,account,role,valid.stores(),false,0);
  return write(c,"staff.grant",hash(merchant,account,role,new TreeSet<>(valid.stores())),()->{
   var m=repo.merchant(merchant);permit(c,"staff.grant",merchant,null,m.applicant());platform(c,"staff.grant");active(account);
   grantedPolicy[0]=approvedPolicy();
   for(String store:valid.stores())if(!repo.store(store).merchantId().equals(merchant))throw new MerchantFailure(404,"RESOURCE_NOT_FOUND");
   allowed(()->policy.mayGrant(c.actorId(),proposed));
  },()->{
   repo.update("INSERT INTO mch_grant VALUES(?,?,?,?,FALSE,0)",proposed.id(),merchant,account,role.name());for(String store:valid.stores())repo.update("INSERT INTO mch_grant_store VALUES(?,?)",proposed.id(),store);return new Result(proposed.id(),0);
  },r->merchant,r->null,()->"{\"policyId\":\""+grantedPolicy[0].id()+"\",\"policyVersion\":"+grantedPolicy[0].version()+",\"policyStatus\":\""+grantedPolicy[0].status()+"\"}");
 }
 public Result revoke(Context c,String id,long expected){
  return write(c,"staff.revoke",hash(id,expected),()->{var g=repo.grant(id);permit(c,"staff.revoke",g.merchantId(),null,g.accountId());platform(c,"staff.revoke");},()->{
   var g=repo.grant(id);version(g.version(),expected);if(g.revoked())throw new MerchantFailure(409,"ALREADY_REVOKED");repo.update("UPDATE mch_grant SET revoked=TRUE,version=version+1 WHERE id=?",id);return new Result(id,expected+1);
  },r->repo.grant(id).merchantId(),r->null);
 }
 /** Trusted IAM provider read: this returns staff scope only and never paid membership or platform authority. */
 public Decision currentStaffDecision(String account,String merchant,String store,Role role){
  try{return repo.transaction(()->{active(account);var s=repo.store(store);if(!s.merchantId().equals(merchant))return Decision.DENY;
   for(var r:repo.rows("SELECT id FROM mch_grant WHERE account_id=? AND merchant_id=? AND role_name=? AND revoked=FALSE",account,merchant,role.name())){
    if(repo.grant((String)r.get("id")).stores().contains(store))return Decision.ALLOW;
   }return Decision.DENY;
  });}catch(MerchantFailure e){return e.status()==503?Decision.UNAVAILABLE:Decision.DENY;}
 }
 public StorePage stores(Context c,String merchant,int limit,String cursor){
  if(limit<1||limit>100)throw new MerchantFailure(422,"INVALID_PAGE_SIZE");
  return repo.transaction(()->{
   identity(c);var m=repo.merchant(merchant);permit(c,"store.list",merchant,null,m.applicant());String after="";String binding=hash(c.actorId(),merchant,"stores.active");
   if(cursor!=null){try{String decoded=new String(Base64.getUrlDecoder().decode(cursor),StandardCharsets.UTF_8);String[] fields=decoded.split(":",-1);if(fields.length!=2||!fields[0].equals(binding)||fields[1].length()>64)throw new IllegalArgumentException();after=fields[1];}catch(IllegalArgumentException e){throw new MerchantFailure(422,"INVALID_CURSOR");}}
   var rows=repo.rows("SELECT id FROM mch_store WHERE merchant_id=? AND deleted=FALSE AND id>? ORDER BY id LIMIT ?",merchant,after,limit+1);
   var result=new ArrayList<Store>();for(int i=0;i<Math.min(limit,rows.size());i++){String id=(String)rows.get(i).get("id");result.add(storePermit(c,id,"store.read"));}
   String next=rows.size()>limit?Base64.getUrlEncoder().withoutPadding().encodeToString((binding+":"+result.getLast().id()).getBytes(StandardCharsets.UTF_8)):null;
   return new StorePage(result,next);
  });
 } public StoreSnapshot requireStore(String id){return repo.transaction(()->{var s=repo.store(id);String status=s.end()==null?"PENDING_POLICY":new ServiceTerm(s.end(),s.graceEnd()).at(clock.instant()).name();return new StoreSnapshot(s.id(),s.merchantId(),status,s.version());});}
 public Grant employeeGrant(Context c,String id){return repo.transaction(()->{identity(c);var g=repo.grant(id);permit(c,"staff.read",g.merchantId(),null,g.accountId());return g;});} private PolicyMetadata approvedPolicy(){
  PolicyMetadata m;try{m=policy.metadata();}catch(RuntimeException e){throw new MerchantFailure(503,"POLICY_UNAVAILABLE");}
  if(m==null||m.status()==null||m.status()==PolicyStatus.UNAPPROVED||(m.status()==PolicyStatus.CONFIGURED_FIXTURE&&!allowFixturePolicies))throw new MerchantFailure(422,"POLICY_UNAPPROVED");
  if(m.id()==null||!m.id().matches("[A-Za-z0-9._-]{1,64}")||m.version()<1)throw new MerchantFailure(422,"POLICY_UNAPPROVED");return m;
 }}





