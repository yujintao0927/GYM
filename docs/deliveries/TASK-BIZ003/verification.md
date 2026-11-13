# TASK-BIZ003 验证与重放

本任务数据库仅 gym_dev_test_biz003，localhost:3306。凭据通过私有进程环境 GYM_DB_USERNAME/GYM_DB_PASSWORD；verify.ps1 在未设置时从当前用户 .codex/AGENTS.md 私下读取，不回显。禁止将凭据写入命令行或提交。

从本 worktree/backend：

```powershell
$env:JAVA_HOME='D:/java_environment'
.\mvnw.cmd -f ../modules/booking-core/pom.xml -B clean verify
```

也可运行模块 verify.ps1 -JavaHome D:/java_environment。全量日志 verify-output.log。Java 21.0.2、Boot4.1.2、Spring JDBC7.0.10、MySQL Connector9.7.0、Flyway12.4.0（Boot BOM），Maven Wrapper3.10.0；MySQL 实测26.7，保留高于 Flyway 已验证9.4的警告，不代表生产兼容。

真实执行：规则测试初次缺类编译失败，随后4项通过；白名单守卫初次缺实现编译失败，随后通过；MySQL迁移测试实际断言 applied=1 得到0，加入V5后通过；应用测试初次缺类失败；接受取消收费测试实际命中 CANCELLATION_TRADE_ADAPTER_REQUIRED，补交易端口后通过；依赖异常脱敏测试复现原异常透出；同成员不同key占两席测试实际发现未抛异常，增加锁内去重后通过。先失败日志保留在模块 target（clean会移除），本页记录区别，不伪称每个新增断言都曾独立失败。

现有自动断言覆盖：半开时段/非法区间、明示接受与整数费用、预约终态互斥、URL守卫、MySQL V5重复执行0及独立连接读取、屏障末席两用户竞争、同key重放与异体冲突、权益端口失败回滚、重连状态、接受取消费与重复订单指令防重、教练重叠与相邻区间、当前撤权/跨主体/跨商家、未知依赖与敏感异常、交易失败不释放席位、取消与签到竞争、游标主体绑定、未批政策与期限私教履约、同成员不同key防重复占席。

fixture 权限、商家/账号目录、政策、Rights/Trade 响应均仅测试源码。没有真实 IAM/merchant/trade/rights 联调，没有真实支付/退款，未执行跨模块原子性、前端U或上线L验收。状态与回执/Outbox在本地事务里，fixture计数不作为其他模块写事务回滚证据。

测试只追加合成UUID数据，不 DROP/clean/清表；单 JVM、竞争最多2线程，连接按需关闭。MySQL 初次不存在由 JDBC 的显式本库 createDatabaseIfNotExist 创建，迁移前拒绝未知表。应用测试自身确保迁移已安装，不依赖测试类排序。

独立审查后还覆盖严格版本提供方（同源两次预占再两次履约）、错单/权益绑定、教练归属、主体校验前置、排课详情往返；写入后回执唯一冲突强制真实MySQL回滚，断言两场合计占席1、预约1、Outbox仅两排课+一预约共3、同键回执1。另实际复现并修复 record显示文本导致同key异体误判。Flyway checksum=1511461192，数据库字符集utf8mb4。

暂存检查初次发现生成文件末尾空行和Maven日志行尾空格，已清理；verify-output.log只规范行尾空白，保留全部实质日志。此后暂存diff检查再次确认。
