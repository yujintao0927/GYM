# TASK-NIGHT002 完整Prompt：独立认证回归与安全工具包

项目D:/GYM，origin https://github.com/yujintao0927/GYM.git。先读实际AGENTS及最新用户全局Git规则，检查status/分支/远程/历史；必须独立工作区和codex分支，先回报路径/起点，不在共享D:/GYM切分支，不碰用户AGENTS或运行中WEB001资料。发现同任务或backend写者冲突先报告，不能声称检索了所有聊天。

原TASK-006A已由用户取消且从未派发，不执行旧任务单；当前是新编号较大任务。实际从含本轮取消登记、DEC-026、plans/NIGHT-BATCH.md和本Prompt的main建立并记SHA；固定003B源码e20e499已验收。读取requirements、coordination的README/tasks/sessions/decisions/requirements-trace/questions-risks/acceptance/local-development/log、完整NIGHT-BATCH计划，当前backend及相关QA/契约；范围冲突先记录报告，不修改总台账或别人文件。

用户睡前希望较大独立任务，但不保证/不凑满八小时：按计划持续实现、诊断、修复和必要验证，有意义工作完成即提交结束，禁止空转sleep或无意义重复测试拉时长。没有人即时回复时，把未批规则/缺配置分支记阻塞，继续同任务独立部分，不猜AI/支付/隐私/视觉政策、不采购/改系统、不擅自接下一任务。无自动唤醒或回传，完成需用户醒来带报告回COORD-01。

Docker mysql-dev localhost:3306，实际版本26.7.0；固定Java21/Boot4.1.2/Maven3.10.0/现有JDBC和Flyway，警告保留，不改服务或降低验证。凭据仅读适用全局规则进入当前环境/私有忽略文件，不放源码、Prompt、命令参数或报告。不得DROP/清库/覆盖未知对象/执行Flyway clean，严禁生产操作。各任务数据库隔离按计划，不误用旧探测脚本的gym_dev。

Git按完整可理解可验证单元及时commit/push，不每小改机械提交、不长期无保存；只add任务范围，不强推、不改写用户历史、不合并main。阶段/最终提交需验证且记录真实结果，失败保留提交和原因。

report.md包含任务、起点/分支/工作区/最终SHA及推送、范围/文件/功能、接口/数据库变更、命令退出及实际证据、问题/修正、未完成/模拟/预留/真实边界、风险/依赖和路径；完成只待验收，不能自授通过。请用户将最终报告/SHA/路径带回COORD-01，不声称已自动通知或睡后自动续派。

拟EXEC-NIGHT002，分支codex/task-night002-identity-regression。仅修改quality/identity-regression/**及docs/deliveries/TASK-NIGHT002/**；backend跟踪文件一律只读，可正常构建产生其现有忽略target/.tools。受测backend必须固定003B e20e499，不拉取NIGHT001代码或修改V1/生产接口。

一次完成可重复运行的外部HTTP/并发/重启测试工具、至少40种独立条件覆盖矩阵、合成夹具/脱敏结果摘要及问题复现报告。不是只重跑已有28测试，也不是文档审查而无新工具。具体测试分组和有界并发见NIGHT-BATCH计划，不为延长时间压测八小时或反复重跑已证实同场景。

只在gym_dev_test_003b启动自己的V1服务；禁止连接gym_dev或night001测试库。启动显式schema与URL，创建随机独立账号/HMAC，禁止调用固定gym_dev的旧verify-local-process脚本。来源/账号限流夹具必须自己的key与数据；不清库，不停用户服务/Docker。源码中的HMAC、Token、返回秘密和凭据不可输出到交付；代码/结果自动检查脱敏。

覆盖登录/刷新/退出/上下文、两clientKind、generation0、结构畸形/未知字段、回执/重放/并发、跨主体、Cookie/伪role/入口/XFF、限流/边界/默认profile及重启。测试可用受控隔离夹具驱动关闭/过期状态，不增调试接口。明确工具正向与负例能按预期失败，缺陷只报告不修backend；保留关键证据，不能宣称全面安全。

若发现严重问题，先保存脱敏复现/报告，再完成同任务无关可继续部分；不等待睡眠用户立即回复，不擅自扩权限或改另一个任务。最终提交工具与报告供独立验收。
