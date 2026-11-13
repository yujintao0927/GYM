> 已取消：用户2026-11-13明确取消TASK-006A，且确认从未派发。仅保留历史，不得据此开工。夜间新包见TASK-NIGHT001；范围更大，并非旧任务恢复。

# TASK-006A 完整执行Prompt：三类本人记录后端闭环

项目D:/GYM，origin https://github.com/yujintao0927/GYM.git。你是拟EXEC-006A，独立工作区和codex/task-006a-manual-records分支；先读实际适用AGENTS.md及最新用户规则，核对status/分支/远程/历史，回报实际路径及起点。不在共享目录切分支，不覆盖用户未提交AGENTS.md或正在产生的WEB001材料，不假称已列举全部聊天。发现同任务或共享backend占用先报告。

最新Git规则：按完整、可理解、可验证单元选择性add/commit/push，不机械每小改提交；重要里程碑/交接及时保存，只提交自己范围，正常推送已授权，不强推或覆盖历史。

输入：已验收003B e20e499及集成08143f4，003A374a4e9，设计c781088，QA5df227b；实际从含本轮DEC-025/健康记录契约和计划Prompt的main建立并记录SHA。读取docs/requirements.md、coordination的README/tasks/sessions/decisions/requirements-trace/questions-risks/acceptance/local-development/log、health-records-contract.md、plans/TASK-006A.md，现有backend认证/数据库/迁移/28测试、QA-005/018/028相关门槛。其他文档及旧交付只读，不重复等待已通过身份基础。

这是实际后端编码任务，一次完成三类本人手动记录POST/分页列表/详情/PATCH/DELETE、持久化/V2迁移、权限/并发/回滚/分页/重启测试和运行说明，不只做设计或一个接口。字段/错误/游标/幂等/认证锁序按C09-MANUAL v1；冲突或新契约变化先报告，不各自发明。

范围仅backend/**及docs/deliveries/TASK-006A/**。你唯一负责本轮backend与V2 HLT迁移，可最小扩展身份主体解析及已知schema/table守卫，不重写V1/既有IAM表，不删除或削弱旧28测试。WEB001仍运行且改其页面，MINI工具安装用户暂缓，两者不阻塞你；不要修改其代码/资料或自动切记录真实存储。

Docker mysql-dev映射localhost:3306、实查版本26.7.0；开发gym_dev已有V1 IAM，测试gym_dev_test_003b保留，新隔离gym_dev_test_006a不存在可创建，初装或升级先查对象归属。只新增本任务HLT表及保留V1的增量V2；未知数据先报告，禁止DROP数据库、清库、Flyway clean或改服务版本。凭据仅读适用全局规则，置进程/忽略私有配置，不入命令参数、报告、源码、日志。保留Flyway高版本警告，不以关闭验证绕过兼容。

排除UI/视觉、AI/识别、营养估算/趋势、付费/排行榜、教练共享、注销/财务及未批政策。只local-dev本人数据，平台/员工入口无越权豁免。单条记录纠错删除按契约移除正文，不保留正文审计副本，不自动宣称商用隐私保留政策完成。

按计划有效失败测试→实现→全量MySQL集成→新进程CRUD/重启/跨账号/默认profile探测。没有真实数据库、并发和持久化证据不能交为完成；H2/mock/截图不能替代。沿用固定技术版本，必要工具/库先官方核实，不新增服务、不改系统安装。缺环境记录真实阻塞。

修改完成检查diff、范围、敏感信息和git diff --check，按完整闭环阶段commit/push，首次设上游；不强推、不改写用户历史、不合并main。report.md包含任务/输入起点/工作区分支/提交推送/文件与接口数据变更/迁移版本校验/命令退出及实际证据/未完成风险和模拟预留真实边界/后续依赖。交付仅待验收，不自授通过或解锁前端/其他业务。

无自动回传工具，请用户将最终报告、完整SHA和路径带回COORD-01聊天，别声称已自动通知。
