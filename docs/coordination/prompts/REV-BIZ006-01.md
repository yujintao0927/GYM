# TASK-BIZ006 原聊天返工 / EXEC-BIZ006

项目D:/GYM，origin=https://github.com/yujintao0927/GYM.git；协调者COORD-01。继续本任务原独立worktree和原codex分支，不创建重复业务任务，不在共享D:/GYM切分支。原交付输入为 `3203bfc18600d1993e344be3cc809a5be90dddde`；先核对HEAD、工作区、origin和远端，保留用户/其他任务改动。最新main含本轮验收文档是规则输入，**不要求合并main进任务分支**；直接读取D:/GYM/docs/coordination的本轮材料，若状态偏离先报告。

必读适用全局/项目AGENTS.md、docs/requirements.md、docs/coordination/prompts/TASK-BIZ006.md、plans/PAR-BIZ.md、contracts/business-module-ports-v1.md、reviews/2026-11-16-TASK-BIZ006.md及自己交付report/contract/verification。采用systematic-debugging和必要实现/验证技能，先用回归固定问题，再修改本范围源码；不做UI、不改未批收费/权益/隐私规则。

当前状态返工，完整目标仍为原任务M层代码+持久化+真实本库MySQL，不可只改报告或以构建通过替代。负责人仍原EXEC-BIZ006，范围仅modules/ranking-core/**和docs/deliveries/TASK-BIZ006/**；不改backend/web/mini/其他模块/总协调台账。自己的测试库gym_dev_test_biz006保持唯一，不使用gym_dev或night/003b库；本地秘密仅进程环境不输出。已发布迁移V8不得重写、repair、clean或清库；如修复必须增加SQL，BIZ002仅预留V9，BIZ006仅预留V10，其他任务无新增SQL版本，需要时先报协调者。新增SQL仍放本模块命名空间，显式测试locations，旧checksum不变。未知表/历史差异先报告，不越权覆盖。

具体返工与验收要求：

- REV-BIZ006-01 / P1：RankingService.java:63–72关闭参榜也guard rights=true，权益失效后无法撤回展示。退出应保留身份/本人权限但不依赖付费来源或权益服务可用；测试到期、hold、权益依赖不可用均可关闭，开启仍要求有效资格。
- REV-BIZ006-02 / P2：RankingService.java:50–57、88–104手环有效日与榜单参榜者无权威权益有效区间核验，只核查看者。补参榜者资格证据/有效区间及失效过滤，覆盖非有效权益期间数据、开启后到期/暂停及独立权益来源；不要在本批擅定未批阈值、补录追溯政策，未知分支明示PENDING_POLICY而非直接计入。
- REV-BIZ006-03 / P2：VisitService.java:90允许未来leftAt，RankingService.java:133–136直接投影有效日。更正不得晚于权威now；投影拒绝/隔离不可能未来事实，现有固定Clock测试须使用合法过去时间并补未来拒绝与不产生有效日/事件副作用断言。
- REV-BIZ006-04 / 验证缺口：已有同JDBC Connection权益端口只测试consume直接抛错；补显式测试fixture先在同事务写权益变化，再令Visit/Outbox后续失败，断言两边都回滚，重复挑战不二次扣。该fixture不等于真实trade联调，但能验证M原子挂接点。
- REV-BIZ006-05 / 冻结端口缺口：Store缺status、active无epoch事实，补门店状态/版本及账号epoch表示和复核契约；消费真正事务提供方/I层仍未验证。原P05周期/阈值/少人数不得用fixture值做运行时默认。

共同要求：规范编码须区分null/字符串、类型/边界，保持旧持久回执可解释；新签名需写自己contract和映射/兼容说明，不能修改协调者冻结文档。fixture/运行时批准分离，默认授权拒绝；发现同类漏洞同时审查本模块其他路径。业务锁/CAS不能代替业务级唯一幂等。新增有意义测试，不删旧测试或放宽断言；使用JDK21实际运行既有Maven3.10.0及自己独立POM clean verify，报告真实MySQL、并发/失败回滚、重复迁移/重连、错误输出和敏感/范围/diff检查。保持兼容警告和所有I/U/L未完成声明，不擅自接真实外部渠道。

按完整可理解修改单元选择性add/commit/push原专属分支，首次上游如已设置沿用；不强推、不合并main、不改写历史或清理他人worktree。遇业务冲突/共享接口变化先报告，继续本范围独立部分。完成后状态仅待重新验收。

回报docs/deliveries/TASK-BIZ006/report.md及修订契约/verification：原SHA与新完整SHA、原worktree路径、分支/远端、修改文件、逐条REV对应修复和验证命令/退出码/断言、未完成及模拟/真实边界、迁移/端口变化及下游依赖。最终给用户报告链接和SHA带回COORD-01；当前无自动回传，不能声称已通知或已验收。任务范围完成即可提交，不空转凑时长。
