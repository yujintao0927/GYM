# TASK-NIGHT001 / EXEC-NIGHT001 交付报告

状态：待 COORD-01 验收。实现、回归与本地进程验证已交付；存在下列明确证据缺口，不能据本报告自授全部验收通过。日期 2026-11-13（Asia/Shanghai）。

## 起点、工作区与 Git

- main 起点：290e098dda56b9fd6fca1b42d75f560f3a07cd3a，包含 006A 取消登记、DEC-026、NIGHT-BATCH 和本任务 Prompt。003B 已验收源码为 e20e499。
- 工作区：C:/Users/28795/.codex/visualizations/2026/11/13/01a1d560-5e3e-72a3-a240-82ce0c273768/task-night001
- 分支：codex/task-night001-health-foundation
- origin：https://github.com/yujintao0927/GYM.git
- 实现提交：942ca2dd4e963306011949d519dcfae4408929da。最终 SHA 为包含本报告的分支 HEAD，最终聊天提供精确值；可在上述工作区执行 git rev-parse HEAD，避免报告内自引用 SHA。
- 推送状态记录在最终聊天及 progress.md 后续条目；不合并 main、不强推、不改写历史。

读取实际 D:/GYM/AGENTS.md 及 C:/Users/28795/.codex/AGENTS.md，核查 status/branch/remote/history/worktree。共享 D:/GYM 保持 main，未触碰其未跟踪 AGENTS 和 WEB001 交付。已列出的本地分支/工作区及协调登记未发现同 NIGHT001 或新的 backend 写者；没有检索所有聊天的能力，也未声称全局排除冲突。未执行已取消且未派发的 006A。

输入依据：requirements、coordination 的 README/tasks/sessions/decisions/requirements-trace/questions-risks/acceptance/local-development/log、完整 NIGHT-BATCH 与 health-records-contract，当前 backend 和 QA001 相关报告。旧历史描述以新 DEC/用户本次范围为准。协调台账、其他交付、web、mini、根配置均未修改。

## 实现范围及文件

- backend/src/main/java/com/gym/health/HealthController.java：13 个操作，只在 local-dev 注册。
- HealthService.java：校验、规范化、幂等、本人查询/CAS、游标与目标；JdbcTemplate 持久化。
- identity 最小调整：事务内只读主体接口 authenticatedHealthActor，要求已有活动事务；Health 复用权威会话解析，不另建认证。LocalSessionSecurity 允许健康控制器进入，由服务认证；SessionErrors 复用无正文错误信封；数据库白名单仅增加明确 night001 库及三张 HLT 表。
- V2__hlt_health_foundation.sql：hlt_record / hlt_receipt / hlt_weight_goal，明确外键及查询索引。V1 源文件零改动。
- 保留旧 28 测试，运行配置及 CLI 测试子进程改用 night001；新增 HealthFoundationTest 九项集成测试、HealthValidationTest 三项纯单元测试。
- backend/tools/verify-health-process.py：标准库可重放进程工具，随机合成账号/数据，私密环境注入，输出标签，无健康正文或 Token。
- backend/README.md、openapi.json、usage.md、progress.md、evidence.json：运行说明、schema、示例/前端后续 DTO 映射及过程证据。

## 接口及数据行为

/v1/me/health-records 提供 WEIGHT/DIET/TRAINING 的 POST、分页过滤 GET、detail GET、PATCH、DELETE。/v1/me/body-measurements 同五操作，payload 仅身高/体脂/腰/臀/胸围非空子集，不混入三类记录。/v1/me/weight-goal 提供 GET/PUT/DELETE，初建 expectedVersion=0，后续 CAS。目标及体测均为用户手动输入，不生成建议。

时间带时区及秒，最多毫秒，存 UTC 毫秒。小数必须字符串、最多三位，技术范围 DECIMAL(10,3)；体脂允许 0–100，其余正值。JSON 存规范化正文；体重与五类体测数值另有 STORED generated DECIMAL(10,3) 投影，目标直接 DECIMAL。仅解释技术约束，不作医学判断。

主体仅取有效 Bearer；匿名/过期/撤销/epoch 失配/关闭均拒绝，员工入口不获豁免。非本人 id 的 GET/PATCH/DELETE 返回 404。健康动作以 Account→Session→Record/Receipt 同事务锁序执行，健康预期错误和基础设施异常均全回滚；未套用 IAM“拒绝仍提交防护计数”的包装。

POST 幂等键绑定本人、类别及规范化请求 HMAC。相同请求返回当前记录，异体 409；删除移除正文及 DECIMAL 投影，回执只留 id/密钥摘要/受密钥指纹，重放 404、不恢复。响应沿用 SIMULATED 信封和 no-store；这是开发环境标记，MySQL 持久化是真实的，不称正式健康云服务。

列表在 SQL 内按本人/类别/过滤执行，occurredAt/id 升序，limit 1–100，签名游标绑定本人、类别、筛选、limit 和 asOf。asOf 不保证并发编辑下不可变快照；契约明确的是稳定键排序，未新增历史快照版本策略。

## 数据库与迁移事实

只创建并写 gym_dev_test_night001，未写 gym_dev 或 NIGHT002 的 gym_dev_test_003b。创建命令是 CREATE DATABASE IF NOT EXISTS；未 DROP、清库、Flyway clean 或删除未知数据。凭据仅读全局规则进入当前私有环境，不入源码、报告或命令参数。

