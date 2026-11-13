import {createApp} from 'vue';
import {createRouter,createWebHashHistory} from 'vue-router';
import App from './App.vue';
import {client} from './state';
import './style.css';
const router=createRouter({history:createWebHashHistory(),routes:['login','home','records'].map(name=>({path:`/${name}`,name,component:{template:'<span />'}}))});
router.beforeEach(to=>{if(!client.context&&to.name!=='login')return '/login';if(client.context&&to.name==='login')return '/home';if(!to.matched.length)return client.context?'/home':'/login'});
createApp(App).use(router).mount('#app');
