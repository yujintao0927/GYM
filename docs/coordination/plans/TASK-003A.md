# TASK-003A Service Foundation Implementation Plan

> 执行方式已由用户指定：独立执行工作聊天，协调者不写业务代码；不要改用子代理。执行者使用superpowers:executing-plans逐项实施，适用技能与用户明确授权冲突时以用户要求为准。

**Goal:** 无数据库即可构建并启动的服务骨架，只有最小健康检查开放，未授权路径默认拒绝。

**Architecture:** 单体分模块方向，当前一个backend Maven工程，业务模块后续分别实现。骨架不发会话、不提供伪身份，不把健康检查当业务完成。

**Tech Stack:** Java编译目标21、Spring Boot 4.1.2、Maven Wrapper 3.10.0；MySQL/Flyway已批准方向但本任务不连接或执行迁移，暂不加入要求DataSource的运行时依赖。

**Spec:** 已验收设计c781088，用户DEC-013批准的基础范围；本计划只采纳结构和默认安全边界，不实现P01–P08政策提案。

## Global Constraints

- 独立工作区和codex/task-003a-service-foundation，不在共享D:/GYM切分支。
- 只改backend/**、docs/deliveries/TASK-003A/**；根.gitignore仅增加本任务构建/本地私有配置忽略。
- 不连接数据库、不建迁移，不安装系统软件、不改全局PATH；如JDK缺失可使用任务工作区内经官方来源及校验和核实的便携JDK21，工具目录忽略不提交，工具下载失败报告环境阻塞。
- 无凭据、真实业务路由、登录/假令牌、付费权益、支付AI设备或前端工程；不改协调台账/旧交付。
- 完整账号/会话是TASK-003B，当前安全过滤器只拒绝，不能称C01已实现。

## Review Focus

- 无数据库配置也能启动，测试进程不访问localhost:3306。
- /actuator/env、/actuator/configprops及未知业务路径不可公开。
- 伪Bearer、entry=platform-admin等客户端输入不获得权限。
- 健康响应不含env、用户名、栈、凭据、数据库或第三方连通性声明。
- 开发默认密码或临时permitAll不能遗留为后续业务默认入口。

## 文件与接口

Create backend/pom.xml、backend/mvnw、backend/mvnw.cmd、backend/.mvn/wrapper/maven-wrapper.properties：固定版本，禁止动态LATEST/SNAPSHOT；Wrapper来源和分发校验值记录在报告，验证实际运行Maven3.10.0。
Create backend/src/main/java/com/gym/GymApplication.java：SpringBootApplication启动类。
Create backend/src/main/java/com/gym/foundation/security/FoundationSecurityConfiguration.java：SecurityFilterChain foundationFilterChain(HttpSecurity http)，仅GET /actuator/health允许，其余denyAll，无内置开发账号/HTTP Basic或表单登录；禁用生成默认用户的自动配置，合理记录原因。
Create backend/src/main/resources/application.yml：应用名gym，management仅暴露health、show-details=never；不包含DB、支付或AI配置。
Create backend/src/test/java/com/gym/foundation/FoundationBoundaryTest.java：真实Spring上下文的HTTP集成验证，不只测试配置字符串。
Create backend/README.md及docs/deliveries/TASK-003A/report.md：构建启动、工具版本、默认拒绝边界和未实现项。

Produces：GET /actuator/health→200，JSON最小status=UP且无详情；其他请求401或403。没有任何/v1成功业务路由。此为运维接口，不强行套用支付mode或宣称某项集成可用。

## Task 1：可构建启动和默认拒绝的一体交付

- [ ] 在独立工作区核对基线、分支、远程、可用java/javac，先报工作区和起点SHA。参考官方资料核实工具，不根据Oracle javapath存在就推断JDK21可用。
- [ ] 建立POM、Wrapper、空启动入口和必要测试依赖。用Java21编译目标，所有直接依赖通过固定Boot父POM管理，记录resolved versions；不安装DB依赖或迁移。
- [ ] 编写FoundationBoundaryTest（使用Boot4对应官方测试API，不猜旧包路径）先观察安全测试失败：healthIsMinimalAndAvailable断言GET health为200且status=UP、无components/details；unconfiguredApplicationStartsWithoutDatasource验证无DB配置上下文启动；protectedPathsDenyAnonymous遍历/v1/me/contexts、/v1/orders、/actuator/env、/actuator/configprops断言401/403。
- [ ] 增加forgedIdentityDoesNotGrantAccess，用任意Bearer、entry=platform-admin、merchantId/storeId请求上述业务路径仍401/403；noDevelopmentLoginExposed验证/login不提供登录表单或可用账号入口；healthDoesNotLeakConfiguration检查无凭据/环境/健康数据字段。
- [ ] 实现最小安全与health配置，使测试通过；不得为通过测试开放业务路径。预期失败若是工具/依赖问题先诊断，不算需求测试的红灯证据。
- [ ] 在backend运行 .\mvnw.cmd -B clean verify，退出0且所有测试通过；再打包产物启动一个新进程，用实际HTTP请求检查health和拒绝路径，停止自己启动的进程。不要停止用户其他服务；端口被占用可通过临时启动参数选空闲端口并记录。
- [ ] 记录java/javac、Wrapper实际Maven及Boot版本；保存有效POM/依赖树摘要或报告（不提交敏感环境变量、缓存或二进制）；检查没有启动时MySQL连接尝试、生成默认密码或隐藏外部调用。
- [ ] git diff --check、检查范围、敏感信息与忽略规则，仅暂存任务文件，提交完整修改单元并推送上游。报告真实结果；不合并main、不自授验收。

自审：此任务覆盖基础可运行性及五类默认安全失败条件；不会覆盖业务隔离、真实账号、退款、跨端联调或上线安全验收，均保留后续任务。
