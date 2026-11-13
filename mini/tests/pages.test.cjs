const {test}=require('node:test');const a=require('node:assert/strict');
function page(name){let p;global.Page=x=>p=x;delete require.cache[require.resolve(`../dist/pages/${name}/${name}.js`)];require(`../dist/pages/${name}/${name}.js`);p.data=structuredClone(p.data);p.setData=x=>Object.assign(p.data,x);return p;}
const nav=[];global.wx={reLaunch:x=>nav.push(x.url),navigateTo:x=>nav.push(x.url),navigateBack:()=>nav.push('back')};
const {client,setMode}=require('../dist/services/runtime.js');
test('page handlers: demo login, home, invalid and valid records, draft, logout',async()=>{
 setMode('mock');const login=page('login');await login.submit();a.equal(nav.pop(),'/pages/home/home');a.equal(login.data.proof,'');
 const home=page('home');await home.reload();a.equal(home.data.account,'演示用户');home.records();a.equal(nav.pop(),'/pages/records/records');
 const records=page('records');records.onShow();a.equal(records.data.items.length,0);await records.save();a.ok(records.data.errors.first);
 records.input({currentTarget:{dataset:{field:'first'}},detail:{value:'60'}});await records.save();a.equal(records.data.items.length,1);a.equal(records.data.saved,true);
 records.input({currentTarget:{dataset:{field:'first'}},detail:{value:'61'}});records.onHide();a.equal(records.data.first,'');records.onShow();a.equal(records.data.first,'61');
 for(const [i,first,second] of [[1,'演示食物','一份'],[2,'演示训练','20']]){records.kindChange({detail:{value:String(i)}});records.input({currentTarget:{dataset:{field:'first'}},detail:{value:first}});records.input({currentTarget:{dataset:{field:'second'}},detail:{value:second}});await records.save();}
 a.equal(records.data.items.length,3);await home.logout();a.equal(client.records.length,0);a.equal(client.signedIn,false);a.equal(nav.pop(),'/pages/login/login');
 records.onShow();a.equal(nav.pop(),'/pages/login/login');
});
test('live mode without local endpoint displays error and never succeeds',async()=>{setMode('live-auth');const p=page('login');p.data.login='synthetic';p.data.proof='synthetic';await p.submit();a.equal(client.signedIn,false);a.ok(p.data.error);a.equal(p.data.proof,'');});

test('null success envelope rejects instead of leaving login pending',async()=>{const runtime=require('../dist/services/runtime.js');runtime.configureLocalAuth('http://127.0.0.1:19001');let request;global.wx.request=r=>{request=r};setMode('live-auth');const pending=client.login('synthetic','synthetic');a.doesNotThrow(()=>request.success({statusCode:201,data:null}));await a.rejects(pending);a.equal(client.signedIn,false);});
