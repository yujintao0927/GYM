<script setup lang="ts">
import { ref, reactive, computed, watch, nextTick } from "vue";
import { useRoute, useRouter } from "vue-router";
import { client, records } from "./state";
import { errors, type Draft } from "./records";
import type { AppMode } from "./auth";
import {
  PhBarbell,
  PhForkKnife,
  PhScales,
  PhHouse,
  PhClipboardText,
  PhInfo,
  PhCalendarBlank,
  PhCaretRight,
  PhUserCircle,
} from "@phosphor-icons/vue";
const route = useRoute(),
  router = useRouter();
const busy = ref(false),
  message = ref(""),
  login = ref(""),
  proof = ref("");
const fresh = (): Draft => ({
  kind: "weight",
  occurredAt: new Date(Date.now() - new Date().getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 16),
  value: "",
  name: "",
  portion: "",
});
const draft = reactive(fresh()),
  fieldErrors = ref<ReturnType<typeof errors>>({});
const filter = ref("all");
const visible = computed(() =>
  records.items.filter(
    (i) => filter.value === "all" || i.kind === filter.value,
  ),
);
const labels = { weight: "体重", food: "饮食", training: "训练" };
watch(
  () => client.context?.accountId,
  () => {
    records.clear();
    Object.assign(draft, fresh());
    fieldErrors.value = {};
    filter.value = "all";
  },
);
watch(
  () => route.path,
  async () => {
    message.value = "";
    await nextTick();
    document.querySelector<HTMLElement>("h1")?.focus();
  },
);
function mode(event: Event) {
  client.setMode((event.target as HTMLSelectElement).value as AppMode);
  records.clear();
  proof.value = "";
  login.value = "";
  message.value = "";
  Object.assign(draft, fresh());
  void router.replace("/login");
}
async function signIn() {
  if (busy.value) return;
  busy.value = true;
  message.value = "";
  try {
    await client.login(login.value, proof.value);
    proof.value = "";
    await router.replace("/home");
  } catch (e) {
    message.value = e instanceof Error ? e.message : "登录失败";
    proof.value = "";
  } finally {
    busy.value = false;
  }
}
async function signOut() {
  if (busy.value) return;
  busy.value = true;
  let failed = false;
  try {
    await client.logout();
  } catch {
    failed = true;
  } finally {
    records.clear();
    Object.assign(draft, fresh());
    await router.replace("/login");
    message.value = failed
      ? "已退出本机；服务暂不可用，未确认服务器会话已撤销。"
      : "已退出，演示记录已清除。";
    busy.value = false;
  }
}
async function refresh() {
  if (busy.value) return;
  busy.value = true;
  message.value = "";
  try {
    await client.loadContext();
    message.value = "会话状态已更新";
  } catch (e) {
    if (!client.context) {
      records.clear();
      await router.replace("/login");
    }
    message.value = e instanceof Error ? e.message : "加载失败，请重试";
  } finally {
    busy.value = false;
  }
}
async function save() {
  if (busy.value) return;
  fieldErrors.value = errors(draft);
  if (Object.keys(fieldErrors.value).length) {
    await nextTick();
    document.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus();
    return;
  }
  busy.value = true;
  try {
    records.add({ ...draft });
    Object.assign(draft, fresh());
    message.value = "已加入演示列表，未保存到服务器。";
    await nextTick();
  } catch {
    message.value = "添加失败，请重试，输入仍保留。";
  } finally {
    busy.value = false;
  }
}

