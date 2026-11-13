import {Auth,Context,Fault,Session} from '../core';
export interface Request {path:string; method:'GET'|'POST'|'DELETE'; data?:unknown; token?:string; key?:string}
export type Transport=(r:Request)=>Promise<unknown>;
let sequence=0;
const key=()=>`mini-${Date.now()}-${++sequence}-${Math.random().toString(36).slice(2)}`;
function session(v:unknown):Session {const s=v as Session;if(!s || typeof s.sessionId!=='string'||!s.sessionId||typeof s.sessionToken!=='string'||!s.sessionToken||typeof s.refreshToken!=='string'||!s.refreshToken||!Number.isInteger(s.generation)||s.generation<0||s.authnSource!=='LOCAL_DEV')throw new Fault(502);return s;}
export function liveAuth(send:Transport):Auth {return {
 login:async(login,proof)=>session(await send({path:'/v1/sessions',method:'POST',data:{credential:{kind:'LOCAL_PASSWORD',login,proof},entry:'user-mini',clientKind:'MINI_PROGRAM'},key:key()})),
 refresh:async(s)=>session(await send({path:'/v1/sessions/refresh',method:'POST',data:{refreshToken:s.refreshToken,generation:s.generation},key:key()})),
 contexts:async(s)=>{const c=await send({path:'/v1/me/contexts',method:'GET',token:s.sessionToken}) as Context;if(!c||typeof c.accountId!=='string'||!Array.isArray(c.staffScopes)||!Array.isArray(c.entitlementSources))throw new Fault(502);return c;},
 logout:async(s)=>{await send({path:`/v1/sessions/${encodeURIComponent(s.sessionId)}`,method:'DELETE',token:s.sessionToken});}
};}
export function mockAuth():Auth {return {login:async()=>({sessionId:'demo',sessionToken:'demo-memory-only',refreshToken:'demo-refresh-only',generation:0,authnSource:'MOCK'}),contexts:async()=>({accountId:'演示用户',staffScopes:[],entitlementSources:[]}),refresh:async s=>({...s,generation:s.generation+1}),logout:async()=>{}};}
