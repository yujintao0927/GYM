const {test}=require('node:test');
const a=require('node:assert/strict');
const {Client,Fault,validate}=require('../dist/services/core.js');
const session={sessionId:'fixture',sessionToken:'access-fixture',refreshToken:'refresh-fixture',generation:0,authnSource:'LOCAL_DEV'};
const contexts={accountId:'synthetic',staffScopes:[],entitlementSources:[]};
const auth=()=>({login:async()=>({...session}),contexts:async()=>contexts,refresh:async()=>({...session,generation:1}),logout:async()=>{}});
test('mock login and three record kinds; invalid fields reject',async()=>{
 const c=new Client(auth());await c.login('','');
 for(const [kind,first,second] of [['weight','60',''],['diet','演示食物','一份'],['training','演示训练','20']]) {await c.save({kind,first,second,occurredAt:'2026-11-13T12:00'});}
 a.equal(c.records.length,3);
 for(const first of ['','0','-1','Infinity','NaN']) a.ok(validate({kind:'weight',first,second:'',occurredAt:'2026-11-13T12:00'}).first);
 a.ok(validate({kind:'diet',first:'x',second:'',occurredAt:'bad'}).second);
 a.ok(validate({kind:'training',first:'x',second:'0',occurredAt:'bad'}).occurredAt);
});
test('single flight refresh and generation zero; one retry only',async()=>{
 let n=0;let seen; const api=auth();api.contexts=async s=>{if(s.generation===0)throw new Fault(401);return contexts};api.refresh=async s=>{n++;seen=s.generation;await new Promise(r=>setTimeout(r,5));return {...session,generation:1}};
 const c=new Client(api);await c.login('',''); // login also obtains contexts
 a.equal(n,1);a.equal(seen,0);await Promise.all([c.loadContexts(),c.loadContexts()]);a.equal(n,1);
});
test('refresh failure clears subject, draft and records without fallback',async()=>{
 const api=auth();const c=new Client(api);await c.login('',''); c.draft={first:'discard'};api.contexts=async()=>{throw new Fault(401)};api.refresh=async()=>{throw new Fault(409)};
 await a.rejects(c.loadContexts());a.equal(c.signedIn,false);a.equal(c.context,null);a.deepEqual(c.draft,{});
});
test('logout failure still clears memory and reports failure',async()=>{
 const api=auth();api.logout=async()=>{throw new Fault(503)};const c=new Client(api);await c.login('','');a.equal(await c.logout(),false);a.equal(c.signedIn,false);
});
test('mode change and delayed response cannot restore prior subject',async()=>{
 let resolve;const api=auth();api.login=()=>new Promise(r=>resolve=r);const c=new Client(api);const p=c.login('','');c.switchMode('live-auth');resolve(session);await a.rejects(p);a.equal(c.signedIn,false);a.equal(c.mode,'live-auth');
});
test('duplicate saves blocked; new login clears previous records',async()=>{
 const c=new Client(auth());await c.login('','');const d={kind:'weight',first:'60',second:'',occurredAt:'2026-11-13T12:00'};const p=c.save(d);await a.rejects(c.save(d));await p;a.equal(c.records.length,1);await c.login('','');a.equal(c.records.length,0);
});
test('live login error never becomes demo login',async()=>{const api=auth();api.login=async()=>{throw new Fault(503)};const c=new Client(api);c.switchMode('live-auth');await a.rejects(c.login('a','b'));a.equal(c.signedIn,false)});

test('simultaneous expired requests share one refresh; repeated 401 stops',async()=>{
 const api=auth();let expired=false,n=0;api.contexts=async s=>{if(expired&&s.generation===0)throw new Fault(401);return contexts};api.refresh=async s=>{n++;await new Promise(r=>setTimeout(r,5));return {...s,generation:1}};
 const c=new Client(api);await c.login('','');expired=true;await Promise.all([c.loadContexts(),c.loadContexts(),c.loadContexts()]);a.equal(n,1);
 api.contexts=async()=>{throw new Fault(401)};await a.rejects(c.loadContexts());a.equal(n,2);a.equal(c.signedIn,false);
});
test('failed save keeps draft; mode switch discards pending save',async()=>{
 const c=new Client(auth());await c.login('','');c.draft={first:'synthetic'};await a.rejects(c.save({kind:'weight',first:'0',second:'',occurredAt:'bad'}));a.equal(c.draft.first,'synthetic');
 const p=c.save({kind:'weight',first:'60',second:'',occurredAt:'2026-11-13T12:00'});c.switchMode('live-auth');await a.rejects(p);a.equal(c.records.length,0);
});

test('changed account context discards previous records and drafts',async()=>{const api=auth();const c=new Client(api);await c.login('','');await c.save({kind:'weight',first:'60',second:'',occurredAt:'2026-11-13T12:00'});c.draft={first:'discard'};api.contexts=async()=>({...contexts,accountId:'other-synthetic'});await c.loadContexts();a.equal(c.records.length,0);a.deepEqual(c.draft,{});});
test('impossible calendar dates rejected',()=>{a.ok(validate({kind:'weight',first:'60',second:'',occurredAt:'2026-02-31T12:00'}).occurredAt);});
