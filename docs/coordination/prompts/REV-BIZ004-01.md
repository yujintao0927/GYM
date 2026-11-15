# TASK-BIZ004 原聊天返工 / EXEC-BIZ004

项目D:/GYM，origin=https://github.com/yujintao0927/GYM.git；协调者COORD-01。继续本任务原独立worktree和原codex分支，不创建重复业务任务，不在共享D:/GYM切分支。原交付输入为 `2676dc952f7e73a6243a91e8fe0e539d3adc27bd`；先核对HEAD、工作区、origin和远端，保留用户/其他任务改动。最新main含本轮验收文档是规则输入，**不要求合并main进任务分支**；直接读取D:/GYM/docs/coordination的本轮材料，若状态偏离先报告。

必读适用全局/项目AGENTS.md、docs/requirements.md、docs/coordination/prompts/TASK-BIZ004.md、plans/PAR-BIZ.md、contracts/business-module-ports-v1.md、reviews/2026-11-16-TASK-BIZ004.md及自己交付report/contract/verification。采用systematic-debugging和必要实现/验证技能，先用回归固定问题，再修改本范围源码；不做UI、不改未批收费/权益/隐私规则。

当前状态返工，完整目标仍为原任务M层代码+持久化+真实本库MySQL，不可只改报告或以构建通过替代。负责人仍原EXEC-BIZ004，范围仅modules/operations-core/**和docs/deliveries/TASK-BIZ004/**；不改backend/web/mini/其他模块/总协调台账。自己的测试库gym_dev_test_biz004保持唯一，不使用gym_dev或night/003b库；本地秘密仅进程环境不输出。已发布迁移V6不得重写、repair、clean或清库；如修复必须增加SQL，BIZ002仅预留V9，BIZ006仅预留V10，其他任务无新增SQL版本，需要时先报协调者。新增SQL仍放本模块命名空间，显式测试locations，旧checksum不变。未知表/历史差异先报告，不越权覆盖。

具体返工与验收要求：

- REV-BIZ004-01 / P2：Commands.java:10使用String.valueOf，null与合法字符串"null"哈希相同，报修imageRef更换为空值/字符串的异体用同key会被误判重放。规范编码需显式null标记、类型/长度/内容，补两方向异体409、相同规范请求重放及失败回滚。已有持久回执要设计版本/兼容处理，不能静默重释旧摘要或清表。
- 历史统计不可变快照不在本批补造；保留已披露边界。真实事件提供方、跨模块撤权与触达政策I层未完成不因此单独判M失败。

共同要求：规范编码须区分null/字符串、类型/边界，保持旧持久回执可解释；新签名需写自己contract和映射/兼容说明，不能修改协调者冻结文档。fixture/运行时批准分离，默认授权拒绝；发现同类漏洞同时审查本模块其他路径。业务锁/CAS不能代替业务级唯一幂等。新增有意义测试，不删旧测试或放宽断言；使用JDK21实际运行既有Maven3.10.0及自己独立POM clean verify，报告真实MySQL、并发/失败回滚、重复迁移/重连、错误输出和敏感/范围/diff检查。保持兼容警告和所有I/U/L未完成声明，不擅自接真实外部渠道。

按完整可理解修改单元选择性add/commit/push原专属分支，首次上游如已设置沿用；不强推、不合并main、不改写历史或清理他人worktree。遇业务冲突/共享接口变化先报告，继续本范围独立部分。完成后状态仅待重新验收。

回报docs/deliveries/TASK-BIZ004/report.md及修订契约/verification：原SHA与新完整SHA、原worktree路径、分支/远端、修改文件、逐条REV对应修复和验证命令/退出码/断言、未完成及模拟/真实边界、迁移/端口变化及下游依赖。最终给用户报告链接和SHA带回COORD-01；当前无自动回传，不能声称已通知或已验收。任务范围完成即可提交，不空转凑时长。
