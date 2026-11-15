"""Bounded external regression. Secrets stay in memory; evidence is allowlisted."""
import concurrent.futures as cf
import copy, hashlib, json, os, pathlib, re, secrets, socket, subprocess, sys, threading, time, urllib.request, urllib.error, zipfile
HERE=pathlib.Path(__file__).resolve().parent
ROOT=HERE.parents[1]
SCHEMA='gym_dev_test_003b'
URL='jdbc:mysql://localhost:3306/'+SCHEMA
class Harness:
 def __init__(self):
  self.rows=[]; self.secrets=[]; self.proc=None; self.accounts=[]
  diff=subprocess.run(['git','diff','e20e499','--','backend'],cwd=ROOT,capture_output=True,check=True)
  if diff.stdout:raise ValueError('backend differs from pinned source')
  self.env=os.environ.copy()
  if self.env.get('GYM_DB_URL',URL)!=URL: raise ValueError('target rejected')
  if not self.env.get('GYM_DB_PASSWORD'): raise ValueError('private configuration missing')
  self.env.update(GYM_DB_URL=URL,GYM_DB_SCHEMA=SCHEMA,GYM_DB_USERNAME=self.env.get('GYM_DB_USERNAME','root'),GYM_IAM_HMAC_KEY=secrets.token_hex(32),SPRING_PROFILES_ACTIVE='local-dev')
  self.secrets += [self.env['GYM_DB_PASSWORD'],self.env['GYM_IAM_HMAC_KEY']]
  self.java=str(pathlib.Path(self.env.get('JAVA_HOME','D:/java_environment'))/'bin/java.exe')
  self.jar=ROOT/'backend/target/gym-0.0.1.jar'
  self.private=HERE/'.private'; self.private.mkdir(exist_ok=True)
  with zipfile.ZipFile(self.jar) as z:
   name=next(n for n in z.namelist() if 'mysql-connector-j-' in n and n.endswith('.jar'))
   self.driver=self.private/'jdbc.jar'; self.driver.write_bytes(z.read(name))
  with socket.socket() as s: s.bind(('127.0.0.1',0)); self.port=s.getsockname()[1]
  self.base=f'http://127.0.0.1:{self.port}'
  self.prefix='n2_'+secrets.token_hex(8)
 def stop(self):
  if self.proc:
   self.proc.terminate()
   try:self.proc.wait(timeout=15)
   except subprocess.TimeoutExpired:self.proc.kill();self.proc.wait()
   self.proc=None
 def start(self,default=False):
  env=self.env.copy(); env['SPRING_PROFILES_ACTIVE']='default' if default else 'local-dev'
  # Suppress raw process output, including connection exceptions. Retain only warning presence.
  self.log=open(self.private/'process.log','ab')
  self.proc=subprocess.Popen([self.java,'-jar',str(self.jar),f'--server.port={self.port}','--server.address=127.0.0.1','--gym.db.schema='+SCHEMA],env=env,stdout=self.log,stderr=subprocess.STDOUT)
  for _ in range(120):
   if self.proc.poll() is not None:raise RuntimeError('service startup failed (private log)')
   try:
    if self.http('GET','/actuator/health')[0]==200:return
   except OSError:pass
   time.sleep(.25)
  raise RuntimeError('startup timeout')
 def http(self,method,path,body=None,headers=None):
  h={'Content-Type':'application/json'};h.update(headers or {})
  if isinstance(body,dict) and isinstance(body.get('credential'),dict):
   proof=body['credential'].get('proof')
   if isinstance(proof,str) and proof:self.secrets.append(proof)
  data=body.encode() if isinstance(body,str) else json.dumps(body).encode() if body is not None else None
  req=urllib.request.Request(self.base+path,data=data,headers=h,method=method)
  try:r=urllib.request.urlopen(req,timeout=20)
  except urllib.error.HTTPError as e:r=e
  raw=r.read(); parsed=json.loads(raw) if raw else {}
  if isinstance(parsed,dict):
   for k in ('sessionToken','refreshToken'):
    v=parsed.get('data',{}).get(k)
    if v:self.secrets.append(v)
  return r.code,parsed,dict(r.headers)
 def check(self,name,call,status,code=None):
  try:
   result=call(); actual=result[0]; good=actual==status and (code is None or result[1].get('code')==code)
   if status>=400:
    good=good and all(k in result[1] for k in ('code','message','requestId','retryable','details'))
    raw=json.dumps(result[1],ensure_ascii=False);good=good and not any(s in raw for s in self.secrets)
   else:good=good and result[2].get('Cache-Control','').find('no-store')>=0
   self.rows.append({'case':name,'expected':status,'actual':actual,'actualCode':result[1].get('code'),'requestId':result[1].get('requestId'),'pass':good});return result
  except Exception:
   self.rows.append({'case':name,'expected':status,'actual':'exception','pass':False});return (0,{}, {})
 def account(self,password=None):
  name=f'{self.prefix}_{len(self.accounts)}'; password=password or secrets.token_urlsafe(24);self.secrets.append(password)
  p=subprocess.run([self.java,'-Dloader.main=com.gym.identity.local.LocalAccountCli','-cp',str(self.jar),'org.springframework.boot.loader.launch.PropertiesLauncher',name,'--password-stdin'],input=password+'\n',text=True,encoding="utf-8",capture_output=True,env=self.env,timeout=60)
  if p.returncode:raise RuntimeError('CLI failed')
  self.accounts.append(name);return name,password
 def body(self,a):return {'credential':{'kind':'LOCAL_PASSWORD','login':a[0],'proof':a[1]},'entry':'user-web','clientKind':'WEB'}
 def login(self,a,key=None,body=None):return self.http('POST','/v1/sessions',body or self.body(a),{'Idempotency-Key':key or secrets.token_hex(12)})
 def session(self,a):
  r=self.login(a)
  if r[0]!=201:raise RuntimeError('fixture login failed')
  return r[1]['data']
 def ctx(self,s,headers=None):return self.http('GET','/v1/me/contexts',headers={'Authorization':'Bearer '+s['sessionToken'],**(headers or {})})
 def refresh(self,s,key=None,generation=None):return self.http('POST','/v1/sessions/refresh',{'refreshToken':s['refreshToken'],'generation':s['generation'] if generation is None else generation},{'Idempotency-Key':key or secrets.token_hex(12)})
 def logout(self,s,target=None):return self.http('DELETE','/v1/sessions/'+(target or s['sessionId']),headers={'Authorization':'Bearer '+s['sessionToken']})
 def db(self,op,a):
  p=subprocess.run([self.java,'-cp',str(self.driver),str(HERE/'Fixture.java')],input=op+'\n'+a[0]+'\n',env=self.env,text=True,capture_output=True,timeout=30)
  if p.returncode:raise RuntimeError('fixture rejected')
  return int(p.stdout.strip())
 def parallel(self,calls):
  barrier=threading.Barrier(len(calls))
  def run(f):barrier.wait(timeout=10);return f()
  with cf.ThreadPoolExecutor(max_workers=len(calls)) as pool:return list(pool.map(run,calls))
 def invariant(self,name,ok):self.rows.append({'case':name,'pass':bool(ok)})
 def run(self):
  self.start();a=self.account();b=self.account()
  s=self.check('login WEB generation0',lambda:self.login(a),201)[1]['data'];self.invariant('generation0',s['generation']==0)
  self.check('contexts empty grants',lambda:self.ctx(s),200); c=self.ctx(s)[1]['data'];self.invariant('no grants',c['staffScopes']==[] and c['entitlementSources']==[])
  body=self.body(b);body.update(entry='user-mini',clientKind='MINI_PROGRAM');self.check('login MINI_PROGRAM',lambda:self.login(b,body=body),201)
  variants=[]
  for field in ('credential','entry','clientKind'):
   v=self.body(a);del v[field];variants.append(('missing '+field,v,422))
  for field in ('role','accountId','unknown'):
   v=self.body(a);v[field]='admin';variants.append(('extra '+field,v,422))
  for field,val in [('kind','WECHAT'),('login','中名字'),('login','ab'),('login','a'*65),('proof','x'*11),('proof','x'*73),('proof','中'*25),('proof',12),('proof',None),('login',[]),('kind',False)]:
   v=self.body(a);v['credential'][field]=val;variants.append((f'credential {field} {len(variants)}',v,422))
  for field,val in [('entry','root'),('entry',42),('clientKind','web'),('clientKind',None),('credential',[])]:
   v=self.body(a);v[field]=val;variants.append((f'invalid {field} {len(variants)}',v,422))
  for name,v,status in variants:self.check(name,lambda v=v:self.login(a,body=v),status,'INVALID_INPUT')
  for label,key in [('missing',None),('space','bad key'),('long','k'*129)]:self.check('key '+label,lambda key=key:self.http('POST','/v1/sessions',self.body(a),{} if key is None else {'Idempotency-Key':key}),422)
  for name,raw in [('broken','{'),('array','[]'),('null','null')]:self.check('JSON '+name,lambda raw=raw:self.http('POST','/v1/sessions',raw),422)
  for name,h in [('missing',{}),('lowercase',{'Authorization':'bearer '+'x'*43}),('junk',{'Authorization':'Bearer x'}),('cookie',{'Cookie':'session=fake'})]:self.check('auth '+name,lambda h=h:self.http('GET','/v1/me/contexts',headers=h),422 if name=='cookie' else 401)
  self.check('cookie plus bearer',lambda:self.ctx(s,{'Cookie':'a=b'}),422,'AMBIGUOUS_AUTH')
  for entry in ('merchant','coach','platform-admin','platform-support'):
   v=self.body(a);v['entry']=entry;r=self.check('entry '+entry,lambda v=v:self.login(a,body=v),201)
   if r[0]==201:self.invariant('entry grants '+entry,self.ctx(r[1]['data'])[1]['data']['staffScopes']==[])
  for password,label in [('x'*12,'12 bytes'),('x'*72,'72 bytes'),('中'*24,'72 UTF8 bytes')]:
   x=self.account(password);self.check('valid password '+label,lambda:self.login(x),201)
  for length in (1,128):self.check('valid key length '+str(length),lambda length=length:self.login(a,'k'*length),201)
  t=self.session(a)
  for body,label in [({'refreshToken':t['refreshToken'],'generation':0,'role':'admin'},'extra field'),({'refreshToken':t['refreshToken']},'missing generation')]:self.check('refresh '+label,lambda body=body:self.http('POST','/v1/sessions/refresh',body,{'Idempotency-Key':'structural'}),422)
  self.check('login Cookie rejected',lambda:self.http('POST','/v1/sessions',self.body(a),{'Cookie':'x=y','Idempotency-Key':'cookie'}),422)
  key=secrets.token_hex(12);self.check('login first key',lambda:self.login(a,key),201);self.check('login receipt',lambda:self.login(a,key),409,'REAUTH_REQUIRED')
  v=self.body(a);v['entry']='coach';self.check('login mismatch',lambda:self.login(a,key,v),409,'IDEMPOTENCY_MISMATCH')
  s=self.session(a);key=secrets.token_hex(12);r=self.check('refresh generation0',lambda:self.refresh(s,key),201);new=r[1]['data']
  self.invariant('rotation expiry fixed',new['generation']==1 and new['sessionId']==s['sessionId'] and new['refreshExpiresAt']==s['refreshExpiresAt'])
  self.check('old access rejected',lambda:self.ctx(s),401);self.check('refresh same key receipt',lambda:self.refresh(s,key),409,'REAUTH_REQUIRED');self.check('refresh changed generation receipt',lambda:self.refresh(s,key,1),409,'IDEMPOTENCY_MISMATCH')
  self.check('refresh different key reuse',lambda:self.refresh(s),401);self.check('reuse revokes access',lambda:self.ctx(new),401)
  s=self.session(a);t=self.session(a);key=secrets.token_hex(12);self.refresh(s,key);self.check('cross session key',lambda:self.refresh(t,key),409,'IDEMPOTENCY_MISMATCH')
  for label,g in [('negative',-1),('future',4),('string','0'),('boolean',True),('float',0.0)]:self.check('generation '+label,lambda g=g:self.refresh(t,generation=g),422 if isinstance(g,(str,bool,float)) else 401)
  t=self.session(b);s=self.session(a);self.check('cross account logout',lambda:self.logout(s,t['sessionId']),404);self.check('victim remains active',lambda:self.ctx(t),200)
  self.check('logout',lambda:self.logout(s),200);self.check('logout access',lambda:self.ctx(s),401);self.check('logout refresh',lambda:self.refresh(s),401);self.check('logout repeated old bearer',lambda:self.logout(s),401)
  s=self.session(a);key=secrets.token_hex(12);self.stop();self.start();self.check('restart access persistence',lambda:self.ctx(s),200);r=self.refresh(s,key);self.stop();self.start();self.check('restart refresh receipt',lambda:self.refresh(s,key),409,'REAUTH_REQUIRED');self.check('restart new access',lambda:self.ctx(r[1]['data']),200)
  for op in ('accessExpired','familyExpired','epoch','closed','disabled'):
   x=self.account();t=self.session(x);self.invariant('fixture '+op,self.db(op,x)>0)
   if op=='disabled':self.check('disabled credential',lambda:self.login(x),401)
   elif op=='closed':self.check('closed proven login',lambda:self.login(x),403,'ACCOUNT_CLOSED');self.check('closed contexts',lambda:self.ctx(t),401)
   elif op=='familyExpired':self.check('family expiration',lambda:self.refresh(t),401)
   else:self.check(op+' contexts',lambda:self.ctx(t),401)
  key=secrets.token_hex(12);rr=self.parallel([lambda:self.login(a,key),lambda:self.login(a,key)]);self.invariant('concurrent login same key',sorted(r[0] for r in rr)==[201,409])
  for same in (True,False):
   t=self.session(a);key=secrets.token_hex(12);rr=self.parallel([lambda:self.refresh(t,key),lambda:self.refresh(t,key if same else None)]);self.invariant('concurrent refresh '+str(same),sorted(r[0] for r in rr)==([201,409] if same else [201,401]))
   if not same:
    winner=next(r[1]['data'] for r in rr if r[0]==201);self.check('concurrent reuse revocation',lambda:self.ctx(winner),401)
  t=self.session(a);rr=self.parallel([lambda:self.logout(t),lambda:self.refresh(t)]);self.invariant('logout refresh bounded ordering',rr[0][0] in (200,401) and rr[1][0] in (201,401));self.check('logout race final old access',lambda:self.ctx(t),401)
  if rr[1][0]==201:
   winner=rr[1][1]['data'];self.invariant('refresh winner rejects old logout',rr[0][0]==401);self.check('logout race winner remains active',lambda:self.ctx(winner),200);self.check('logout winner explicitly',lambda:self.logout(winner),200);self.check('winner after explicit logout',lambda:self.ctx(winner),401)
  else:self.invariant('logout winner prevents refresh',rr[0][0]==200 and rr[1][0]==401)
  s=self.session(a);t=self.session(b);rr=self.parallel([lambda:self.logout(s,t['sessionId']),lambda:self.logout(t,s['sessionId'])]);self.invariant('cross account mutual logout',all(r[0]==404 for r in rr))
  for x in (a,b):self.invariant('DB digest generation invariant '+str(x==a),self.db('invariants',x)==0)
  # Fresh random HMAC isolates source counter threshold without deleting any rows.
  self.stop();self.env['GYM_IAM_HMAC_KEY']=secrets.token_hex(32);self.secrets.append(self.env['GYM_IAM_HMAC_KEY']);self.start()
  x=self.account();bad=self.body(x);bad['credential']['proof']='invalid-proof-value'
  for i in range(5):self.check('account failure '+str(i),lambda:self.login(x,body=bad),401,'AUTHENTICATION_FAILED')
  self.check('account limit ignores XFF',lambda:self.http('POST','/v1/sessions',self.body(x),{'Idempotency-Key':secrets.token_hex(12),'X-Forwarded-For':'198.51.100.8'}),429,'AUTH_RATE_LIMITED')
  # Source bucket is isolated by this process's random HMAC; no other process can share it.
  for i in range(25):
   self.check('source failure '+str(i+6),lambda:self.http('POST','/v1/sessions/refresh',{'refreshToken':'x'*43,'generation':0},{'Idempotency-Key':secrets.token_hex(12)}),401,'SESSION_INVALID')
  r=self.check('source limit XFF cannot bypass',lambda:self.http('POST','/v1/sessions',self.body(a),{'Idempotency-Key':secrets.token_hex(12),'X-Forwarded-For':'203.0.113.2'}),429,'AUTH_RATE_LIMITED');self.invariant('retry after bounded',0<int(r[2].get('Retry-After','0'))<=900)
  self.stop();self.start(default=True)
  self.invariant('default health',self.http('GET','/actuator/health')[0]==200)
  for method,path,body in [('POST','/v1/sessions',self.body(a)),('POST','/v1/sessions/refresh',{}),('GET','/v1/me/contexts',None),('DELETE','/v1/sessions/unknown',None)]:self.invariant('default denies '+method+path,self.http(method,path,body)[0] in (401,403))
 def save(self,path):
  result={'target':'e20e499','schema':SCHEMA,'cases':self.rows,'passed':sum(r['pass'] for r in self.rows),'total':len(self.rows)}
  raw=json.dumps(result,indent=2,ensure_ascii=False)
  if any(s in raw for s in self.secrets):raise RuntimeError('output redaction rejected')
  pathlib.Path(path).write_text(raw+'\n',encoding='utf-8')
def main():
 if '--reject-target' in sys.argv:
  if os.environ.get('GYM_DB_URL')!=URL:print('target rejected');return 2
  return 3
 h=None
 try:
  h=Harness();h.run()
 except Exception:
  if h:h.invariant('execution completed',False)
  print('Execution blocked or failed; no raw exception emitted.')
 finally:
  if h:
   h.stop();h.save(HERE/'results.json');print(f"checks={len(h.rows)} passed={sum(r['pass'] for r in h.rows)}")
 return 0 if h and h.rows and all(r['pass'] for r in h.rows) else 1
if __name__=='__main__':sys.exit(main())
