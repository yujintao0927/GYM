# TASK-BIZ001 验证记录

最终验证：2026-11-13 23:48:40 +08:00，clean verify退出0；27项、0失败、0错误、0跳过。完整输出 verification.log；其中21项为真实MySQL应用测试，6项为领域/URL守卫测试。不存在H2替代或跳过数据库配置的成功路径。

重放：在本worktree根执行 `modules/merchant-core/tools/verify-local.ps1 -UsePersonalRules`。脚本私下读取本机用户规则配置注入进程环境；或不传该开关，由环境私下提供 GYM_DB_USERNAME/GYM_DB_PASSWORD。脚本不打印配置，自动固定唯一数据库URL。内部从backend执行 `.\mvnw.cmd -f ../modules/merchant-core/pom.xml -B clean verify`；单JVM测试，有限双线程并发，DriverManagerDataSource无保留连接池。

实际版本：Boot parent/BOM4.1.2；Maven Wrapper3.3.4/Maven3.10.0；Spring JDBC7.0.10；Flyway core/mysql12.4.0；MySQL Connector/J9.7.0。解析证据dependencies.log，命令dependency:tree退出0。实际JDK路径用户提供为openjdk-22.0.1，运行报告22.0.1；编译release21，产物class major65；Java21运行时尚未实机测试，不能宣称已在21运行。

数据库：localhost:3306/gym_dev_test_biz001，MySQL26.7，utf8mb4断言通过；首次不存在，通过明确JDBC参数createDatabaseIfNotExist创建；未连接其他业务库。Flyway显式locations=classpath:db/modules/merchant-core，预留V3，实际checksum=-407201789、success=true；重复migrate=0。MySQL版本高于Flyway验证范围的警告保留；额外Maven仓库元数据401警告也保留，构建本身成功；无生产兼容担保。

| 不变量 | 实际证据 |
| --- | --- |
| 人工入驻仅PENDING单次终审，驳回不得建店 | rejectedMerchantCannotCreateStoreAndReviewIsFinal；同时验证授权商家查询 |
| 门店版本、同key同体重放/异体409、当前权限再判 | storeVersionIdempotencyIsolationAndReconnection、oldReceiptDoesNotBypassCurrentRevocation |
| 显式授权不因新店扩大，撤授立即拒绝、不能自提权 | explicitGrantRevokeCurrentDecisionAndNewStore；fixtureSubsetPolicyAndIndependentReconnectHaveRealAssertions（范围子集、角色提权拒绝） |
| 角色/门店/所有者来自数据库，查询和写隔离 | scopedProviderUsesPersistedOwnersAndBlocksForeignReadWriteDelete、crossMerchantGrantDeniedWithoutWrites；拒绝后版本/receipt无变化 |
| 会员与员工独立、撤销账号不因旧grant继续放行 | 员工模型无会员字段；persistedGrantDoesNotSurviveClosedOrUnavailableAccount验证DENY/UNAVAILABLE |
| 未知授权、缺平台来源/账号失效、未批政策失败闭合 | unknownDependenciesAndUnapprovedGrantFailClosed、inactiveAccountsAndMissingPlatformSourceFailClosed、fixturePolicyCannotEnterDefaultRuntimeBoundary |
| 可变输入不能绕过批准快照 | scopeSnapshotSurvivesCallerMutationDuringPolicy：策略回调修改原集合，DB仍无新增scope |
| 事务中途失败零部分写 | outboxFailureRollsBackSourceAuditAndReceipt：本库唯一临时trigger注入SQLSTATE45000，finally移除；源version、audit、receipt均不变，原key随后可成功 |
| 真实并发版本争用 | concurrentUpdatesHaveOneWinner：双线程CyclicBarrier，结果恰为200/409，最终version1 |
| 删除/重试/读取及分页隔离 | deletedStoreReplaysAndCannotBeRead、paginationBindsActorAndMerchantAndRechecksPermission |
| 服务边界、非法输入、不同门店独立 | ServiceTermTest两项；serviceEndpointsPersistAndInvalidWriteRollsBack；directoryAndGrantQueryReturnPersistedScopedSnapshots |
| 迁移与重连 | repeated migration0、utf8mb4；fixtureSubsetPolicyAndIndependentReconnectHaveRealAssertions新建独立DataSource查询先前持久化记录 |
| 未知对象启动拒绝 | ownUnknownFixtureTableBlocksMigrationAndIsRemoved：仅创建本测试新表，拒绝迁移后finally移除它；不清理他人表/数据 |
| 凭据/内部异常不泄漏 | dependencySecretsAndInvalidScopeNeverEscape、policyProviderExceptionHasSafeBoundary；源/交付日志扫描本地真实密码无匹配；Outbox仅最小字段，授权事件保留政策来源版本 |

RED→GREEN证据：red-service（非法端点未拒绝/算法空桩）；red-grant（空集合/撤授/不可变范围）；red-dbguard（其他库未拒绝）；red-application（真实V3后应用空桩）；red-boundaries（分页未实现/删除重试404）；red-safety（原集合被修改/数据库原始异常）；red-review（账号内部消息、422映射）；red-policy（fixture误入运行）；red-provider（目录空桩/策略异常）。这些失败均在最终verification.log对应功能修复并全套通过。最初未知数据库和沙箱网络/缓存限制为环境问题，未计作业务失败证据。

已做一次独立只读审查：无Critical，三项Important已用失败测试复现并在单轮修复中处理；补齐当前账号失效、旧key撤权、按仓储scope鉴权用例。Minor仅源码排版偏紧凑，暂缓行为无关的格式重排。审查后新增的目录/员工查询有应用MySQL回归；没有声称二次独立审查。

只证明消费者fixture/本模块逻辑；真实IAM/其他模块/统一迁移、跨模块事务及Outbox投递未联调。无需Inbox：当前无外部事件消费者。I/U/L未完成，前端视觉批准不适用。
