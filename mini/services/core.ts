export type Mode = 'mock' | 'live-auth';
export interface Session { sessionId:string; sessionToken:string; refreshToken:string; generation:number; authnSource:string }
export interface Context { accountId:string; staffScopes:unknown[]; entitlementSources:unknown[] }
export interface Auth { login(login:string,proof:string):Promise<Session>; refresh(s:Session):Promise<Session>; contexts(s:Session):Promise<Context>; logout(s:Session):Promise<void> }
export class Fault extends Error { constructor(public status:number) { super('服务暂不可用'); } }
export interface Draft { kind:string; first:string; second:string; occurredAt:string }
export function validate(d:Draft):Record<string,string> {
 const e:Record<string,string>={};
 if(!['weight','diet','training'].includes(d.kind))e.kind='请选择记录类型';
 if(!d.first.trim() || d.first.length>80)e.first='请输入1至80字';
 if(d.kind==='weight' && (!Number.isFinite(Number(d.first)) || Number(d.first)<=0 || Number(d.first)>999))e.first='请输入大于0且不超过999的kg数值';
 if(d.kind==='diet' && (!d.second.trim() || d.second.length>80))e.second='请输入1至80字分量';
 if(d.kind==='training' && (!Number.isFinite(Number(d.second)) || Number(d.second)<=0 || Number(d.second)>1440))e.second='请输入大于0且不超过1440的分钟数';
 const parsed=new Date(d.occurredAt);
 const calendarValid=parsed.getFullYear()===Number(d.occurredAt.slice(0,4)) && parsed.getMonth()+1===Number(d.occurredAt.slice(5,7)) && parsed.getDate()===Number(d.occurredAt.slice(8,10));
 if(!calendarValid || !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(d.occurredAt) || !Number.isFinite(Date.parse(d.occurredAt)))e.occurredAt='请选择有效日期和时间';
 return e;
}
export class Client {
 mode:Mode='mock'; private session:Session|null=null; context:Context|null=null;
 records:(Draft & {id:string})[]=[]; draft:Partial<Draft>={}; private epoch=0; private flight:Promise<void>|null=null; private saving=false;
 constructor(private api:Auth){}
 get signedIn(){return this.session!==null;}
 clear(){this.epoch++;this.session=null;this.context=null;this.records=[];this.draft={};this.flight=null;this.saving=false;}
 switchMode(mode:Mode){this.clear();this.mode=mode;}
 private check(epoch:number){if(epoch!==this.epoch)throw new Fault(401);}
 async login(login:string,proof:string){this.clear();const epoch=this.epoch;const s=await this.api.login(login,proof);this.check(epoch);if(s.generation!==0)throw new Fault(502);this.session=s;try{await this.loadContexts();}catch(e){if(epoch===this.epoch)this.clear();throw e;}}
 private acceptContext(c:Context){if(this.context && this.context.accountId!==c.accountId){this.records=[];this.draft={};}this.context=c;}
 async loadContexts(){
 const epoch=this.epoch;const s=this.session;if(!s)throw new Fault(401);
 try {const c=await this.api.contexts(s);this.check(epoch);this.acceptContext(c);return c;}
 catch(e){this.check(epoch);if(!(e instanceof Fault) || e.status!==401)throw e;
 try {
 // A late 401 for the previous access token reuses an already completed refresh.
 if(this.session===s){if(!this.flight){const f=this.api.refresh(s).then(next=>{this.check(epoch);if(next.sessionId!==s.sessionId || next.generation<=s.generation)throw new Fault(502);this.session=next;});this.flight=f;const done=()=>{if(this.flight===f)this.flight=null;};f.then(done,done);}await this.flight;}
 this.check(epoch);const c=await this.api.contexts(this.session!);this.check(epoch);this.acceptContext(c);return c;
 }catch(error){if(epoch===this.epoch)this.clear();throw error;}
 }
 }
 async logout(){const s=this.session;this.clear();if(!s)return true;try{await this.api.logout(s);return true;}catch{return false;}}
 async save(d:Draft){if(!this.signedIn)throw new Fault(401);if(this.saving)throw new Fault(409);if(Object.keys(validate(d)).length)throw new Fault(422);this.saving=true;const epoch=this.epoch;try{await Promise.resolve();this.check(epoch);this.records=[{...d,id:`record-${Date.now()}-${this.records.length}`},...this.records];this.draft={};}finally{if(epoch===this.epoch)this.saving=false;}}
}
export function message(e:unknown){if(e instanceof Fault){if(e.status===401||e.status===409)return '会话已失效，请重新登录';if(e.status===403)return '当前账号无法执行此操作';if(e.status===429)return '操作频繁，请稍后重试';if(e.status===422)return '请检查输入';}return '服务暂不可用，请重试';}