实际 MySQL26.7、Java21、Boot4.1.2、Maven3.10.0、现有 JDBC/Flyway 均保留。Flyway 对 MySQL26.7 超出已验证9.4的警告未屏蔽；不是厂商认证兼容性声明。

最初 V2 曾在空专用库成功与 V1 一起迁移。随后发现 JSON 数值单独存储未满足 DECIMAL 契约，在本任务尚未发布且已知自有 HLT 表上精确 ALTER 增加六个 STORED DECIMAL 列，Flyway repair 同步了 V2 校验记录为 -1419392502。日志只显示修复 V2，V1 未改。没有删除记录、覆盖未知对象或修改其他库；一次性辅助类移到忽略 .tools，未进入交付常规测试。后续正常启动只有 migrate/validate，不 repair。

**关键证据缺口：最终修订后的 V2 未从全新空库再跑首次 Flyway 迁移。** 为遵守不清库/不扩展数据库目标，没有销毁现有库另测。已验证最终 V1/V2 校验、精确版本序列 [1,2]、重复 migrate=0、生成 DECIMAL 实值和完整业务运行；这些不能替代最终 SQL 在空库首迁移证据。COORD-01 需在另行授权的空隔离环境补验。

## 实际命令、退出及结果

私有辅助 run-tests.ps1 仅读规则注入环境，再调用固定 Maven3.10.0；辅助文件位于忽略 backend/.tools，不提交。公开重放方式为注入 README 所述私有环境后在 backend 执行 mvnw.cmd -B clean verify。

- 初始 Maven 沙箱运行失败：缓存锁写权限不足；提升后继续。首次 JDBC 连接被沙箱拒绝，提升后专用库创建退出0。未改系统或数据库服务。
- 最初测试 red 运行因私有凭据正则编码错误失败，属于配置失败，不能作为正确 TDD red 证据。
- 首轮全量：50项/2失败（测试继承导致旧测试重复且 Clock 配置错配）；改为独立测试类后33项全部通过。旧测试断言未删除或放宽。
- DECIMAL 列扩充后原无列名 INSERT 失败，专门回归确认后改为显式列名。
- 审查复现时间缺秒问题：预期422、实际201；收紧 RFC3339 结构正则后通过。没有声称全部功能经历了完美 RED→GREEN。
- clean verify 最终业务修订：退出0，37项通过；补纯单元/精确迁移断言后 verify：退出0，**40项，0失败/错误/跳过**。分组 Foundation6、IAM21、Migration1、Health集成9、Health单元3。见 evidence.json 及本地 .tools/delivery-verify.log。
- python tools/verify-health-process.py：两次退出0，第二次使用最终业务包，验证打包CLI、两个随机账号、三记录+体测、过滤分页、重启数据仍在、跨账号GET/PATCH/DELETE拒绝、CAS、删除重放、目标持久性和默认profile拒绝。日志在忽略 target/health-process.log；只停止本工具进程。
- git diff --check：退出0；源码/文档对实际本地凭据扫描通过；OpenAPI JSON可解析、内部引用可解析、5路径/13操作检查通过。未运行专业 OpenAPI lint，也未声称已有生成客户端联调。

集成测试覆盖：所有记录/体测操作、目标初建及更新竞争、幂等同体/异体/删除重放、分页游标篡改/换主体/换筛选、类别隔离、跨账号读写/匿名、撤销/过期/epoch/关闭、非法字段/数值/时间、CAS并发、同键并发、退出竞争、强制健康写先/退出先的两个 Account 锁序、预期错误与真实 SQL 异常回滚。只操作自己随机合成夹具；旧限流测试仅删除自身 HMAC 来源桶，未清未知数据。

## 独立审查、修正与边界

使用 executing-plans 要求的一次新上下文只读整分支审查。审查提出无列名 INSERT、一次性修复测试误入常规目录、schema响应/类型绑定不足、RFC3339缺秒及测试覆盖不足；上述已修正并由最终回归覆盖。没有再派第二轮审查；审查者未执行 DB 写入或完整测试，实际运行证据来自本执行会话。

仍未验证：最终 V2 空库首次迁移；epoch/关闭与健康写的强制双向竞争（已测预置失效态，退出则有强制双向竞争）；所有异常网络断连/数据库服务重启；生产性能/容灾/隐私合规。本轮重启是应用进程重启，不是 Docker/MySQL 服务重启。未做全穷举 fuzz 或吞吐 SLO。

OpenAPI 提供机器可读取接口、输入及响应 schema，但专业 lint/客户端代码生成未执行。体测与目标 PATCH不存在：按计划分别使用体测PATCH、目标PUT；未新增未授权业务。

无付费/会员/排行榜/教练健康授权/注销/营养计算/AI/识图/前端。没有新增中间件、采购或改系统。前端逐页视觉门槛不适用本后端交付：批准图及批准范围均无，未编码任何页面。健康日志与回执不含健康正文，测试进程日志为框架元信息，不提交私有运行日志。

裁定：按用户已明确的 NIGHT-BATCH 执行，不再索取设计批准；小数正文+持久化 DECIMAL 投影保留规范请求同时满足精确数值存储；游标为稳定键分页而非不可变快照。若这些解释不符合协调验收，需明确返工，不据此扩大政策授权。未解决关键证据缺口保留在本报告，不标“全部验收通过”。

请用户将本报告、最终分支 SHA 和工作区路径带回 COORD-01。未自动通知其他聊天、无自动唤醒或睡后续派，未擅自领取下一任务。
