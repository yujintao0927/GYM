# TASK-BIZ001 交付报告

状态：M层独立模块实现与验证已交付，待用户/COORD-01验收；不标整条复合REQ完成。I真实提供方/统一HTTP与迁移、U获批多端UI、L真实渠道上线均未完成。

## 基线、分支与文件

实际起点：b3ce453e92cc378a25dac27f7d1591eb01274baf，远程main当时同SHA，包含DEC-027、PAR-BIZ及本任务Prompt；不是290e098。核对时无同任务本地/远程分支或worktree。当前聊天为用户人工派发，不声称自动绑定/回传其他聊天。

实现提交：e735225b56b497336473b06ec4adb8a3a4988acf，已成功push并设置上游origin/codex/task-biz001-merchant-core。分支：codex/task-biz001-merchant-core。随后报告提交只补本报告及尾随空白规范化，最终报告提交完整SHA由聊天最终回复提供；其代码语义与上述已验证实现提交一致。

独占worktree：C:/Users/28795/.codex/visualizations/2026/11/13/01a1d576-43e1-71c1-8069-975dca2032de/task-biz001。共享D:/GYM仍为main，AGENTS.md与docs/deliveries/TASK-WEB001未跟踪材料保留，未切共享分支、未合并main、未清理任何其他worktree。

唯一修改目录：modules/merchant-core/**、docs/deliveries/TASK-BIZ001/**。源码按api/application/domain/ports/persistence划分；独立pom.xml、V3迁移、4个测试类、tools/verify-local.ps1。交付材料contract.md、verification.md、integration.md、event.schema.json、progress.md、dependencies.log、verification.log及各red-*.log。未修改backend/web/mini/quality/其他模块/root构建/协调文档/旧交付。

前置依据：docs/requirements.md，coordination的README/decisions/tasks/sessions/questions-risks/requirements-trace/local-development、冻结business-module-ports-v1、PAR-BIZ；TASK-002领域/契约/状态/权限/未决与report，QA001策略/风险；backend/pom.xml、已有MySQL守卫与事务实现只读。历史业务5f5e091、领域c781088、技术批准678b873、会话e20e499作为已批基线，不沿用旧草案中的技术未批结论；不复制003B安全计数的异常提交方式。

## 实现与结果

商家私有入驻资料引用/PENDING人工终审、通过或驳回且终审不覆盖；门店增查改逻辑删除/归属不可迁移；员工角色、显式门店集合、授予/撤授/当前判断；门店显式end/graceEnd服务状态。员工与会员完全独立，新增店不扩已有范围。数据库归属用于授权，不信客户端role/merchantId。全部业务写有当前账号/权限、幂等及版本边界，业务失败完整回滚；查询重新授权和隔离分页。

授权写必须有平台来源及带id/version/provenance的GrantPolicy；运行默认拒绝fixture及UNAPPROVED。批准的生产策略没有提供，OD-02继续待批。测试fixture范围子集/禁自提权仅测试源码，不发布生产管理员。可信StoreDirectory/currentStaffDecision提供方只返最小范围，不开放HTTP旁路。

所有数据只写mch_表；无跨模块表或外部FK。Outbox、审计、状态和幂等回执同事务；授权事件带政策来源/版本。不消费外部事件，故无需Inbox；没有把Outbox持久化称作投递成功。精确Java方法、DTO、action、表和事件schema见contract.md。

实际验证命令：从本worktree/backend执行 `.\mvnw.cmd -f ../modules/merchant-core/pom.xml -B clean verify`，退出0，最终27项/0失败/0错误/0跳过，其中21项真实MySQL。可重放命令及环境方式见verification.md与模块tools/verify-local.ps1；配置从本地规则私下读取或环境注入，不写入源码/日志/本报告。

数据库唯一gym_dev_test_biz001（首次不存在，按授权创建utf8mb4）；MySQL26.7，V3显式locations，checksum=-407201789，重复迁移0。真实双线程屏障证明同版本恰一成功；本模块临时Outbox触发器故障证明源、审计、receipt全回滚；新独立DataSource证明记录重连后存在；自己新建的唯一未知夹具表证明守卫拒绝，finally只移除该夹具。未DROP DATABASE/clean/清他人表/改MySQL全局参数/停共享MySQL。

实际解析Boot4.1.2、Spring JDBC7.0.10、Flyway12.4.0、MySQL驱动9.7.0、Maven3.10.0。JDK用户提供路径实际为22.0.1，编译release21、class major65；Java21运行时测试未完成。Flyway对MySQL26.7的警告原样保留，不代表生产兼容担保。

失败到修复链：服务端点与显式授权空桩→实现；数据库守卫空桩→唯一URL限制；真实V3后应用空桩→事务用例；分页缺实现/删除重试隐藏墓碑→绑定游标/当前授权后重放；原集合突变与原始SQL异常→全程不可变快照/安全错误映射；独立审查发现账户/政策内部消息、非法输入原始异常及政策布尔来源不足→新增RED回归并单轮修复为安全503/422/三态来源与fixture隔离。逐例测试名、日志和断言见verification.md。

自审：最终真实测试通过；源及交付扫描真实本地密码无匹配；只本任务路径入Git。最初暂存diff-check发现Maven日志尾随空格及源码EOF空行，报告提交仅规范化这些空白，未改SQL/断言/业务语义，最终对实际基线diff --check通过。

## 限制、取舍及后续

- Java21字节码已验证，Java21实机运行仍待补；不能把用户给出的22目录称为21运行证据。
- 真IAM/账号epoch、其他模块及统一数据库事务未联调，当前均为明确fixture；同数据库适配须加入事务并统一锁序，不得持锁调网络，不能以fixture证明跨模块原子性。若无法加入同库须先批状态编排及失败补偿。
- P01精确日历未批，不暗算30天月/时区/日历端点；本模块接收显式期限，30天提醒/15天宽限主体保留。提醒发送、完整P04能力矩阵、恢复/权益补偿交相应责任方评审，本模块不写权益/收费，不影响他店及个人来源。
- 已一次独立只读审查，三项Important已修复并全套验证；Minor源码排版较紧凑暂缓，没有二次独立审查或已获用户验收的声明。
- 实施裁定：worktree放本会话可写目录，避免改根ignore（需保留路径或依Git恢复）；模块mutex选择正确性优先，性能/跨模块锁序需I层核验；实际JDK22以release21编译（21运行兼容未证）；目录与当前员工判断为可信内部提供方（集成不得暴露未认证HTTP）；按用户目录限制使用本交付progress.md作executing-plans ledger，未创建额外技能scratch（技能辅助脚本不可直接复用）。
- 无真实支付/短信/微信/硬件/AI/精确定位/健康正文，无商业上线。前端批准图与批准范围：不适用，本任务无前端。

下一步请用户将本report.md、最终完整SHA、分支和材料目录带回COORD-01进行M层评审。没有自动回传能力，没有通知协调者，也不会自动接手其他任务。
