# 已撤销的审查候选，禁止派发

2026-11-16复核后撤销两项M阻断候选：显式测试fixture注入符合原约定；未发现当前可信BookingFailure实际敏感来源。TASK-BIZ003已独立JDK21/MySQL 24项通过并按ACC-012验收，不执行下面历史草案。本撤销只针对返工草案，不取消业务任务。

---

# TASK-BIZ003 原聊天返工 / EXEC-BIZ003

项目D:/GYM，origin=https://github.com/yujintao0927/GYM.git；协调者COORD-01。继续本任务原独立worktree和原codex分支，不创建重复业务任务，不在共享D:/GYM切分支。原交付输入为 `e874dbaa001da6a6578ba79776bbd8c0ae9c636b`；先核对HEAD、工作区、origin和远端，保留用户/其他任务改动。最新main含本轮验收文档是规则输入，**不要求合并main进任务分支**；直接读取D:/GYM/docs/coordination的本轮材料，若状态偏离先报告。

必读适用全局/项目AGENTS.md、docs/requirements.md、docs/coordination/prompts/TASK-BIZ003.md、plans/PAR-BIZ.md、contracts/business-module-ports-v1.md、reviews/2026-11-16-TASK-BIZ003.md及自己交付report/contract/verification。采用systematic-debugging和必要实现/验证技能，先用回归固定问题，再修改本范围源码；不做UI、不改未批收费/权益/隐私规则。

当前状态返工，完整目标仍为原任务M层代码+持久化+真实本库MySQL，不可只改报告或以构建通过替代。负责人仍原EXEC-BIZ003，范围仅modules/booking-core/**和docs/deliveries/TASK-BIZ003/**；不改backend/web/mini/其他模块/总协调台账。自己的测试库gym_dev_test_biz003保持唯一，不使用gym_dev或night/003b库；本地秘密仅进程环境不输出。已发布迁移V5不得重写、repair、clean或清库；如修复必须增加SQL，BIZ002仅预留V9，BIZ006仅预留V10，其他任务无新增SQL版本，需要时先报协调者。新增SQL仍放本模块命名空间，显式测试locations，旧checksum不变。未知表/历史差异先报告，不越权覆盖。

具体返工与验收要求：

- REV-BIZ003-01 / P2：BookingService.java:59、84–86仅拒UNAPPROVED，普通运行构造直接接受CONFIGURED_FIXTURE。补默认拒绝fixture政策的运行边界，只在显式测试模式允许；默认入口注入fixture仍不得排课/预约，APPROVED_RUNTIME正常，UNAPPROVED拒绝。
- REV-BIZ003-02 / P2：BookingService.java:21原样透传所有BookingFailure，端口也可抛该公开异常，敏感code/message越过应用边界；增加端口异常规范化、稳定允许码映射，端口抛BookingFailure(503,"fixture-sensitive-marker")的输出不得含标记。保持本地业务状态码含义，增加测试。
- I层提示而非本次M缺陷：商家NORMAL/GRACE/DISABLED与预约ACTIVE须显式映射；真实Trade/Rights同事务仍留I验证，不用fixture结果宣称已完成。

共同要求：规范编码须区分null/字符串、类型/边界，保持旧持久回执可解释；新签名需写自己contract和映射/兼容说明，不能修改协调者冻结文档。fixture/运行时批准分离，默认授权拒绝；发现同类漏洞同时审查本模块其他路径。业务锁/CAS不能代替业务级唯一幂等。新增有意义测试，不删旧测试或放宽断言；使用JDK21实际运行既有Maven3.10.0及自己独立POM clean verify，报告真实MySQL、并发/失败回滚、重复迁移/重连、错误输出和敏感/范围/diff检查。保持兼容警告和所有I/U/L未完成声明，不擅自接真实外部渠道。

按完整可理解修改单元选择性add/commit/push原专属分支，首次上游如已设置沿用；不强推、不合并main、不改写历史或清理他人worktree。遇业务冲突/共享接口变化先报告，继续本范围独立部分。完成后状态仅待重新验收。

回报docs/deliveries/TASK-BIZ003/report.md及修订契约/verification：原SHA与新完整SHA、原worktree路径、分支/远端、修改文件、逐条REV对应修复和验证命令/退出码/断言、未完成及模拟/真实边界、迁移/端口变化及下游依赖。最终给用户报告链接和SHA带回COORD-01；当前无自动回传，不能声称已通知或已验收。任务范围完成即可提交，不空转凑时长。
