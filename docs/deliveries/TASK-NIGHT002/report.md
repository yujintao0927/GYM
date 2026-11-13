# TASK-NIGHT002 交付报告（待验收）

执行角色 EXEC-NIGHT002。起点main：290e098dda56b9fd6fca1b42d75f560f3a07cd3a；受测backend固定003B e20e499，git diff确认无差异，未取NIGHT001源码。

分支：codex/task-night002-identity-regression。
工作区：C:/Users/28795/.codex/visualizations/2026/11/13/01a1d561-11c1-7681-b6d1-9aafb192d8b9/task-night002。
最终SHA为本报告所在交付分支提交，可用 git rev-parse HEAD 复核；完整值及实际推送结果见本聊天最终回报，避免报告内自指哈希循环。origin：https://github.com/yujintao0927/GYM.git。不合并main，不改写历史。

范围仅 quality/identity-regression/**、docs/deliveries/TASK-NIGHT002/**。文件包括Python标准库运行器、私有规则加载入口、只作用于随机账号的Java JDBC夹具、自测/脱敏验证器、README、自动结果、条件矩阵、本报告/账本/问题边界。backend只产生已忽略target构建输出；共享AGENTS、WEB001、协调总台账均未改。

接口/数据库结构变更：无。只启动自身loopback V1进程，URL及schema显式固定gym_dev_test_003b；真实CLI创建随机账号，保留合成IAM数据与私有HMAC对应限流行，不清库、不DROP、不停Docker或用户服务。夹具仅设置自身账号关闭/epoch/禁用、会话或家族过期。未使用gym_dev或night001库。无前端工作，批准图和批准页面范围不适用。

重放命令见工具README。环境：Python3.10.11、Java21、Boot4.1.2、Maven3.10.0；保留原Connector/J9.7.0及Flyway12.4.0组合。用户环境MySQL26.7.0，未改服务版本，Flyway兼容警告保留；未把警告当验证豁免。

命令及实际结果：

- backend目录 ./mvnw.cmd -B -DskipTests package：退出0，BUILD SUCCESS（跳过原测试明确记录，不声称28项重跑）。首次错误cwd及沙箱缓存锁失败均已诊断，未改源码。
- python quality/identity-regression/local-run.py：初版104/104退出0；UTF8扩展曾因工具管道编码失败退出1，修正后112/112退出0；最终修正后138/138退出0，结果以 results.json 为准。
- python -m unittest discover -s quality/identity-regression -p test_tool.py -v：7/7通过，退出0，覆盖正向断言及篡改预期/敏感反射/错误体/缓存策略负例。
- 非法目标受控子进程：明确验证退出2且未启动Java，无数据库连接。
- 最终 evidence/secret/scope 检查与Git结果见最终回报；完整逐条件证据在results.json与matrix.md。

已确认后端新缺陷：无。工具自身编码/并发预期/Unicode脱敏修正见ledger；不得把零发现称为全面安全。并发允许合法不同提交顺序，不为耗时重复压测。剩余覆盖空白和安全边界详见findings.md：特别是精确时间等号、15分钟窗口恢复、SQL断连/崩溃、完整事务审计、存储不变量、未知/错误/禁用凭据的完整非枚举等价性和账号ID一致性专项尚未完成。

真实HTTP/MySQL/打包进程已执行，认证本身仍 LOCAL_DEV/SIMULATED；无生产Cookie、第三方身份、员工或付费授权、注销、健康/支付/前端联调或生产安全验收。私有诊断不交付，不含Token/密码/HMAC/原HTTP body。缺配置或未批政策分支不猜测实现，无自动唤醒/回传/续派。

请用户醒来后将此报告、最终SHA及工作区路径带回COORD-01，由其独立复核与决定验收；本包仅交付待验收。
