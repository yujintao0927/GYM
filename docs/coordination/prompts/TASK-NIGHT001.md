# TASK-NIGHT001 完整Prompt：健康基础后端大包

项目D:/GYM，origin https://github.com/yujintao0927/GYM.git。先读实际AGENTS及最新用户全局Git规则，检查status/分支/远程/历史；必须独立工作区和codex分支，先回报路径/起点，不在共享D:/GYM切分支，不碰用户AGENTS或运行中WEB001资料。发现同任务或backend写者冲突先报告，不能声称检索了所有聊天。

原TASK-006A已由用户取消且从未派发，不执行旧任务单；当前是新编号较大任务。实际从含本轮取消登记、DEC-026、plans/NIGHT-BATCH.md和本Prompt的main建立并记SHA；固定003B源码e20e499已验收。读取requirements、coordination的README/tasks/sessions/decisions/requirements-trace/questions-risks/acceptance/local-development/log、完整NIGHT-BATCH计划，当前backend及相关QA/契约；范围冲突先记录报告，不修改总台账或别人文件。

用户睡前希望较大独立任务，但不保证/不凑满八小时：按计划持续实现、诊断、修复和必要验证，有意义工作完成即提交结束，禁止空转sleep或无意义重复测试拉时长。没有人即时回复时，把未批规则/缺配置分支记阻塞，继续同任务独立部分，不猜AI/支付/隐私/视觉政策、不采购/改系统、不擅自接下一任务。无自动唤醒或回传，完成需用户醒来带报告回COORD-01。

Docker mysql-dev localhost:3306，实际版本26.7.0；固定Java21/Boot4.1.2/Maven3.10.0/现有JDBC和Flyway，警告保留，不改服务或降低验证。凭据仅读适用全局规则进入当前环境/私有忽略文件，不放源码、Prompt、命令参数或报告。不得DROP/清库/覆盖未知对象/执行Flyway clean，严禁生产操作。各任务数据库隔离按计划，不误用旧探测脚本的gym_dev。

Git按完整可理解可验证单元及时commit/push，不每小改机械提交、不长期无保存；只add任务范围，不强推、不改写用户历史、不合并main。阶段/最终提交需验证且记录真实结果，失败保留提交和原因。

report.md包含任务、起点/分支/工作区/最终SHA及推送、范围/文件/功能、接口/数据库变更、命令退出及实际证据、问题/修正、未完成/模拟/预留/真实边界、风险/依赖和路径；完成只待验收，不能自授通过。请用户将最终报告/SHA/路径带回COORD-01，不声称已自动通知或睡后自动续派。

拟EXEC-NIGHT001，分支codex/task-night001-health-foundation。修改仅backend/**及docs/deliveries/TASK-NIGHT001/**；你唯一拥有本轮backend与V2 HLT迁移。NIGHT002只读backend并在quality写工具，Web原范围继续。不要改web/mini/根配置/其他交付/协调文件。

一次完成三类手动记录CRUD/分页过滤、身高体脂围度测量历史CRUD、本人体重目标管理、V2/精确权限事务、OpenAPI或等价schema、运行说明、全量旧回归+新单元/MySQL并发/故障/重启测试。三类记录沿用health-records-contract.md作为部分基线，新增接口字段精确按NIGHT-BATCH计划；不是只把006A改名，也不是只交设计。体测/目标是原需求已确认的手动数据，不生成AI或医学建议。

全部自动测试使用gym_dev_test_night001（不存在可创建），原28测试运行配置也改为该专用库，不碰NIGHT002占用的gym_dev_test_003b。开发新进程可对gym_dev保留V1增量V2；V1校验和不改。schema/table白名单只加明确对象，不放宽任意库。测试只操作自己的随机合成夹具，不清未知数据。

验收关键：所有本人/非本人/匿名/过期/撤销/CAS/幂等/游标/删除重放/目标竞争分支；V1+V2精确迁移与重复0；真实MySQL和进程重启后记录仍在；默认profile不开放新业务。Health写错误全回滚，不能误用IAM“拒绝仍提交防护计数”的事务包装。日志/回执无健康正文泄漏。未完成关键验证必须如实标出。

排除收费/会员/排行榜/教练健康授权/注销/营养推算/AI/图片识别/前端。用户离线不等于批准这些分支。按计划完成实际代码和证据，最终交付一个较大可验收模块。
