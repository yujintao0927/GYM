const {test}=require('node:test');const a=require('node:assert/strict');
const {liveAuth,mockAuth}=require('../dist/services/adapters/auth.js');
const s={sessionId:'synthetic',sessionToken:'access-fixture',refreshToken:'refresh-fixture',generation:0,authnSource:'LOCAL_DEV'};
test('four locked routes, nested credential, generation zero and bearer',async()=>{
 const sent=[];const api=liveAuth(async r=>{sent.push(r);if(r.method==='GET')return {accountId:'synthetic',staffScopes:[],entitlementSources:[]};return s});
 await api.login('synthetic-input','synthetic-proof');await api.refresh(s);await api.contexts(s);await api.logout(s);
 a.deepEqual(sent.map(r=>r.path),['/v1/sessions','/v1/sessions/refresh','/v1/me/contexts','/v1/sessions/synthetic']);
 a.deepEqual(Object.keys(sent[0].data).sort(),['clientKind','credential','entry']);a.equal(sent[0].data.credential.kind,'LOCAL_PASSWORD');a.equal(sent[0].data.entry,'user-mini');a.equal(sent[1].data.generation,0);a.ok(sent[0].key!==sent[1].key);a.equal(sent[2].token,s.sessionToken);
});
test('malformed live response rejected and mock source separate',async()=>{await a.rejects(liveAuth(async()=>({})).login('',''));a.equal((await mockAuth().login('','')).authnSource,'MOCK')});
