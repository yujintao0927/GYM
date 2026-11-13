import {Client,Fault,Mode} from './core';
import {liveAuth,mockAuth,Transport} from './adapters/auth';
// Intentionally unconfigured; an explicit developer-only initialization is required.
let baseUrl='';
export function configureLocalAuth(url:string){if(!/^http:\/\/127\.0\.0\.1:\d+$/.test(url))throw new Fault(422);baseUrl=url;}
const transport:Transport=r=>new Promise((resolve,reject)=>{
 if(!baseUrl){reject(new Fault(503));return;}
 wx.request({url:baseUrl+r.path,method:r.method,data:r.data as WechatMiniprogram.IAnyObject,timeout:10000,header:{'Content-Type':'application/json',...(r.key?{'Idempotency-Key':r.key}:{}),...(r.token?{Authorization:`Bearer ${r.token}`}:{})},success:res=>{if(res.statusCode>=200&&res.statusCode<300){const envelope=res.data as {data:unknown};if(!envelope || typeof envelope!=='object' || !('data' in envelope)){reject(new Fault(502));return;}resolve(envelope.data);}else reject(new Fault(res.statusCode));},fail:()=>reject(new Fault(503))});
});
const mock=mockAuth(),live=liveAuth(transport);
export const client:Client=new Client({login:(u,p)=>(client.mode==='mock'?mock:live).login(u,p),refresh:s=>(client.mode==='mock'?mock:live).refresh(s),contexts:s=>(client.mode==='mock'?mock:live).contexts(s),logout:s=>(client.mode==='mock'?mock:live).logout(s)});
export const demoNotice='演示数据，未连接真实业务，请勿输入真实健康资料或密码';
export const recordNotice='记录演示，未保存到服务器；退出或冷启动后清除';
export function requireLogin(){if(!client.signedIn){wx.reLaunch({url:'/pages/login/login'});return false;}return true;}
export function setMode(mode:Mode){client.switchMode(mode);}
