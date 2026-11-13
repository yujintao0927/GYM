# I 层统一装配要求

1. COORD-01独立验收本模块后，单一责任方将V8并入统一V1–V8迁移；本任务未改backend、主构建或gym_dev。当前Database.open仅放行BIZ006测试库，统一生产DataSource工厂须另行授权实现；不得以当前测试守卫绕过生产迁移审查。
2. 映射实际IAM当前授权/账号epoch栅栏、merchant门店status/version和归属、trade多来源权益及激活/消费命令。先账号栅栏→门店服务→order→权益→业务锁，真实同库同连接事务；当前消费者fixture没有写交易表，不能宣称原子激活已与提供方联调。
3. 装配经过鉴权的模拟设备唯一绑定端口。默认拒绝不可更换为万能fixture。真实硬件/门禁不在范围。
4. 入口生成可信actor/requestId/idempotencyKey，统一错误封装和SIMULATED展示。挑战有效期限及授权action角色映射必须获批；不是仅凭前端传owner/merchant授权。
5. 安装P05批准政策版本、月榜窗口、时区、并列后序、有效阈值及少人规则；地区首次计数与异常成绩处理未批，继续明确PENDING。当前fixture不作为运行时政策。
6. 可靠Outbox投递及Inbox信封适配、重试/认领/重放，再真实测试更正至榜单收敛；源存储rnk_raw主键仅证本模块去重。
7. I/U/L分别验收；无前端开发，无批准效果图，视觉批准范围为空，不触发页面实现。
