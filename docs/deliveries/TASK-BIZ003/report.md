# TASK-BIZ003 / EXEC-BIZ003 交付报告

2026-11-13启动，Asia/Shanghai；M层模块实现完成，提交协调者待验收，未自授正式验收。不是完整业务、真实渠道或商业上线完成。

## 基线与独占范围

实际起点 b3ce453e92cc378a25dac27f7d1591eb01274baf，包含DEC-027、PAR-BIZ及本任务Prompt；远端main只读核实相同，历史290e098未作为起点。分支 codex/task-biz003-booking-core。

worktree：C:/Users/28795/.codex/visualizations/2026/11/13/01a1d576-8e5e-7e31-8054-c8f1dfa127a0/task-biz003。

origin：https://github.com/yujintao0927/GYM.git。共享 D:/GYM 未切分支，初始未跟踪 AGENTS.md 与 TASK-WEB001 材料保留。未发现本地或远端同任务分支/worktree；当前工具无 list_threads，不能据此声称已核查全部聊天。用户本次手动派发是开工授权。

适用用户全局规则与 D:/GYM/AGENTS.md 已读取。输入包括 requirements.md、协调 README/decisions/tasks/sessions/questions-risks/requirements-trace/local-development、冻结 business-module-ports-v1、PAR-BIZ、TASK-002领域/契约/状态/report、QA001策略及风险用例、对应005V退款设计、backend/pom.xml 与既有MySQL/事务源码（只读）。原003B预期异常提交模式未照搬，业务异常在事务外转换，源写全回滚。

仅修改 modules/booking-core/** 与本交付目录；无 backend/web/mini/quality/其他模块/共享协调修改。使用 executing-plans、TDD、独立worktree、verification-before-completion及一次独立只读代码审查。进度记录见 plan.md，审查五项及自审幂等修复见 review.md。

## 实现与材料

- Java库独立POM、api/application/domain/ports/persistence；默认拒绝端口，无HTTP服务/登录旁路/万能角色。
- 团课与私教排课、半开教练时段排他、容量及同成员占席约束、预约/取消/签到履约、版本与幂等回执、主体/归属校验、绑定订单与商品/教练权益来源、可信政策快照与接受证据的原始订单引用。
- 期限/次数商品显式快照，权益命令返回并保存版本，后续动作读取当前源版本；消费/取消费通过本地事务端口。取消费只生成SIMULATED交易引用，绝非真实扣款；订单退款独立由trade处理。
- MySQL V5：五张bkg表及Flyway历史，数据库唯一 gym_dev_test_biz003，utf8mb4。预留版本未越界，既有V5安装后不改写。
- 状态、回执、Outbox同事务；事务内复核当前授权，查询分页绑定主体/门店/类型。当前不消费外部异步事件，故没有虚构Inbox功能。
- 精确类型/方法/action、事件payload、SQL SHA256、端口fixture边界和装配要求：[contract.md](contract.md)。
- 重放命令与失败到修复实绩：[verification.md](verification.md)，完整最终日志：[verify-output.log](verify-output.log)。

## 实际验证

从本worktree/backend执行 `.\mvnw.cmd -f ../modules/booking-core/pom.xml -B clean verify`，通过模块verify.ps1私下注入进程凭据，JAVA_HOME指定D:/java_environment。最终退出0，24测试、0失败/错误/跳过。Java21.0.2、Boot4.1.2、JDBC7.0.10、Connector9.7.0、Flyway12.4.0。MySQL26.7警告保留，未降低校验。

真实范围：仅本库创建、V5迁移/重复0、重连查询、教练/末席/取消签到竞争、权益与交易失败、回执冲突发生在源写之后的全事务回滚（占席/预约/回执/Outbox均断言），严格版本提供方同源两次预占后履约、归属/撤权/依赖未知/异常脱敏/分页/政策/幂等异体。Flyway checksum=1511461192；SQL SHA256=ABECEFF60E2918CF9E346FBFE810379A20837924BD24104EF40CF677B7E32852。

未连接gym_dev、003b/night001或其他业务库；不DROP/clean/清表，不改MySQL全局参数。测试只追加合成UUID数据，单JVM、竞争2线程。Docker Desktop/backend进程运行；默认沙箱Docker管道拒绝及CLI未返回，后续直接JDBC核验端口与本库成功，无重启共享容器。Maven共享缓存锁/Git元数据/网络验证使用批准的提升执行，不关闭锁或覆盖共享改动。

## 未完成、模拟与下一步

当前权限、账号/商家/教练目录、政策、Rights/Trade为消费者fixture（仅测试源码），真实提供方未联调。fixture响应和调用次数不证明跨模块原子性；I层必须同一DataSource/事务、账号与服务栅栏、真实权益版本/hold互斥、首次履约激活、取消费用支付/UNKNOWN对账、Outbox派发、统一HTTP和迁移装配，禁止持锁网络调用。

价格/结算与具体日期、取消时间阈值等未批政策无默认。商品/接受证据通过不可变订单快照及其引用核验，当前不提供课程目录发布CRUD。未提供真实退款资金执行，不修改统一退款公式。UI/U/L无交付；没有批准效果图与页面范围，本任务没有开发前端。

范围自审、敏感信息及暂存差异检查后提交推送；实际Git结果见下方追加。请用户将本报告、最终完整SHA、分支和本目录带回COORD-01独立验收；没有自动回传或通知承诺。

## 实际 Git 结果

主实现提交：13efba09c50b361bb700ab3a049188128c3f7e86。首次 `git push -u origin codex/task-biz003-booking-core` 退出0，已建立上游，未强推或合并main。本节与进度收尾为随后独立文档提交；最终完整SHA取包含本节的分支HEAD，在聊天中回报，避免文件包含自身SHA的循环。

主提交前暂存 diff --check 退出0；提交后相对基线 diff --check 退出0，20个文件仅本任务两目录。私有凭据逐文件检查无匹配，模块target和.env不跟踪。共享D:/GYM依旧仅原有未跟踪AGENTS.md与TASK-WEB001材料。最终完整SHA及远端同步以聊天交付时核对为准。
