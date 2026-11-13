import {it,expect,vi} from 'vitest';
import {SessionClient} from '../src/auth';
const s=(generation=0)=>({sessionId:'controlled-session',sessionToken:crypto.randomUUID(),refreshToken:crypto.randomUUID(),generation,expiresAt:'2099-01-01',refreshExpiresAt:'2099-01-02',authnSource:'LOCAL_DEV'});
const context={accountId:'controlled-subject',staffScopes:[],entitlementSources:[],asOf:'2026-11-13'};
const res=(data:unknown,status=200)=>new Response(JSON.stringify({data}),{status});
it('retry after refresh is bounded to one and repeated 401 returns login',async()=>{let contexts=0;const fetcher=vi.fn(async(p:string)=>p==='/v1/sessions'?res(s(),201):p.endsWith('/refresh')?res(s(1),201):++contexts===1?res(context):res({},401));const c=new SessionClient(fetcher);c.setMode('live-auth');await c.login('controlled','');await expect(c.loadContext()).rejects.toThrow();expect(c.context).toBeNull();expect(c.session).toBeNull();expect(fetcher.mock.calls.filter(([p])=>p.endsWith('/refresh'))).toHaveLength(1)});
it('missing initial generation is rejected',async()=>{const c=new SessionClient(async()=>res({...s(),generation:undefined},201));c.setMode('live-auth');await expect(c.login('controlled','')).rejects.toThrow();expect(c.session).toBeNull()});
it('context unavailable can retry without a mock fallback',async()=>{let n=0;const c=new SessionClient(async(p)=>p==='/v1/sessions'?res(s(),201):++n===2?res({},503):res(context));c.setMode('live-auth');await c.login('controlled','');await expect(c.loadContext()).rejects.toThrow();expect(c.mode).toBe('live-auth');await c.loadContext();expect(c.context?.accountId).toBe('controlled-subject')});

it('records retain only fields for their kind',async()=>{const {Records}=await import('../src/records');const r=new Records();r.add({kind:'weight',occurredAt:'2026-11-13T12:00',value:'1',name:'unused',portion:'unused'});expect(r.items[0]?.name).toBe('');expect(r.items[0]?.portion).toBe('')});
