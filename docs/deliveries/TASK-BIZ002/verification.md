# TASK-BIZ002 验证

## 命令与环境

本worktree/backend：`./mvnw.cmd -f ../modules/trade-core/pom.xml -B clean verify`。凭据只由GYM_DB_USERNAME/GYM_DB_PASSWORD进程环境注入，不回显。可调用verify.ps1重放；没有MySQL时测试失败，不skip。当前JDK22.0.1，release21；Boot父BOM4.1.2。dependencies.log实际解析：Spring JDBC/TX7.0.10、Connector/J9.7.0、Flyway core/mysql12.4.0。dependency:tree退出0；两个外部插件前缀元数据源401警告保留，不影响已解析依赖构建。

## 实际MySQL

专属gym_dev_test_biz002此前不存在（mysql-preflight.log），按授权创建utf8mb4并确认空表；SELECT VERSION()=26.7.0。仅V4显式classpath:db/modules/trade-core；7张trd_表及Flyway历史，重复migrate=0，新连接余额持久。Flyway警告已验证最高MySQL9.4，保持校验，非厂商认证。无其他库迁移、DROP DATABASE/clean/清表/全局参数变更。SQL触发器仅在本库故障注入，finally删除；不停止共享容器。Docker只读列表未及时响应，JDBC已经实证连接。

V4 Flyway checksum=-2127131053；SQL SHA256=397F139C82C3A2E6350D41C06551515A6CAAB88A75FD3CEDBF5F4B31B731AF75。安装后没有改写迁移。

## 测试与失败证据

最终final-verify.log：26测试，0失败/0错误/0跳过，BUILD SUCCESS，退出0。RefundRules2（原样16个005V夹具在一个方法内逐项断言）、RefundInstruction3、Intervals2、MysqlEnvironment1、TradeMysql18。

覆盖：赠送总量/半分/大整数/累计上限；原指令未知不重发和迟到成功；并交差区间去重；迁移重复；两线程屏障总12并发各扣8仅一成功/余额4及重连；同键异体/撤权后回放拒绝/默认拒绝/跨主体404；个人无store及门店停用隔离；预占释放/hold与另一来源独立；暂停后允许退款和释放；申请7500快照/唯一退款资金流水；错指令及过时失败拒绝；PAYMENT/REFUND/NET事实对账/未付款0对0；个人异常人工引用；游标跨store拒绝。

付款和退款成功分别在Outbox插入时用真实MySQL触发器抛错，断言余额、资金、回执全部回滚，再同key成功；依赖抛合成私有详情只返回503且余额不变。

RED→GREEN日志：red→green（缺公式到2通过）；instruction-red→baseline-verify（缺指令到6通过）；application-red→application-green（缺应用到5个MySQL测试）；extended-red→extended-green（预占/恢复/区间到16通过）；isolation-red→isolation-green（个人null store错误到18通过）；review-red→review-green（审查API到22通过）；dependency-red观察到私有详情穿透后修复；event-red缺退款金额字段后修复；paging-red缺游标入口后修复；reapplication-red复现撤回后无法再申请后修复。最后完整运行见final-verify.log。最初Maven锁沙箱失败、Unknown database均已定位解决，保留日志而不冒称业务失败。

## 边界

权限/账号/门店/政策都是测试源码fixture；未证明真实提供方联调、跨模块账号关闭栅栏/原子性、HTTP/UI/真机/真实渠道/生产负载。未运行backend全套测试，避免越出唯一测试库边界。时间归一算术与区间算法不等于TIME生命周期或落库补偿验收。没有把整条REQ或I/U/L标完成。

日志仅规范化行尾和尾随空格，以通过git diff --check；未改变结果、堆栈或断言正文。
