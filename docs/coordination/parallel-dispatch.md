# 当前并行派发包 PAR-BIZ（2026-11-13）

用户要求尽量并行、扩大单聊天任务闭环。当前新增六个完整模块编码包，每个包含领域/应用代码、自己的MySQL迁移、持久化、并发/异常/隔离验证和报告；不按接口或文件再拆聊天。项目仍是单体，模块库并不是六个新服务。参见[完整计划](plans/PAR-BIZ.md)和[冻结端口/数据所有权](contracts/business-module-ports-v1.md)。

## 保持原聊天执行

WEB001、NIGHT001、NIGHT002按用户当前语境正在执行；本轮实际发现各自独立worktree，不重复派发，不读取/覆盖其未交付修改。worktree存在只证明执行环境已创建，不证明当前CPU运行或任务完成。NIGHT001独占backend及V2，WEB001独占web，NIGHT002独占quality/identity-regression及V1测试库。MINI001仍待验收、开发工具和真机BLOCKED_ENV；006A已取消。

## 六份可复制Prompt

| 任务 | 完整闭环 | 独占代码/迁移/测试库 | 分支 | 状态 |
| --- | --- | --- | --- | --- |
| [TASK-BIZ001](prompts/TASK-BIZ001.md) | 商家、门店、员工与服务状态 | modules/merchant-core/**；V3；gym_dev_test_biz001 | codex/task-biz001-merchant-core | 待开始／等待人工派发 |
| [TASK-BIZ002](prompts/TASK-BIZ002.md) | 卡、权益、模拟交易与退款 | modules/trade-core/**；V4；gym_dev_test_biz002 | codex/task-biz002-trade-core | 待开始／等待人工派发 |
| [TASK-BIZ003](prompts/TASK-BIZ003.md) | 团课与私教预约履约 | modules/booking-core/**；V5；gym_dev_test_biz003 | codex/task-biz003-booking-core | 待开始／等待人工派发 |
| [TASK-BIZ004](prompts/TASK-BIZ004.md) | 器械维修、经营统计与站内提醒 | modules/operations-core/**；V6；gym_dev_test_biz004 | codex/task-biz004-operations-core | 待开始／等待人工派发 |
| [TASK-BIZ005](prompts/TASK-BIZ005.md) | 客服、投诉、证据与申诉 | modules/support-core/**；V7；gym_dev_test_biz005 | codex/task-biz005-support-core | 待开始／等待人工派发 |
| [TASK-BIZ006](prompts/TASK-BIZ006.md) | 出入场、模拟手环与排行榜 | modules/ranking-core/**；V8；gym_dev_test_biz006 | codex/task-biz006-ranking-core | 待开始／等待人工派发 |

每份Prompt单独交给一个新工作聊天，从包含本批文件的最新main创建自己的worktree，不要把六份贴进同一个聊天，也不要把共享D:/GYM切到任务分支。统一协调维护者COORD-01。完整Prompt含前置输入、允许范围、端口、Git/验证和回报方式；执行者不改总台账。

工具检查无create_thread/list_threads/send_message_to_thread/wait_threads，本轮只能准备等待人工派发材料，无法代建用户聊天、读取自动回传或承诺无人值守调度。用户派发后带回任务与worktree绑定，交付后带回SHA和报告。完成必要范围即可提交，无须空转跑满八小时。

## 为什么这些可以并行

输入使用当前已验收领域设计、已批Java与本批冻结的端口语义；只在自己代码目录、本模块表和唯一测试库写入。不编译依赖其他在途模块，不改backend/root公共文件。实际IAM/权益/交易/事件提供方目前由显式fixture替代；同一业务模块内全部逻辑与MySQL事务需真实实现，未批政策拒绝。缺失真实提供方不阻塞M层模块验证，但会阻塞I层整合。

## 此时不能一并宣称派发的后续工作

- 统一backend装配、公共构建、V1–V8迁移集合、真实身份/权益、跨模块事务、跨端联调：等NIGHT001及相关模块验收，指定单一集成责任方，不能现在另开共享backend写者。
- 新业务Web/小程序页面：需具体效果图逐页批准和对应契约；不能以UX001结构批准替代视觉批准。
- AI/图片识别真实接入：待对应模块提供商配置及配额等必要输入；可以保留路线图，不夜间猜厂商。
- 注销/教练健康授权的真实跨模块收口、商业隐私/未成年人/保留与支付规则、真实渠道和生产部署：须各自依赖、批准和环境；L01–L09继续保留。

M模块、I集成、U多端、L商业验收独立记录，本轮不是把原预计所有后续任务压缩成六个即全项目完成；节约的是当前可并行的模块编码等待时间。
