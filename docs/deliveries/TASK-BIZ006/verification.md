# 验证记录

从本 worktree/backend 执行 `.\mvnw.cmd -f ../modules/ranking-core/pom.xml -B clean verify`。

环境变量 RNK_DB_URL 必须严格等于 jdbc:mysql://localhost:3306/gym_dev_test_biz006；GYM_DB_USERNAME、GYM_DB_PASSWORD由进程环境私下注入。不得把密码写入命令参数/报告。可执行本目录 verify.ps1。MysqlFixture仅测试源码可见，可创建不存在的唯一测试库，保留既有数据，未知表拒绝；没有DROP/clean/清表。

证据：red.txt保留缺失规则断言失败；red-expansion.txt保留更正/地区端口尚不存在的编译失败；verify.txt为最终构建/真实MySQL输出，unit.txt为纯领域阶段结果。第一次调用Wrapper工作目录错误未当作规则RED；已改为backend目录。持久化测试在重复运行时发现全局榜残留fixture使“空榜”假设失效，改为断言当前合成主体没有擅自参榜，保留数据库而不清表。编译中的误替换read回执变量已修正。

真实 MySQL 26.7.0；Flyway未验证该版本的警告保留，不是生产兼容保证。V8 Flyway checksum=1441057216；脚本 SHA256=f4b0aae75189627f73ea9ba564c3f933c9ce75135cba8356b59b1ceb9bebbbf5。

断言覆盖：重复迁移0、错误库拒绝、人工入场/QR单次挑战、同key重放、重复在场、跨店拒绝、撤权后读取拒绝、权益失败零Visit、双线程入场只一获胜、签退与更正同版本只一获胜、原入场回执不随签退变化、重连接历史、原始手环异体/事件去重/日去重、默认不参与、未知政策拒绝/原始PENDING保留、地区授权拒绝/月限、到店投影/申诉审核。外部权限/设备/权益均显式测试fixture；不证明真实提供方。

限制：未知表守卫存在但尚无对未知表的自动故障注入；未做进程强杀/投递崩溃恢复/大规模负载；跨请求榜单分页漂移、完整异常成绩改判、更多跨主体/跨商家权限矩阵需后续加固。只报告实际断言，不按测试数量认定所有复合REQ完成。

最终命令退出0：20项测试（ModuleMysqlTest14、RankingMysqlTest3、RankingRulesTest3）。依赖精确解析见dependencies.txt：spring-jdbc7.0.10、Connector/J9.7.0、Flyway12.4.0。独立审查指出的旧快照与内部事件键占用风险已改：锁后当前读、源限定和保留前缀拒绝，测试中含跨主体设备与保留前缀拒绝。
