# GYM Web 基础闭环

在本目录运行（Node 22.23.2 / npm 10.9.8 已验证）：

```sh
npm ci
npm run dev
npm run typecheck
npm test
npm run build
npm run test:e2e
```

打开 Vite 输出的 127.0.0.1 地址。默认演示模式不需要密码，切换演示主体需退出后选择。首页和手动记录使用 hash 路由，未登录直达受保护路径会返回登录。表单输入在页面内导航时保留；退出、主体/模式变化或刷新清除全部记录与草稿。时间按本机本地时间显示；正有限数值、名称1–80字、分量1–120字为输入约束，不是医疗范围。

认证 Token、资料及记录仅内存，无 localStorage/sessionStorage/IndexedDB/Service Worker。live-auth 只对 /v1/sessions、/v1/sessions/refresh、/v1/sessions/{id}、/v1/me/contexts 请求。Vite 开发代理固定到 http://127.0.0.1:8080；这是开发路由目标，不表示服务已启动。配置若与后端实际端口不符，由协调者确认后调整本目录代理，不修改后端 CORS。生产构建没有开发代理，部署路由与HTTPS尚未验收。

live-auth 使用后端已配置的开发账号；没有默认密码或注册。后端失败不会自动切演示。记录在两种连接方式下都仅是明确标识的内存演示，不调用健康API。退出立即清客户端；服务器撤销失败会单独提示。仅展示后端 accountId，不推断员工、门店或付费能力；这些入口不在本批。

测试使用随机生成的临时证明/Token替身，不记录真实资料。浏览器测试配置使用当前 Windows 已有 Chrome 路径，可在其他环境显式调整 playwright.config.ts 中 executablePath；未安装系统浏览器。测试截图写入本任务交付 evidence/，使用的都是演示数据。真实认证联调、跨端、读屏及生产部署仍待独立验收。

依赖选择与证据见 docs/deliveries/TASK-WEB001/report.md。不要把本批视为整个健康记录、权限或持续登录体验已完成。