const categories = [
  { kind: "weight", label: "体重", icon: PhScales },
  { kind: "food", label: "饮食", icon: PhForkKnife },
  { kind: "training", label: "训练", icon: PhBarbell },
] as const;
const today = new Intl.DateTimeFormat("sv-SE").format(new Date());
function switchMode(value: AppMode) {
  mode({ target: { value } } as unknown as Event);
}
function selectKind(kind: Draft["kind"]) {
  draft.kind = kind;
  fieldErrors.value = {};
}
async function moveTab(event: KeyboardEvent, index: number) {
  if (
    ![
      "ArrowRight",
      "ArrowDown",
      "ArrowLeft",
      "ArrowUp",
      "Home",
      "End",
    ].includes(event.key)
  )
    return;
  event.preventDefault();
  let next =
    event.key === "Home"
      ? 0
      : event.key === "End"
        ? 2
        : (index +
            (event.key === "ArrowRight" || event.key === "ArrowDown" ? 1 : 2)) %
          3;
  selectKind(categories[next]!.kind);
  await nextTick();
  document.getElementById(`tab-${draft.kind}`)?.focus();
}
function details(item: Draft) {
  return item.kind === "weight"
    ? `${item.value} kg`
    : item.kind === "food"
      ? `${item.name} · ${item.portion}`
      : `${item.name} · ${item.value} 分钟`;
}
</script>
<template>
  <div class="shell" :class="{ 'is-login': route.name === 'login' }">
    <header class="topbar">
      <a class="brand" href="#/home" aria-label="GYM 首页"
        ><b>GYM</b><span>Strength club</span></a
      >
      <nav v-if="client.context" class="desktop-nav" aria-label="主导航">
        <RouterLink to="/home">首页</RouterLink
        ><RouterLink to="/records">手动记录</RouterLink>
      </nav>
      <div v-if="client.context" class="account">
        <span :title="`当前主体：${client.context.accountId}`"
          ><PhUserCircle aria-hidden="true" />{{
            client.context.accountId
          }}</span
        ><button class="text-button" :disabled="busy" @click="signOut">
          退出登录
        </button>
      </div>
      <time v-else class="header-date">{{ today }}</time>
    </header>
    <p v-if="message" role="status" class="status">{{ message }}</p>
    <main>
      <section v-if="route.name === 'login'" class="login-layout">
        <div class="login-photo">
          <img src="/images/gym-interior.png" alt="暖色灯光下的力量训练空间" />
          <p>练过的每一次，<br />都值得记下。</p>
        </div>
        <div class="login-content">
          <div class="login-inner">
            <h1 tabindex="-1">欢迎回来</h1>
            <p class="intro">开始你的记录体验。</p>
            <div class="mode-tabs" aria-label="连接方式">
              <button
                :class="{ active: client.mode === 'mock' }"
                :disabled="busy"
                @click="switchMode('mock')"
              >
                演示体验</button
              ><button
                :class="{ active: client.mode === 'live-auth' }"
                :disabled="busy"
                @click="switchMode('live-auth')"
              >
                本地开发认证
              </button>
            </div>
            <form @submit.prevent="signIn">
              <label v-if="client.mode === 'mock'"
                >演示主体<select v-model="login" aria-label="演示主体">
                  <option value="">演示甲</option>
                  <option value="b">演示乙</option>
                </select></label
              >
              <template v-else
                ><label
                  >开发账号<input
                    v-model="login"
                    required
                    maxlength="120"
                    autocomplete="off"
                    autocapitalize="none" /></label
                ><label
                  >开发账号密码<input
                    v-model="proof"
                    required
                    type="password"
                    autocomplete="off" /></label
              ></template>
              <button
                class="primary login-submit"
                type="submit"
                :disabled="busy"
              >
                {{
                  busy
                    ? "正在登录…"
                    : client.mode === "mock"
                      ? "进入演示"
                      : "登录"
                }}
              </button>
              <p class="help">
                {{
                  client.mode === "mock"
                    ? "无需密码，刷新后记录将清除。"
                    : "仅使用已配置的本地开发账号，服务失败不会切换为演示。"
                }}
              </p>
            </form>
          </div>
          <p class="disclaimer">
            <PhInfo aria-hidden="true" />{{
              client.mode === "mock"
                ? "演示体验，请勿输入真实健康资料或密码。"
                : "本地开发认证，记录仍为演示，请勿输入真实健康资料。"
            }}
          </p>
        </div>
      </section>
      <template v-else>
        <section v-if="route.name === 'home'" class="home">
          <div class="home-hero">
            <img
              src="/images/gym-interior.png"
              alt="黑色力量器械与暖色建筑照明"
            />
            <div class="home-copy">
              <h1 tabindex="-1">练过的每一次，<br />都值得记下。</h1>
              <RouterLink class="primary" to="/records">添加记录</RouterLink>
            </div>
          </div>
          <div class="home-workspace">
            <div class="quick-records">
              <RouterLink
                v-for="category in categories"
                :key="category.kind"
                to="/records"
                @click="selectKind(category.kind)"
                ><strong>{{ category.label }}</strong
                ><span>{{
                  category.kind === "weight"
                    ? "kg"
                    : category.kind === "food"
                      ? "食物与分量"
                      : "名称与时长"
                }}</span
                ><PhCaretRight aria-hidden="true"
              /></RouterLink>
            </div>
            <div class="home-recent">
              <h2>最近记录</h2>
              <time>{{ today }}</time>
              <p v-if="!records.items.length" class="empty">
                还没有记录，从第一笔开始。
              </p>
              <ul v-else class="home-list">
                <li v-for="item in records.items.slice(0, 3)" :key="item.id">
                  <strong>{{ labels[item.kind] }}</strong
                  ><span>{{ details(item) }}</span
                  ><small>演示 · 未保存服务器</small>
                </li>
              </ul>
            </div>
          </div>
        </section>
        <section v-else-if="route.name === 'records'" class="records-page">
          <div class="record-console">
            <img
              class="record-photo"
              src="/images/dumbbells.png"
              alt="暖铜色反光的金属哑铃"
            />

            <div class="category-tabs" role="tablist" aria-label="记录类型">
              <button
                v-for="(category, index) in categories"
                :id="`tab-${category.kind}`"
                :key="category.kind"
                role="tab"
                :aria-selected="draft.kind === category.kind"
                :tabindex="draft.kind === category.kind ? 0 : -1"
                aria-controls="record-panel"
                @click="selectKind(category.kind)"
                @keydown="moveTab($event, index)"
              >
                <component :is="category.icon" aria-hidden="true" /><span>{{
                  category.label
                }}</span>
              </button>
            </div>
            <div
              id="record-panel"
              class="record-entry"
              role="tabpanel"
              :aria-labelledby="`tab-${draft.kind}`"
            >
              <h1 tabindex="-1">记下今天</h1>
              <form novalidate @submit.prevent="save">
                <label v-if="draft.kind !== 'weight'" class="name-field"
                  >{{ draft.kind === "food" ? "食物名称" : "训练名称"
                  }}<input
                    v-model="draft.name"
                    :placeholder="
                      draft.kind === 'food' ? '输入食物名称' : '输入训练名称'
                    "
                    maxlength="80"
                    :aria-invalid="!!fieldErrors.name"
                    aria-describedby="name-error"
                  /><span class="error" id="name-error">{{
                    fieldErrors.name
                  }}</span></label
                >
                <label
                  v-if="draft.kind !== 'food'"
                  class="number-field"
                  :class="{ large: draft.kind === 'weight' }"
                  >{{
                    draft.kind === "weight" ? "体重（kg）" : "训练时长（分钟）"
                  }}<span class="unit-input"
                    ><input
                      v-model="draft.value"
                      inputmode="decimal"
                      :placeholder="
                        draft.kind === 'weight' ? '输入体重' : '输入分钟数'
                      "
                      :aria-invalid="!!fieldErrors.value"
                      aria-describedby="value-error"
                    /><span aria-hidden="true">{{
                      draft.kind === "weight" ? "kg" : "分钟"
                    }}</span></span
                  ><span class="error" id="value-error">{{
                    fieldErrors.value
                  }}</span></label
                >
                <label v-if="draft.kind === 'food'"
                  >分量<input
                    v-model="draft.portion"
                    placeholder="输入分量"
                    maxlength="120"
                    :aria-invalid="!!fieldErrors.portion"
                    aria-describedby="portion-error"
                  /><span class="error" id="portion-error">{{
                    fieldErrors.portion
                  }}</span></label
                >
                <label class="time-field"
                  ><span
                    ><PhCalendarBlank
                      aria-hidden="true"
                    />记录时间（本地时间）</span
                  ><input
                    v-model="draft.occurredAt"
                    type="datetime-local"
                    :aria-invalid="!!fieldErrors.occurredAt"
                    aria-describedby="time-error"
                  /><span class="error" id="time-error">{{
                    fieldErrors.occurredAt
                  }}</span></label
                >
                <button
                  class="primary save-button"
                  :disabled="busy"
                  type="submit"
                >
                  {{ busy ? "添加中…" : "添加到演示列表" }}
                </button>
              </form>
            </div>
          </div>
          <section class="record-history">
            <div class="history-heading">
              <h2>最近记录</h2>
              <select v-model="filter" aria-label="筛选记录">
                <option value="all">全部</option>
                <option value="weight">体重</option>
                <option value="food">饮食</option>
                <option value="training">训练</option></select
              ><time>{{ today }}</time>
            </div>
            <p v-if="!visible.length" class="empty">
              还没有记录，从第一笔开始。
            </p>
            <ul v-else class="list">
              <li v-for="item in visible" :key="item.id">
                <strong
                  ><component
                    :is="categories.find((c) => c.kind === item.kind)!.icon"
                    aria-hidden="true"
                  />{{ labels[item.kind] }}</strong
                >
                <p>{{ details(item) }}</p>
                <time>{{ item.occurredAt.replace("T", " ") }}</time
                ><small>演示 · 未保存到服务器</small>
              </li>
            </ul>
            <RouterLink class="back-link" to="/home">返回首页</RouterLink>
          </section>
        </section>
        <footer class="site-footer">
          <p>
            <PhInfo aria-hidden="true" />{{
              client.mode === "mock"
                ? "演示体验，请勿输入真实健康资料。"
                : "本地开发认证；记录仍为演示。"
            }}
          </p>
          <span>记录仅保留本次体验，未保存到服务器。</span>
        </footer>
        <details class="session-settings">
          <summary>会话设置</summary>
          <p>当前主体：{{ client.context?.accountId }}</p>
          <label
            >连接方式<select
              :value="client.mode"
              :disabled="busy"
              @change="mode"
            >
              <option value="mock">演示体验</option>
              <option value="live-auth">本地开发认证</option>
            </select></label
          ><button class="text-button" :disabled="busy" @click="refresh">
            {{ busy ? "正在更新…" : "更新会话状态" }}
          </button>
        </details>
        <nav class="mobile-nav" aria-label="小屏导航">
          <RouterLink to="/home"><PhHouse aria-hidden="true" />首页</RouterLink
          ><RouterLink to="/records"
            ><PhClipboardText aria-hidden="true" />手动记录</RouterLink
          >
        </nav>
      </template>
    </main>
  </div>
</template>
