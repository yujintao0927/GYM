import {client,recordNotice,requireLogin} from '../../services/runtime';
import {Draft,validate,message} from '../../services/core';
function now(){const d=new Date();const pad=(n:number)=>String(n).padStart(2,'0');return {date:`${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}`,time:`${pad(d.getHours())}:${pad(d.getMinutes())}`};}
Page({data:{notice:recordNotice,kinds:['体重','饮食','训练'],index:0,kind:'weight',first:'',second:'',date:'',time:'',errors:{} as Record<string,string>,items:[] as {id:string;label:string;time:string}[],busy:false,error:'',saved:false},
 onShow(){if(!requireLogin())return;const d=client.draft;const n=now();this.setData({kind:d.kind||'weight',index:Math.max(0,['weight','diet','training'].indexOf(d.kind||'weight')),first:d.first||'',second:d.second||'',date:d.occurredAt?.split('T')[0]||n.date,time:d.occurredAt?.split('T')[1]||n.time});this.list();},
 list(){this.setData({items:client.records.map(r=>({id:r.id,label:r.kind==='weight'?`体重 ${r.first} kg`:r.kind==='diet'?`饮食 ${r.first} · ${r.second}`:`训练 ${r.first} · ${r.second} 分钟`,time:r.occurredAt.replace('T',' ')}))});},
 keep(){client.draft={kind:this.data.kind,first:this.data.first,second:this.data.second,occurredAt:`${this.data.date}T${this.data.time}`};},
 input(e:WechatMiniprogram.Input){this.setData({[e.currentTarget.dataset.field]:e.detail.value,saved:false});this.keep();},
 kindChange(e:WechatMiniprogram.PickerChange){const i=Number(e.detail.value);this.setData({index:i,kind:['weight','diet','training'][i],first:'',second:'',errors:{},saved:false});this.keep();},
 dateChange(e:WechatMiniprogram.PickerChange){this.setData({date:String(e.detail.value)});this.keep();},timeChange(e:WechatMiniprogram.PickerChange){this.setData({time:String(e.detail.value)});this.keep();},
 async save(){if(this.data.busy||!requireLogin())return;const d:Draft={kind:this.data.kind,first:this.data.first,second:this.data.second,occurredAt:`${this.data.date}T${this.data.time}`};const errors=validate(d);this.setData({errors,error:'',saved:false});if(Object.keys(errors).length)return;this.setData({busy:true});try{await client.save(d);this.setData({first:'',second:'',saved:true});this.list();}catch(e){this.setData({error:message(e)});requireLogin();}finally{this.setData({busy:false});}},
 back(){wx.navigateBack();},onHide(){this.setData({first:'',second:'',items:[]});},onUnload(){this.setData({first:'',second:'',items:[]});}
});
