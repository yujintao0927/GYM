# TASK-MINI001 完整Prompt：原生TS小程序基础用户闭环编码

项目D:/GYM，origin https://github.com/yujintao0927/GYM.git。读取实际AGENTS.md（若存在）与用户全局Git规则；修改前检查status/分支/远程/历史。独立工作区、codex/任务分支，先回报实际起点/路径，不在共享D:/GYM切分支，不覆盖用户改动；同任务已领取则先去重，不声称检索全部聊天。

用户DEC-021已认可结构并批准Web Vue3/TypeScript/Vite、小程序原生TypeScript及登录/首页/手动记录基础前端。固定输入UX906fc7a、QA5df227b、已验收骨架374a4e9、003D2ef475e；实际从含本轮ACC-007/008及计划Prompt的main建立并记录SHA。读取docs/requirements.md、coordination的README/tasks/sessions/decisions/requirements-trace/questions-risks/acceptance/log，UX全部页面/旅程/九图/切片、QA策略/用例/门槛、plans/TASK-003B.md、ui-qa-alignment.md、frontend-foundation-contract.md及plans/PAR-003.md。共同契约v1是本任务锁定接口/模式边界，发现变化先报协调者，不各自发明API。

本任务是实际前端编码，不再只交设计文档；一个任务完整交付三个页面的认证/导航/记录/状态/测试及说明，不另派单组件。只有有限基础闭环，不是整个Web/小程序。不要机械像素复刻桌面线框到手机；品牌和最终视觉未批准，以清晰中性功能界面供评审，技术/评审注释不放进业务主流程。

默认mock显著提示演示且不输入真实资料；live-auth仅四个003B路由，未完成后端不假称联调。记录仍内存mock并单独标未保存服务器，无静默降级；不实现AI/收费/定位/员工/注销/真实健康持久化。Token只内存，旧主体/模式切换清理；单飞刷新，失败回登录，不无限循环。凭据/Token/个人资料不入源码/日志/参数示例或持久缓存。未知外部配置不猜不采购。

backend由003B独占；协调者独占总台账与共同契约，其他交付只读；两个前端不改对方目录或根workspace/lock/.gitignore。工具版本由你以官方资料核实兼容后精确锁包及lockfile，允许常规依赖选择，不盲目用latest重大版本，不改系统Node或装系统软件。测试/构建/实际交互检查见PAR-003计划；环境失败保留未完成状态，不能跳过验证称通过。

git diff --check、实际范围/敏感扫描通过后仅暂存自身任务文件，整体commit/push，首次设上游，不强推/不改写历史/不合并main；失败保留提交并如实报告。report.md包含任务/输入起点/工作区分支/版本来源/文件摘要/命令结果及实际界面证据/接口数据变更/未完成及模拟预留真实边界/风险及下游依赖，不能自授验收。无自动回传，请用户带最终报告、SHA与路径回COORD-01聊天，不声称自动通知。

负责人拟EXEC-MINI001，分支codex/task-mini001-user-foundation；修改只mini/**及docs/deliveries/TASK-MINI001/**，不改根配置。

采用原生TypeScript/WXML/WXSS，login/home/records三页完整交互，不生成H5套壳或跨端框架。开发工具支持条件经实际核查，不伪造AppID/服务器域名/真实微信身份；没有配置不影响先写原生代码和可执行逻辑测试，但开发工具/真机证据必须分开，不能假称环境已可用。

验收：锁定版本、类型/编译和适配/状态逻辑测试通过，触控录入/错误/空/加载与认证失败流程完整；有工具时实际导入操作并给截图，无法启动明确BLOCKED_ENV和剩余项，tsc或wx替身测试不能代替运行截图/真机。记录未持久化及mock状态醒目标明，live-auth失败无自动降级。按PAR-003整体交付，不只交项目模板。
