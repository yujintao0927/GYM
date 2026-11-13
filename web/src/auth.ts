export type AppMode='mock'|'live-auth';
export interface Session {sessionId:string;sessionToken:string;refreshToken:string;generation:number;expiresAt:string;refreshExpiresAt:string;authnSource:string}
export interface Context {accountId:string;staffScopes:unknown[];entitlementSources:unknown[];asOf:string}
export class ApiError extends Error {constructor(public status:number,public code='SERVICE_UNAVAILABLE'){super(status===401?'登录已失效，请重新登录':status===409?'会话无法恢复，请重新登录':status===429?'操作频繁，请稍后重试':'服务暂不可用，请稍后重试')}}
export class SessionClient {
 mode:AppMode='mock';session:Session|null=null;context:Context|null=null;
 private epoch=0;private refreshing:Promise<void>|null=null;
 constructor(private transport:(path:string,init?:RequestInit)=>Promise<Response>=fetch){}
 clear(){this.epoch++;this.session=null;this.context=null;this.refreshing=null}
 setMode(mode:AppMode){this.clear();this.mode=mode}
 private async request<T>(path:string,method='GET',body?:unknown,bearer?:string):Promise<T>{
  let response:Response;try{response=await this.transport(path,{method,cache:'no-store',credentials:'omit',signal:AbortSignal.timeout(10000),headers:{'Content-Type':'application/json',...(method==='POST'?{'Idempotency-Key':crypto.randomUUID()}:{}),...(bearer?{Authorization:`Bearer ${bearer}`}:{})},...(body===undefined?{}:{body:JSON.stringify(body)})})}catch{throw new ApiError(503)}
  if(!response.ok)throw new ApiError(response.status);
  try{return (await response.json()).data as T}catch{throw new ApiError(502)}
 }
 private validSession(s:Session){if(!s||!s.sessionId||!s.sessionToken||!s.refreshToken||!Number.isInteger(s.generation)||s.authnSource!=='LOCAL_DEV')throw new ApiError(502);return s}
 async login(login:string,proof:string){this.clear();const epoch=this.epoch;
  if(this.mode==='mock'){this.context={accountId:login==='b'?'演示乙':'演示甲',staffScopes:[],entitlementSources:[],asOf:new Date().toISOString()};return}
  try{const s=this.validSession(await this.request<Session>('/v1/sessions','POST',{credential:{kind:'LOCAL_PASSWORD',login,proof},entry:'user-web',clientKind:'WEB'}));if(epoch!==this.epoch)throw new ApiError(401);if(s.generation!==0)throw new ApiError(502);this.session=s;await this.loadContext()}catch(e){if(epoch===this.epoch)this.clear();throw e}
 }
 async loadContext(){if(this.mode==='mock')return this.context;const epoch=this.epoch;const session=this.session;if(!session)throw new ApiError(401);
  try{let context:Context;try{context=await this.request<Context>('/v1/me/contexts','GET',undefined,session.sessionToken)}catch(e){if(!(e instanceof ApiError)||e.status!==401)throw e;if(epoch!==this.epoch)throw new ApiError(401);
   if(this.session===session){if(!this.refreshing){this.refreshing=this.refresh(session,epoch).finally(()=>{if(epoch===this.epoch)this.refreshing=null})}await this.refreshing}
   if(epoch!==this.epoch||!this.session)throw new ApiError(401);context=await this.request<Context>('/v1/me/contexts','GET',undefined,this.session.sessionToken)
  }if(epoch!==this.epoch)throw new ApiError(401);if(!context?.accountId||!Array.isArray(context.staffScopes)||!Array.isArray(context.entitlementSources))throw new ApiError(502);if(this.context&&this.context.accountId!==context.accountId){this.clear();throw new ApiError(401)}this.context=context;return context
  }catch(e){if(epoch===this.epoch&&e instanceof ApiError&&(e.status===401||e.status===409))this.clear();throw e}
 }
 private async refresh(s:Session,epoch:number){try{const next=this.validSession(await this.request<Session>('/v1/sessions/refresh','POST',{refreshToken:s.refreshToken,generation:s.generation}));if(epoch!==this.epoch)throw new ApiError(401);if(next.sessionId!==s.sessionId||next.generation<=s.generation)throw new ApiError(502);this.session=next}catch(e){if(epoch===this.epoch)this.clear();throw e}}
 async logout(){const s=this.session;const live=this.mode==='live-auth';this.clear();if(live&&s)await this.request(`/v1/sessions/${encodeURIComponent(s.sessionId)}`,'DELETE',undefined,s.sessionToken)}
}
