<script setup lang="ts">
import {ref,reactive,computed,watch,nextTick} from 'vue';
import {useRoute,useRouter} from 'vue-router';
import {client,records} from './state';
import {errors,type Draft} from './records';
import type {AppMode} from './auth';
const route=useRoute(),router=useRouter();const busy=ref(false),message=ref(''),login=ref(''),proof=ref('');
const fresh=():Draft=>({kind:'weight',occurredAt:new Date(Date.now()-new Date().getTimezoneOffset()*60000).toISOString().slice(0,16),value:'',name:'',portion:''});
const draft=reactive(fresh()),fieldErrors=ref<ReturnType<typeof errors>>({});const filter=ref('all');const visible=computed(()=>records.items.filter(i=>filter.value==='all'||i.kind===filter.value));
const labels={weight:'体重',food:'饮食',training:'训练'};
watch(()=>client.context?.accountId,()=>{records.clear();Object.assign(draft,fresh());fieldErrors.value={};filter.value='all'});
watch(()=>route.path,async()=>{message.value='';await nextTick();document.querySelector<HTMLElement>('h1')?.focus()});
function mode(event:Event){client.setMode((event.target as HTMLSelectElement).value as AppMode);records.clear();proof.value='';login.value='';message.value='';Object.assign(draft,fresh());void router.replace('/login')}
async function signIn(){if(busy.value)return;busy.value=true;message.value='';try{await client.login(login.value,proof.value);proof.value='';await router.replace('/home')}catch(e){message.value=e instanceof Error?e.message:'登录失败';proof.value=''}finally{busy.value=false}}
async function signOut(){if(busy.value)return;busy.value=true;let failed=false;try{await client.logout()}catch{failed=true}finally{records.clear();Object.assign(draft,fresh());await router.replace('/login');message.value=failed?'已退出本机；服务暂不可用，未确认服务器会话已撤销。':'已退出，演示记录已清除。';busy.value=false}}
async function refresh(){if(busy.value)return;busy.value=true;message.value='';try{await client.loadContext();message.value='会话状态已更新'}catch(e){if(!client.context){records.clear();await router.replace('/login')}message.value=e instanceof Error?e.message:'加载失败，请重试'}finally{busy.value=false}}
async function save(){if(busy.value)return;fieldErrors.value=errors(draft);if(Object.keys(fieldErrors.value).length){await nextTick();document.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus();return}busy.value=true;try{records.add({...draft});Object.assign(draft,fresh());message.value='已加入演示列表，未保存到服务器。';await nextTick()}catch{message.value='添加失败，请重试，输入仍保留。'}finally{busy.value=false}}
</script>
<template>
<div class="shell">
<header><a class="brand" href="#/home">GYM <span>每日记录</span></a><label class="mode">连接方式<select aria-label="连接方式" :value="client.mode" :disabled="busy" @change="mode"><option value="mock">演示体验</option><option value="live-auth">本地开发认证</option></select></label></header>
<aside class="notice" v-if="client.mode==='mock'">演示数据，未连接真实业务，请勿输入真实健康资料或密码。</aside>
<aside class="notice" v-else>本地开发认证 · 仅连接开发账号服务；记录仍为演示，请勿输入真实健康资料。</aside>
<nav v-if="client.context" aria-label="主导航"><RouterLink to="/home">首页</RouterLink><RouterLink to="/records">手动记录</RouterLink><button class="quiet" :disabled="busy" @click="signOut">退出登录</button></nav>
<p v-if="message" role="status" class="status">{{message}}</p>
<main>
<section v-if="route.name==='login'" class="login card"><p class="eyebrow">从今天的一笔开始</p><h1 tabindex="-1">欢迎来到 GYM</h1><p class="muted">记录体重、饮食和训练，让日常更有条理。</p>
<form @submit.prevent="signIn">
<template v-if="client.mode==='mock'"><label>演示主体<select v-model="login" aria-label="演示主体"><option value="">演示甲</option><option value="b">演示乙</option></select></label><p class="muted">无需密码，记录仅保留在当前页面内存中。</p></template>
<template v-else><label>开发账号<input v-model="login" required maxlength="120" autocomplete="off" autocapitalize="none"></label><label>开发账号密码<input v-model="proof" required type="password" autocomplete="off"></label><p class="muted">请使用已配置的本地开发账号。服务不可用时可重试或手动切换演示体验。</p></template>
<button type="submit" :disabled="busy">{{busy?'正在登录…':client.mode==='mock'?'进入演示':'登录'}}</button></form></section>
<section v-else-if="route.name==='home'">
<p class="eyebrow">我的日常</p><h1 tabindex="-1">今天，也为自己留一笔。</h1><p class="muted">从简单记录开始。无需会员即可体验手动记录。</p>
<div class="grid"><article class="card"><h2>当前会话</h2><p>当前主体：{{client.context?.accountId}}</p><p>{{client.mode==='mock'?'演示身份':'本地开发身份'}}</p><p class="muted">{{client.mode==='mock'?'本页未连接账号服务。':'仅展示账号服务返回的本人上下文。'}}</p><button class="secondary" :disabled="busy" @click="refresh">{{busy?'正在更新…':'更新会话状态'}}</button></article>
<article class="card"><h2>手动记录</h2><p>体重、饮食、训练，按自己的节奏填写。</p><p class="muted">当前共 {{records.items.length}} 条演示记录</p><RouterLink class="button" to="/records">开始记录</RouterLink><p class="caption">记录演示，未保存到服务器</p></article></div>
</section>
<section v-else-if="route.name==='records'">
<p class="eyebrow">每日一笔</p><h1 tabindex="-1">手动记录</h1><p class="notice">记录演示，未保存到服务器。退出、切换主体或连接方式、刷新页面后清除。</p>
<div class="grid records"><article class="card"><h2>新增记录</h2><form novalidate @submit.prevent="save">
<label>记录类型<select v-model="draft.kind" @change="fieldErrors={}"><option value="weight">体重</option><option value="food">饮食</option><option value="training">训练</option></select></label>
<label>记录时间（本地时间）<input v-model="draft.occurredAt" type="datetime-local" :aria-invalid="!!fieldErrors.occurredAt" aria-describedby="time-error"><span id="time-error" class="error">{{fieldErrors.occurredAt}}</span></label>
<label v-if="draft.kind!=='food'">{{draft.kind==='weight'?'体重（kg）':'训练时长（分钟）'}}<input v-model="draft.value" inputmode="decimal" :aria-invalid="!!fieldErrors.value" aria-describedby="value-error"><span class="error" id="value-error">{{fieldErrors.value}}</span></label>
<label v-if="draft.kind!=='weight'">{{draft.kind==='food'?'食物名称':'训练名称'}}<input v-model="draft.name" maxlength="80" :aria-invalid="!!fieldErrors.name" aria-describedby="name-error"><span class="error" id="name-error">{{fieldErrors.name}}</span></label>
<label v-if="draft.kind==='food'">分量<input v-model="draft.portion" maxlength="120" :aria-invalid="!!fieldErrors.portion" aria-describedby="portion-error"><span class="error" id="portion-error">{{fieldErrors.portion}}</span></label>
<button :disabled="busy" type="submit">{{busy?'添加中…':'添加到演示列表'}}</button><RouterLink class="back" to="/home">返回首页</RouterLink>
</form></article><article class="card"><h2>我的记录</h2><label>筛选记录<select v-model="filter"><option value="all">全部</option><option value="weight">体重</option><option value="food">饮食</option><option value="training">训练</option></select></label><p v-if="!visible.length" class="empty">还没有记录，从左侧表单开始添加。</p><ul v-else class="list"><li v-for="item in visible" :key="item.id"><strong>{{labels[item.kind]}}</strong><time>{{item.occurredAt.replace('T',' ')}}</time><p>{{item.kind==='weight'?`${item.value} kg`:item.kind==='food'?`${item.name} · ${item.portion}`:`${item.name} · ${item.value} 分钟`}}</p><small>演示 · 未保存到服务器</small></li></ul></article></div>
</section>
</main><footer>GYM · 日常由你记录 <span>刷新页面将结束本次体验</span></footer>
</div>
</template>
