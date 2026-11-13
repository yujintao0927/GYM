import {client,demoNotice,setMode} from '../../services/runtime';
import {message} from '../../services/core';
Page({data:{mode:'mock',notice:demoNotice,login:'',proof:'',busy:false,error:'',noticeFromLogout:''},
 onLoad(q:Record<string,string>){this.setData({noticeFromLogout:q.logout==='failed'?'已清除本机登录；服务端撤销未确认，请稍后重新登录核实。':'',mode:client.mode});},
 modeChange(e:WechatMiniprogram.SwitchChange){setMode(e.detail.value?'live-auth':'mock');this.setData({mode:client.mode,login:'',proof:'',error:''});},
 input(e:WechatMiniprogram.Input){const field=e.currentTarget.dataset.field as 'login'|'proof';this.setData({[field]:e.detail.value});},
 async submit(){if(this.data.busy)return;if(client.mode==='live-auth'&&(!this.data.login.trim()||!this.data.proof)){this.setData({error:'请输入账号及密码'});return;}this.setData({busy:true,error:''});try{await client.login(this.data.login,this.data.proof);this.setData({proof:'',login:''});wx.reLaunch({url:'/pages/home/home'});}catch(e){this.setData({proof:'',error:message(e)});}finally{this.setData({busy:false});}},
 onHide(){this.setData({proof:'',login:''});},onUnload(){this.setData({proof:'',login:''});}
});
