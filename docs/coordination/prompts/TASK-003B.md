# TASK-003B 完整执行Prompt：本地账号会话闭环编码

你是智慧健身系统执行者，拟EXEC-003B。项目D:/GYM，origin https://github.com/yujintao0927/GYM.git。先读实际适用AGENTS.md及用户全局规则，核对status/分支/origin/历史。独立工作区、codex/task-003b-local-sessions，先回报实际路径和起点；不在共享D:/GYM切分支，不覆盖用户改动。若发现同任务已领取先报告，不重复开工。

输入：业务5f5e091及后续DEC，TASK-002验收稿c781088、身份草案2ef475e、003A代码374a4e9已验收集成，main此前10a994a。实际分支必须从含本轮DEC-018–020、local-development.md及本Prompt/计划的最新main建立，记录SHA，不从旧快照丢失已集成改动。读取docs/requirements.md、docs/coordination/{README.md,tasks.md,sessions.md,decisions.md,requirements-trace.md,questions-risks.md,acceptance.md,local-development.md,log.md}、TASK-003D全部设计、TASK-002 C01/D25、backend现有代码，及完整计划docs/coordination/plans/TASK-003B.md。遇到冲突或接口范围变更先报告。

用户要求任务不拆太小，本任务一次交付CLI本地逐账号配置、登录/刷新/退出/本人上下文、Flyway初始IAM迁移、真实MySQL持久化/并发测试和运行说明。不是设计任务，请完成实际代码；不能只交文档或只做一个接口。规则及四路由精确契约按计划和DEC-020；登录来源仅local-dev，账号CLI不赋员工/平台角色，无公共注册和共享默认密码。

当前MySQL是Docker mysql-dev、主机localhost:3306，实际SELECT VERSION为26.7.0；用户授权创建的gym_dev已核实空库，utf8mb4/utf8mb4_unicode_ci。凭据仅读适用全局规则，经当前进程/忽略配置使用，禁止复制到Prompt、报告、源码、参数日志或tracked YAML。只可在gym_dev新增本任务IAM表；隔离测试gym_dev_test_003b不存在可创建，已有未知数据先报告；禁止DROP库/覆盖未知表/Flyway clean/生产操作。V1初始IAM迁移由你唯一拥有，记录实际兼容性，不能假定MySQL8。数据库版本/驱动不兼容先报告，不擅改服务。

修改仅backend/**、docs/deliveries/TASK-003B/**，根.gitignore仅任务私有配置/缓存忽略。backend构建与安全配置本轮你唯一维护；UX001、QA001正在执行各自文档，互不等待，不改其范围或目录。共享迁移/业务接口变更先报协调者。协调总台账、需求、旧交付只读。

排除真实微信/短信、公共注册/找回/合并、员工及平台授予、健康/账号注销、会员收费退款、前端；OD-02/03/04等未批分支不实现。CLOSED/epoch拒绝通过隔离测试夹具验证，不冒充已实现注销。Bearer开发传输不等于生产Web Cookie方案，保留其他路径安全默认和health边界。

按计划先有效失败测试再实现并验证：JDK21/Boot4.1.2/Maven3.10.0保持，真实MySQL而非H2替代；必要依赖按固定BOM解析并报告版本，未管理者以官方兼容资料锁显式版本，不用LATEST/SNAPSHOT。环境网络阻塞如实报告，不跳过测试。保留旧六项骨架回归、默认无DB启动，增加profile与数据库/并发验证、新进程业务闭环及重启持久化。记录失败/修正/成功事实，无秘密输出。

修改完成检查实际diff、范围、敏感信息、git diff --check，按完整闭环git add/commit/push，首次设上游，不强推、不改写历史、不合并main；推送失败保留提交并说明原因。report.md至少含任务/输入起点/工作区分支/提交推送/文件摘要/接口及迁移数据变更/命令退出结果/实际业务与未完成项/风险/模拟预留真实边界/下游依赖。完成只进入待验收，不能自授通过或解锁其他模块。

没有自动派发/回传工具，请用户将最终报告、完整SHA、材料路径带回COORD-01聊天；不要声称已经自动通知。

接口补充（PAR-003对齐）：首次登录201 data包含generation=0，首次刷新请求用0，刷新成功返回递增generation；Token不透明，客户端不能猜代数或解析Token。其余四路由/安全规则和任务范围不变；若已在原聊天执行，请先读取最新协调计划确认该字段，不重复新建任务。
