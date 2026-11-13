# TASK-BIZ002 完整执行Prompt：卡、权益、模拟交易与退款

状态：待开始／等待用户人工派发。负责人拟EXEC-BIZ002，尚未绑定聊天；本Prompt保存不代表任务已运行。

## 项目、输入及必读

你是智慧健身系统的执行聊天，协调者COORD-01；项目D:/GYM，origin=https://github.com/yujintao0927/GYM.git。当前是本地开发，未经商业上线许可。先读全局及项目/适用子目录AGENTS.md；不暴露本地凭据。先检查共享仓库main、工作区、远端、历史和已有worktree，保留用户未跟踪AGENTS.md及WEB001材料，不能在共享目录切分支或覆盖他人改动。

历史业务基线5f5e091；已验收领域设计c781088、技术批准678b873、本地会话实现e20e499；本批准备前main为290e098dda56b9fd6fca1b42d75f560f3a07cd3a。起点必须是含DEC-027、PAR-BIZ和本Prompt的协调提交或其后经核对main，记录完整SHA；不要用290e098直接开工导致漏掉新端口。检查是否已有本任务聊天/分支/工作区，重复则先回报。

必读：docs/requirements.md；docs/coordination/{README,decisions,tasks,sessions,questions-risks,requirements-trace,local-development}.md；docs/coordination/contracts/business-module-ports-v1.md；docs/coordination/plans/PAR-BIZ.md；docs/deliveries/TASK-002/的领域、契约、状态和report.md；TASK-QA001策略及风险用例；backend/pom.xml与已有MySQL守卫/事务实现（只读，不盲抄）。交易模块还要读TASK-005V完整金额夹具；课程读对应商品和退款设计。实际文件名用rg --files查，不能因未读而猜接口。

采用executing-plans在本聊天完成本模块完整计划，按需要使用适用技能；不再按类/单接口拆聊天。本任务是实际编码、真实本模块MySQL验证、说明和报告，不是又一份纯设计。没有持续运行八小时要求：完成必要工作即可提交，禁止无意义循环凑时长。

## 共同契约与限制

Java21、Spring Boot4.1.2 BOM、既有Maven Wrapper/JDBC/Flyway，版本从backend/pom.xml核验并记录。模块为同一个单体的Java库，未来统一数据库；不能创建微服务、独立HTTP应用、绕过认证入口或新增中间件。各模块POM独立，不修改根构建；本地端口类型声明与冻结契约一致，不编译依赖其他在途模块。领域包使用本任务指定合法Java包名（目录名连字符不用于Java包）。

Authorization/AccountDirectory/StoreDirectory/Rights/Trade/Event/Policy输入及语义严格遵守business-module-ports-v1。未配授权默认拒绝，归属由仓储查，不能信任客户端role/merchantId。测试fixture可显式给权限和策略，但不得随运行包提供万能管理员/会员或把未批准政策当默认。资金和权益唯一写者trade-core；出入场唯一写者ranking-core；人员门店唯一写者merchant-core。跨模块只走端口/事件，不能复制对方权威实体或直接写对方表。

所有者隔离、事务、幂等、乐观版本、分页和Outbox/Inbox按需要有实际实现与失败验证；不要把003B用于安全防御计数的预期异常提交方式照搬成业务部分写提交。跨模块fixture不能证明真实跨模块原子性；写清未来同数据库事务适配和失败补偿要求，不能持锁调用外部网络。未批准的收费/日期/补偿分母/权限/阈值/隐私分支返回明确未批准或待核查，继续已确认部分，不静默填默认。

卡绑定单店，不跨店、不转卡、不冻结、不自动续费；不同门店卡及个人权益独立。退款以申请快照、申请后hold、成功关闭、驳回或撤回恢复，平台统一整数金额公式；商家预约取消政策不能改订单退款。不得把真实支付预留/模拟成功说成真实收款。不得接入AI、真实支付/短信/微信/硬件、精确定位或擅自收集健康正文。前端及未获逐页视觉批准的新页面不在范围。

## 工作区、数据库、文件组织

