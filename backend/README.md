# Backend foundation

Java 21 编译目标；Spring Boot 4.1.2；Apache Maven Wrapper 3.3.4 固定 Maven 3.10.0。

PowerShell（先确认 JAVA_HOME 指向包含 java/javac 的 JDK21；只修改当前进程环境）：

```powershell
$env:JAVA_HOME='D:/java_environment' # 本次验证的本机路径，其他机器使用自身 JDK21
cd backend
.\mvnw.cmd -B clean verify
& "$env:JAVA_HOME/bin/java.exe" -jar target/gym-0.0.1.jar --server.port=8080
```

需要网络代理的环境应使用私有 Maven settings.xml（通过 `-s` 指定），不要提交代理认证或凭据。Wrapper 分发地址及 SHA256 固定于 `.mvn/wrapper/maven-wrapper.properties`。首次运行需要访问 Maven Central。Linux/macOS 使用 `sh mvnw -B clean verify`。

仅 GET /actuator/health 公开，响应严格为 `{"status":"UP"}`。其他路径及 health 的其他方法默认 401/403；无 HTTP Basic、表单登录、开发账号、会话或 Bearer 认证能力。UP 只说明本进程健康，不表示数据库、支付或第三方连通。

MySQL/Flyway 仅为后续已批准技术方向；当前没有 JDBC/MySQL/Flyway 运行依赖、数据库配置、迁移或数据库连接要求。C01 账号及其余业务均未实现，默认拒绝不能证明业务多租户隔离。P01–P08 未升级批准；TASK-003B 由协调者独立解锁。

测试 FoundationBoundaryTest 使用真实 SpringBootTest 随机端口和 JDK HTTP 客户端，验证最小 health、匿名/伪身份拒绝、开发登录关闭及无 DataSource。
