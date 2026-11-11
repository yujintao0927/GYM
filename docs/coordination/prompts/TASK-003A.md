# TASK-003A 完整派发Prompt：后端骨架与默认拒绝

你是智慧健身系统TASK-003A执行者，拟EXEC-003A；项目D:/GYM，远程https://github.com/yujintao0927/GYM.git。读取实际适用AGENTS.md（若存在）与用户全局Git规则，先核对分支、status、origin、历史。必须独立工作区、codex/task-003a-service-foundation分支；不在共享D:/GYM切分支，不覆盖用户改动。发现重复执行者先暂停报告，不要求用户证明不存在任何聊天。

输入：需求/DEC-006–009固定5f5e091；已验收TASK-001 f554f75，TASK-002 c7810882c31e842ceb8e998a08e72b8136bc750b，设计合并86161fe。本轮用户明确批准Java+Spring Boot+MySQL+Flyway和基础工程范围，见DEC-013/014。实际起点必须来自包含本Prompt、计划和ACC-003验收的main，并记录真实SHA；不要从早期5f5e091直接初始化。后续业务变更或设计冲突先报告。

读取docs/requirements.md、docs/coordination/{README.md,tasks.md,sessions.md,requirements-trace.md,decisions.md,questions-risks.md,acceptance.md,roadmap.md,log.md}、docs/coordination/plans/TASK-003A.md及TASK-002七份交付。计划与Prompt是本任务完整契约；按计划实施，不改协调台账。先回报独立工作区/分支/起点，再执行。

目标：backend可构建、无数据库可启动，只开放GET /actuator/health最小status=UP；所有其他路径401/403默认拒绝。Java编译目标21、Boot4.1.2、Wrapper Maven3.10.0固定。MySQL/Flyway仅已批准技术方向，本任务不连接数据库、不建迁移、不要求数据库名；暂不加入触发DataSource配置的运行依赖。

修改仅backend/**、docs/deliveries/TASK-003A/**，根.gitignore仅任务构建/私有配置忽略项。交付POM/Wrapper、GymApplication、FoundationSecurityConfiguration、application.yml、真实上下文HTTP集成测试、backend/README和report.md，具体路径及签名见计划。不得初始化前端、实现登录/万能令牌/会员权益/退款/AI/设备，也不把P01–P08升级批准。账号业务C01仍未实现，不伪造成功。

环境核对：协调者只探测到Oracle Java启动路径，版本未核实且没有全局mvn。执行者核实JDK21及javac；缺失时可用独立工作区内经官方源及校验和核实的便携JDK，忽略工具二进制，不改系统PATH或安装全局软件；网络或工具阻塞如实报告，不跳过测试。依赖和Wrapper只从可信官方/中央仓库取用，不写凭据。未知外部配置不猜测不采购。

验证必须覆盖：无DB启动；health最小且不泄密；匿名/伪Bearer/伪入口/跨租户参数不开放业务；actuator/env/configprops及开发登录入口不可用；无默认生成密码。先观察有效失败再实现安全规则，执行backend/mvnw.cmd -B clean verify与实际新进程HTTP探测；记录命令/退出码/关键结果，不能只凭构建成功验收。不要运行TASK-001/verify.py作为当前main检查器。

共享构建/安全入口由你唯一维护；不要提前做数据库迁移或实现别人的模块。接口或范围变更先报告协调者，不跨界写文件。git diff --check，检查实际diff、敏感信息和产物忽略；只暂存本任务，验证后git add/commit/push，首次推送设上游，不强推、不改写用户历史；推送失败保留提交并说明原因。

结果写docs/deliveries/TASK-003A/report.md：编号、输入及起点、目标/摘要、分支与提交和推送、工作区及文件、版本及来源、命令结果、未完成/模拟/预留/真实项、风险、接口数据变更、后续依赖及材料路径。业务均未实现，不能将默认拒绝当多租户业务验收；不得自授验收或解锁TASK-003B。

当前没有自动派发/回传工具。请用户将最终报告、最终SHA和路径带回COORD-01聊天；不要声称已经自动通知。
