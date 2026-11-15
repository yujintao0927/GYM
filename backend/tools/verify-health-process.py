"""Synthetic fixtures only. Run from backend after verify; secrets in environment only."""
import os, json, subprocess, socket, time, urllib.request, urllib.error, uuid, secrets
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
env=os.environ.copy()
assert env.get('GYM_TEST_DB_URL','').split('?')[0]=='jdbc:mysql://localhost:3306/gym_dev_test_night001'
env['GYM_DB_URL']=env['GYM_TEST_DB_URL']
env['SPRING_APPLICATION_JSON']=json.dumps({'gym':{'db':{'schema':'gym_dev_test_night001'}}})
java=str(Path(env['JAVA_HOME'])/'bin/java.exe'); jar=str(ROOT/'target/gym-0.0.1.jar')
password=secrets.token_urlsafe(24); name='health_'+uuid.uuid4().hex
result=subprocess.run([java,'-Dloader.main=com.gym.identity.local.LocalAccountCli','-cp',jar,'org.springframework.boot.loader.launch.PropertiesLauncher',name,'--password-stdin'],input=password+'\n',text=True,env=env,capture_output=True)
assert result.returncode==0,'CLI failed (output withheld)'
other_name='health_'+uuid.uuid4().hex
result=subprocess.run([java,'-Dloader.main=com.gym.identity.local.LocalAccountCli','-cp',jar,'org.springframework.boot.loader.launch.PropertiesLauncher',other_name,'--password-stdin'],input=password+'\n',text=True,env=env,capture_output=True)
assert result.returncode==0,'second CLI failed (output withheld)'
process=None
log=open(ROOT/'target/health-process.log','w',encoding='utf8')
def start(local=True):
 global process,port
 with socket.socket() as s:s.bind(('127.0.0.1',0));port=s.getsockname()[1]
 process=subprocess.Popen([java,'-jar',jar,'--server.port='+str(port)]+(['--spring.profiles.active=local-dev'] if local else []),env=env,stdout=log,stderr=log,creationflags=subprocess.CREATE_NO_WINDOW)
 deadline=time.monotonic()+45
 while time.monotonic()<deadline:
  if process.poll() is not None:raise AssertionError('own server exited')
  try:
   if call('GET','/actuator/health')[0]==200:return
  except OSError:pass
  time.sleep(.2)
 raise AssertionError('own server startup timeout')
def stop():
 global process
 if process:
  process.terminate()
  try:process.wait(10)
  except subprocess.TimeoutExpired:process.kill();process.wait()
  process=None
def call(method,path,body=None,token=None,key=None):
 headers={'Content-Type':'application/json'}
 if token:headers['Authorization']='Bearer '+token
 if key:headers['Idempotency-Key']=key
 req=urllib.request.Request('http://127.0.0.1:'+str(port)+path,data=None if body is None else json.dumps(body).encode(),headers=headers,method=method)
 try:
  with urllib.request.urlopen(req,timeout=10) as response:return response.status,json.load(response)
 except urllib.error.HTTPError as error:
  try:body=json.load(error)
  except ValueError:body={}
  return error.code,body
def ok(method,path,body=None,token=None,key=None):
 status,result=call(method,path,body,token,key);assert status in (200,201),'HTTP '+str(status);return result['data']
try:
 start()
 token=ok('POST','/v1/sessions',{'credential':{'kind':'LOCAL_PASSWORD','login':name,'proof':password},'entry':'user-web','clientKind':'WEB'},key=uuid.uuid4().hex)['sessionToken']
 other=ok('POST','/v1/sessions',{'credential':{'kind':'LOCAL_PASSWORD','login':other_name,'proof':password},'entry':'merchant','clientKind':'WEB'},key=uuid.uuid4().hex)['sessionToken']
 records=[]
 for kind,payload in [('WEIGHT',{'weightKg':'70.123'}),('DIET',{'foodName':'synthetic rice','portionText':'fixture'}),('TRAINING',{'name':'synthetic walk','durationMinutes':10}),('MEASUREMENT',{'heightCm':'175','waistCm':'80'})]:
  path='/v1/me/'+('body-measurements' if kind=='MEASUREMENT' else 'health-records');body={'occurredAt':'2026-11-13T00:00:00.000Z','payload':payload}
  if kind!='MEASUREMENT':body['kind']=kind
  key=uuid.uuid4().hex;record=ok('POST',path,body,token,key);records.append((path,record['id'],body,key))
 ok('PUT','/v1/me/weight-goal',{'targetWeightKg':'65','expectedVersion':0},token)
 stop();start()
 page=ok('GET','/v1/me/health-records?limit=1',token=token)
 assert len(page['items'])==1 and page['nextCursor']
 next_page=ok('GET','/v1/me/health-records?limit=1&cursor='+page['nextCursor'],token=token)
 assert next_page['items'][0]['id']!=page['items'][0]['id']
 assert len(ok('GET','/v1/me/health-records?kind=DIET',token=token)['items'])==1
 for path,id,body,key in records:
  assert ok('GET',path+'/'+id,token=token)['version']==1
  for method,body2,suffix in [('GET',None,''),('PATCH',{'occurredAt':body['occurredAt'],'payload':body['payload'],'expectedVersion':1},''),('DELETE',None,'?expectedVersion=1')]:assert call(method,path+'/'+id+suffix,body2,other)[0]==404
  assert call('GET',path+'/'+id)[0]==401
  ok('PATCH',path+'/'+id,{'occurredAt':body['occurredAt'],'payload':body['payload'],'expectedVersion':1},token)
  ok('DELETE',path+'/'+id+'?expectedVersion=2',token=token)
  assert call('POST',path,body,token,key)[0]==404
 assert ok('GET','/v1/me/weight-goal',token=token)['targetWeightKg']=='65'
 ok('DELETE','/v1/me/weight-goal?expectedVersion=1',token=token)
 stop();start(False)
 for path in ['/v1/me/health-records','/v1/me/body-measurements','/v1/me/weight-goal']:assert call('GET',path,token=token)[0] in (401,403)
 print('PASS: CLI, four categories, restart persistence, CAS, deletion replay, goal, anonymous, default profile')
finally:
 stop();log.close()
