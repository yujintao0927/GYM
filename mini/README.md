# GYM 原生小程序基础闭环

本目录为 TASK-MINI001，原生 TypeScript/WXML/WXSS，无 H5 或跨端运行时。默认 mock。登录、首页、三类记录和列表为本次范围。

## 构建和测试

使用现有 Node 22.23.2 / npm 10.9.8，不安装系统软件。在 mini 目录执行：

```
npm ci
npm run typecheck
npm test
npm run build
```

TypeScript 5.9.3 和微信官方 miniprogram-api-typings 4.0.8 精确锁定于 package-lock.json。TS 编译输出 ES2017 CommonJS 到 dist，脚本复制原生页面资源；不依赖开发工具内置 TS 插件版本。dist 不纳入 Git。npm test 的 wx/Page 替身只验证逻辑，不渲染 WXML。

## 导入与操作（待实际环境验证）

先构建，再用微信开发者工具导入本 mini 目录；project.config.json 将 miniprogramRoot 指向 dist/。配置刻意不含 AppID，不伪造测试账号。使用工具实际支持的游客能力，或由授权开发者在未跟踪的 project.private.config.json 中填写自己的配置。若无法导入，记录 BLOCKED_ENV，不能改称已运行。

演示路径：进入演示 → 首页 → 新增/查看记录 → 切体重/饮食/训练 → 无效输入观察字段提示 → 填虚构输入及本机日期时间 → 加入演示记录 → 列表 → 返回首页 → 退出。返回前未保存输入保存在本次内存草稿，切模式/主体/退出清除。记录表单数字上限是防误输入的工程限制，不是医疗范围。

应补截图：登录、首页、记录空态、三类录入成功、字段错误、loading/禁重复、认证失败、退出失败说明；至少小屏窄宽与真机键盘/日期选择/滚动。当前没有这些运行截图，详见交付报告。

## 本地开发认证（仍待003B独立联调）

默认地址为空，选择本地开发账号登录将明确失败，不回退演示。只允许显式配置 http 的 IPv4 loopback 和端口，未预填后端地址或端口。配置函数是 services/runtime.ts 的 configureLocalAuth(url)，不会自动读取私有 JSON 或环境变量。

授权开发者可在开发工具调试控制台调用 `require('./services/runtime.js').configureLocalAuth(localBackendUrl)`，其中变量须由开发者在本机设为003B实际地址；或在本地 app.ts 中显式导入并调用该函数后构建，地址变更不应提交。此控制台调用方式尚待开发工具验证。仅开发工具允许本地网络且按工具规则关闭本地地址检查时探测 loopback；不得将该方式当作真机合法域名或生产方案。工具配置及接口不可用时保持失败。冷启动需重新配置和登录。

四路由为 POST /v1/sessions、POST /v1/sessions/refresh、GET /v1/me/contexts、DELETE /v1/sessions/{id}。entry=user-mini，clientKind=MINI_PROGRAM 为平台标识；v1计划未枚举 clientKind，需协调者与003B确认此值，未宣称联调成功。凭据嵌套在 credential 内，初次 generation=0，刷新采用返回代数，Bearer 仅在内存。

演示登录无需任何密码。真实认证不代表健康接口已实现；记录始终醒目标明未保存服务器。无 wxStorage、健康上传、员工权限、AI、收费、定位、微信真实身份认证或注销实现。退出立即清本机数据，远程退出失败显示未确认撤销说明。
