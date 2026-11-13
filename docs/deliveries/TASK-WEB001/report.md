# TASK-WEB001 / EXEC-WEB001 交付报告

日期：2026-11-13（Asia/Shanghai）。状态：代码与有限模拟闭环验证已交付，等待COORD-01组织验收；不自授验收。

## 起点与输入

共享仓库 D:/GYM 保持main未切分支，初始工作区干净；origin https://github.com/yujintao0927/GYM.git。fetch成功，main与origin/main均为2b48c1e272ca3ab30d1b626b9aabb5b20a973f44，包含ACC-007/008、DEC-021与计划Prompt。本地分支、worktree及协调tasks/sessions未发现WEB001已领取，不代表检索了全部聊天。未发现项目及D:/父目录AGENTS.md，采用用户消息中的全局规则。

工作区：C:/Users/28795/.codex/visualizations/2026/11/13/01a1d4d3-cd1a-7ad3-89d8-7a5a613de6f5/task-web001

分支：codex/task-web001-user-foundation。固定输入UX906fc7a、QA5df227b、003A374a4e9、003D2ef475e；已读取需求、协调指定台账、UX页面/旅程/切片/九图结构、QA策略/用例/门槛、TASK-003B计划、ui-qa-alignment与PAR-003、共同契约v1。共同契约未改动。

## 实现与文件

web/package.json和独立package-lock锁定Vue3.5.27、Vue Router4.6.4、Vite7.3.1、plugin-vue6.0.4、TypeScript5.9.3、vue-tsc3.1.5、Vitest3.2.4、Playwright1.58.2、@types/node22.19.11，均精确版本。没有根workspace/lock/ignore改动，没有backend或mini改动。web/.gitignore忽略私有环境、产物和测试临时文件。

src/auth.ts负责四路由、单飞刷新、内存Token与epoch；state.ts负责当前页面内存；records.ts负责有限DTO/校验/列表；App.vue实现PAGE-01/02/03，main.ts保护hash路由；style.css实现桌面双栏与小屏单栏。测试位于tests/，运行说明为web/README.md。没有新增业务API/服务端数据模式；记录DTO仍客户端模拟，不发布C09。

接口按共同契约使用credential.kind=LOCAL_PASSWORD、entry=user-web、clientKind=WEB、Idempotency-Key与Bearer；初始generation必须0，刷新使用并更新返回代数。请求超时10秒、同次401共用一次刷新，重试最多一次，失败不降级mock。退出清本地，服务端撤销失败说明未确认。无角色/门店授予，不展示虚构商家或会员入口。

## 版本依据

2026-11-13核对官方[Vite指南](https://vite.dev/guide/)、[Vue快速开始](https://vuejs.org/guide/quick-start.html)、[Vitest指南](https://vitest.dev/guide/)。没有盲用当前latest主版本，选择Vite7/Vitest3与TS5稳定组合，实际安装及类型/构建验证兼容。

另以npm官方registry元数据核对：vite@7.3.1 engines为^20.19.0或>=22.12.0；plugin-vue@6.0.4支持Vue^3.2.25和Vite5/6/7/8预发布；vue-tsc@3.1.5要求TS>=5。来源可复查：[Vite包元数据](https://registry.npmjs.org/vite/7.3.1)、[plugin-vue](https://registry.npmjs.org/@vitejs/plugin-vue/6.0.4)、[vue-tsc](https://registry.npmjs.org/vue-tsc/3.1.5)。实际Node22.23.2/npm10.9.8，Windows PowerShell；未改系统Node或安装系统软件。

## 执行证据与结果

| 检查 | 实际结果 |
| --- | --- |
| npm ci --no-audit --no-fund（web目录） | 成功，90依赖包 |
| npm run typecheck | vue-tsc退出0 |
| npm test | 最终12/12通过 |
| npm run build | Vite生产构建退出0 |
| npm run test:e2e | 最终4/4通过；真实已安装Chrome156.0.8078.82，headless |
| 1440×1000桌面与390×844小屏 | 登录→三类新增→列表/筛选→退出，主体切换/刷新清空、live503无mock回退 |
| 320×900 / 768×900 | 无横向溢出断言通过，截图已打开核查 |
| 自动边界 | 并发401只刷一次、刷新409清会话、刷新后仍401不循环、generation缺失拒绝、模式切换拒旧响应、退出失败清本地、503重试、数值/空字段校验、记录冷启动空、快速提交单条、导航草稿保留 |

截图均实际打开检查，演示信息、表单标签及字段错误可见；桌面双栏与小屏单栏无裁切。见[evidence目录](evidence/)：[登录](evidence/desktop-login.png)、[首页](evidence/desktop-home.png)、[三类记录](evidence/desktop-records.png)、[小屏记录](evidence/mobile-records.png)、[小屏失败](evidence/mobile-live-failure.png)、[320](evidence/records-320.png)、[768](evidence/records-768.png)。输入为合成示例，密码及Token无截图/日志。

首失败保留说明：普通沙箱执行两次因CreateProcessSecurityEnvironment 0x80070020失败，Node REPL也无法启动，改为受审查执行后成功。初次npm install --prefix web解析到根缺少package.json，安装失败，随后改web工作目录直接安装成功。初始测试虽先编写，未在实现前观察预期RED（依赖失败），这是测试先行流程偏差，不能声称严格完成全套RED→GREEN。最终12项测试已实际执行。审查发现隐藏类型字段残留后，新增回归测试先观察1失败/11通过，再修复按kind丢弃无关字段，最终全绿。Playwright有NO_COLOR/FORCE_COLOR环境警告，不影响退出码，没有隐藏环境skip。

独立只读审查未发现认证/隔离阻断问题；字段残留已修复。保留非阻断小项：登录证明401沿用“登录已失效”提示，未细分账号/密码错误（仍拒绝登录，不泄露账号存在性）。当前未做独立读屏、全浏览器矩阵或人工键盘全流程，自动检查了错误焦点/标签和快速提交，不冒充完整无障碍认证。

## 模拟、预留、待验与风险

默认mock显著提示请勿输入真实资料；live-auth仍明确记录为演示。Token/健康输入都只内存，无持久缓存。退出/冷启动/切主体/模式清除记录和草稿；记录成功只表示本机演示列表添加。正有限数值及名称80/分量120字是实现输入限制，不是医疗建议。

真实003B联调：待验收。本任务未连接真实后端或真实账号；受控fetch与浏览器503替身只证明消费者行为，不证明服务端。开发代理到127.0.0.1:8080，需与实际后端配置独立核对；无CORS修改。未实现真实健康持久化、AI/收费/定位/员工/注销/完整上下文权限/持续登录/跨端/生产部署；这些不计本批完成。未来真实接口联调需003B验收及隔离账号，不需当前采购或猜测外部配置。

代码仅请求四个认证路由，无未实现健康写入。暂不把opaque Token推断为JWT，也不推断权限。服务端返回上下文变化为另一主体会要求重登；未来可用门店/角色展示需要协调者给出实际字段契约。

## 回传与Git

仅整体提交web/**与docs/deliveries/TASK-WEB001/**，不合并main。提交SHA及push结果由最终回复提供（避免报告自引用SHA）。请用户将本报告、最终SHA与工作区路径带回COORD-01聊天；本会话没有自动回传或通知。

Git补充：首提交2da4b8d已推送；暂存检查发现records.ts末尾多空行，后续独立格式修正提交清理，不改写历史。最终diff检查、敏感扫描与范围检查均通过，分支设置上游成功；最终SHA见回复。