从确认后的main创建独立worktree和任务分支，先核对路径和已有分支；不得在D:/GYM切分支，不清理别的worktree。只修改下方本任务允许目录；不改backend/**、web/**、mini/**、quality/**、其他modules/**、根POM/.gitignore、协调文档和旧交付。必要忽略项放模块自己的.gitignore。接口冲突先报告，不越界修别人。

模块代码分api（应用命令/查询DTO）、application（用例/事务）、domain（规则/状态）、ports（外部依赖）、persistence（JDBC/迁移/Inbox/Outbox）；按职责聚合，避免超大单类。src/test含领域、模块应用及MySQL集成测试，未批准策略fixture仅测试源码可见。docs/deliveries/本任务需有report.md、contract.md、verification.md及集成说明，可合并相关说明，不以文档数量代替实现。

只连接本任务唯一MySQL测试库，localhost:3306/docker mysql-dev；本地认证从适用全局规则私下读取或进程环境注入，不写源码/Prompt/日志/命令行回显/提交；.env需忽略且不回报内容。用户已授权创建项目开发测试库，本任务库不存在可创建utf8mb4；已有未知表/迁移先报告，禁止DROP DATABASE、Flyway clean、清空他人表、改MySQL全局参数。每任务连接池建议<=4、单测试JVM，有限并发，不停止共享MySQL。MySQL26.7与Flyway警告保留，不降低校验；这不是生产兼容担保。

迁移只能在本模块src/main/resources/db/modules/<module>/下，测试显式locations，只执行本模块预留版本；不能自动扫描进入主服务，不碰V1/V2、gym_dev、gym_dev_test_003b或gym_dev_test_night001及其他业务测试库。跨模块FK当前仅存外部ID并校验端口，不能为测试创建对方表。数据库名校验不符必须启动拒绝。

## 执行与共同验收

1. 报实际起点、独占工作区/分支、文件范围、预留SQL版本和接口清单；读已确认规则与待定子项，先列本任务可独立实现部分。不因无法读取其他聊天而要求用户再次确认所有既定基线；如发现本任务重复或实质冲突才停受影响部分。
2. 先写可失败且有意义的领域/应用测试，验证失败再实现。实现本任务完整范围和持久化，不把MySQL测试替换为H2或纯内存；外部端口fixture单独标明。
3. 覆盖正常流程、跨商家/门店/主体拒绝、权限撤销后当前判断、未知依赖失败闭合、同key异体冲突、并发/回滚、重复迁移/重连持久化、敏感数据不泄漏；各项有实际不变量断言。业务特有测试见下。禁止只凑测试数量/轮询跑满八小时。
4. 从本worktree/backend执行 .\mvnw.cmd -f ../modules/<module>/pom.xml -B clean verify（替换模块名）。保留可重放配置方式和逐项结果；建立真实MySQL迁移、并发/失败注入、数据库重新连接后的持久化证据。失败定位后在本范围修复，不关闭校验或删断言。
5. 交付应用端口精确Java类型/方法、命令/查询示例、授权action清单、表与SQL checksum、事件payload样例/schema、fixture标记、未来统一装配步骤/事务约束。消费者fixture测试与真实提供方未联调分别列出。必要设计澄清放自己的contract.md，冻结契约变更先报告协调者。
6. 自审范围、敏感信息、git diff --check和测试。按完整可理解修改单元选择性add/commit/push，首次推送设上游；仅提交本任务目录，不强推、不合并main、不覆盖用户改动。推送/认证失败保留本地提交，报告原因，不猜凭据。

共同验收是M层：本模块真实逻辑+自己MySQL/事务/权限端口/自动测试。I层真实IAM/其他提供方/统一HTTP/统一迁移集成、U层获批多端UI、L层真实渠道上线都未完成。模块M通过只能解锁后续集成评审，不能将整条复合REQ标完成。用户/协调者验收前状态最多待验收。

## 结果回报和回传

report.md必须列任务编号、实际基线/前置文档、修改摘要、文件、分支/完整SHA/远端同步、worktree绝对路径、验证命令/退出码/真实MySQL范围、失败测试到修复证据、未完成/受阻/模拟项、数据与端口/事件变更、待批政策及下一步；提供可重放材料路径。示例不得含凭据和真实个人信息。

当前没有自动回传能力。将最终report.md链接、完整SHA、分支和材料路径显示在聊天最终回复，请用户带回COORD-01；不要声称已经通知协调者/被验收/会自动继续其他任务。发现阻塞、需求冲突或需改共享契约时先报告，继续本任务无依赖部分；不得跨目录或自行决定未批业务。

## 本任务独占契约与业务验收

- 任务：TASK-BIZ002；关联REQ-005、REQ-007、REQ-008、REQ-009、REQ-010、REQ-011、REQ-016、REQ-017、REQ-018、REQ-021、REQ-024、REQ-025、REQ-026、REQ-027、REQ-028、REQ-029、REQ-032、REQ-033、REQ-034、REQ-035、REQ-036、REQ-037、REQ-038、REQ-039、REQ-062、REQ-064、REQ-065、REQ-066、REQ-067、REQ-082、REQ-084、REQ-087、REQ-090、REQ-091、REQ-094、REQ-095、REQ-096，仅本模块子范围。
- 允许修改：modules/trade-core/**、docs/deliveries/TASK-BIZ002/**；其他一律只读。
- 分支：codex/task-biz002-trade-core；独立worktree由本聊天创建，绝对路径回报。
- Java包：com.gym.modules.tradecore；源码根modules/trade-core/src/main/java/com/gym/modules/tradecore/。
- 唯一库：gym_dev_test_biz002；迁移V4，表前缀trd_；脚本位于modules/trade-core/src/main/resources/db/modules/trade-core/。
- 本模块范围及特别验收：BIZ002 trade-core：卡商品/卡与独立权益、订单/模拟付款事实、消费/hold/恢复/关闭、退款申请审核/原指令重试/对账差异，会员与私教分账。原样消费005V16金额夹具；买赠计总权益、到分/上限、同单唯一申请、并发消费、重复/迟到回调不双发权益/双退款，个人普通退款拒绝且异常保留升级。模拟支付只有显著SIMULATED。时间类用明确已归一总/剩权益和policy参数；日卡/补偿分母/结算/平台价/佣金未知返回政策未批准或待核查，不猜零费用。权益来源按门店/个人独立，暂停不影响其他来源，停用区间去重算法与已批交集规则实现；精确恢复组合未批单独列明。
- 精确应用方法/数据模型由本任务在上述冻结语义内落实，全部公开用例有授权和事务边界；不因其他模块在途推迟本模块fixture级验证。实际提供方集成留给统一责任方。
- 交付：模块全部源码/独立POM/本库迁移与有效自动测试，docs/deliveries/TASK-BIZ002/report.md及契约/验证/集成材料。报告标明M层交付，不能标实际业务全量完成。
