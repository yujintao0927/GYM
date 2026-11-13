import {client,demoNotice,recordNotice,requireLogin} from '../../services/runtime';
import {message} from '../../services/core';
Page({data:{mode:'mock',notice:demoNotice,recordNotice,account:'',busy:false,error:''},
 onShow(){if(requireLogin())void this.reload();},
 async reload(){if(this.data.busy)return;this.setData({busy:true,error:'',mode:client.mode});try{const c=await client.loadContexts();this.setData({account:c.accountId});}catch(e){this.setData({error:message(e),account:''});requireLogin();}finally{this.setData({busy:false});}},
 records(){if(requireLogin())wx.navigateTo({url:'/pages/records/records'});},
 async logout(){if(this.data.busy)return;this.setData({busy:true});const ok=await client.logout();this.setData({account:'',busy:false});wx.reLaunch({url:'/pages/login/login'+(ok?'':'?logout=failed')});},
 onHide(){this.setData({account:''});}
});
