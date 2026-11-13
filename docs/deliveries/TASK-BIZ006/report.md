# TASK-BIZ006 实施回报

状态：M层候选交付，待协调者独立验收；存在下列未完加固项，不声明模块M全部通过或业务全量完成。

实际基线 b3ce453e92cc378a25dac27f7d1591eb01274baf（含DEC-027、PAR-BIZ、冻结端口与本Prompt）；分支 codex/task-biz006-ranking-core。
worktree：C:/Users/28795/.codex/visualizations/2026/11/13/01a1d577-1352-77d2-a543-184d98e5fb40/task-biz006。
源提交完整SHA及推送状态以本目录 git-receipt.txt 为准（随后独立文档提交记录源码SHA，避免自引用）。

修改仅 modules/ranking-core/**、docs/deliveries/TASK-BIZ006/**；D:/GYM保持main，未跟踪AGENTS.md及WEB001材料未动。没有前端/共享契约/其他模块修改。

已实现：独立POM/Java21库；二维码及人工签到签退、更正审计、在场唯一约束、稳定记录分页；幂等原始回执；事务权益端口；模拟设备绑定失败闭合、原始事件与有效日双层去重；到店版本投影；默认关闭、昵称、区域过滤、显式日/月窗口和累计/并列算法；地区授权端口与fixture月限；申诉及审核轨迹；本库V8迁移、守卫和Outbox/持久Inbox键。

证据在 verification.md、verify.txt、red.txt、red-expansion.txt、contract.md、integration.md；可重放verify.ps1。首次真实MySQL验证8项通过，随后并发/投影/申诉扩展；最终数量/退出码见verify.txt。MySQL26.7/Flyway警告未屏蔽。

前置材料核对：requirements、coordination README/decisions/tasks/sessions/questions-risks/requirements-trace/local-development相关范围，business-module-ports-v1、PAR-BIZ、TASK-002 domain/contracts/states/report、TASK-QA001 strategy/risk-cases、backend POM与MySQL守卫/事务实现。没有盲抄003B异常提交语义；业务Failure回滚。

未完成/限制：真实IAM/merchant/trade/设备/定位提供方与同库账号栅栏尚未联调；当前right接口是明确单来源选择，Account epoch/Store status及多来源映射需统一适配；完整异常成绩处置政策未批只留审核记录；地区首次计月次数仅fixture支持，运行时PENDING；动态榜跨请求offset分页不保证快照连续；完整跨商家/未知对象/依赖失败矩阵及Outbox崩溃投递测试未穷尽。本次不能据测试数量认定全部M验收条件满足。I/U/L均未完成。

未批政策不发布20分钟/10人或任意月周期默认；无真实支付、AI、短信、硬件或精确定位。批准图/范围：无，本任务不含前端。

请用户将本report链接、源码完整SHA、分支和worktree带回COORD-01；未自动回传、未合并main、未自授验收。

最终验证：20项测试，0失败/0错误/0跳过，clean verify退出0；依赖解析成功（单个远端metadata 401警告保留），Boot4.1.2、spring-jdbc7.0.10、MySQL Connector9.7.0、Flyway12.4.0，实际JDK22以release21编译。远端main复核仍为实际起点，未存在本任务远端分支。
