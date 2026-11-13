# 本地账号会话 Backend

编译目标 Java 21，Spring Boot 4.1.2，Wrapper Maven 3.10.0。默认 profile 无数据库 Bean，仅 GET `/actuator/health` 公开且返回 `{"status":"UP"}`；其余默认拒绝。账号认证仅在显式 `local-dev` 启用，监听 127.0.0.1。

## 私有配置

在当前 PowerShell 进程设置 `JAVA_HOME`（本机既有 JDK21 为 `D:/java_environment`），以及以下环境变量。数据库密码使用隐藏输入或已有安全环境注入，不写入命令参数、脚本、日志或跟踪 YAML。

- `GYM_DB_URL=jdbc:mysql://localhost:3306/gym_dev`
- `GYM_DB_USERNAME`、`GYM_DB_PASSWORD`：适用本地开发凭据。
- `GYM_IAM_HMAC_KEY`：至少 32 随机字节的十六进制编码；跨应用重启保持一致，用于受密钥保护的请求指纹与限流摘要。更换会影响幂等识别，应停机后受控处理，不能使用固定示例密钥。

开发时可将安全随机生成的 HMAC key 保存在已忽略的 `backend/.tools/iam-hmac-key`，收紧文件 ACL 为当前用户可读写后通过环境变量读入。密码不需写入该文件。不要输出这些环境变量或提交本地文件。

实际 MySQL 为 Docker mysql-dev 的 26.7.0；固定 BOM 解析 Connector/J 9.7.0、Flyway core/mysql 12.4.0。Flyway 保留「最高已验证 9.4」警告；不降低验证，不更换数据库服务。本任务以真实隔离库测试和开发库新进程实测证明本次有限功能兼容，不宣称厂商已认证该组合。

## 构建与真实数据库测试

测试要求显式 `GYM_TEST_DB_URL=jdbc:mysql://localhost:3306/gym_dev_test_003b`；该隔离库必须按任务授权预先创建。测试复用上述 DB 用户/密码，并要求 HMAC key；不会回退到开发 URL。URL/schema 守卫仅允许两个指定库，集成测试只允许隔离库。没有数据库配置时测试明确失败，不跳过、不使用 H2。

```powershell
cd backend
.\mvnw.cmd -B clean verify
```

原六项 FoundationBoundaryTest 保留。新增测试覆盖真实 HTTP/MySQL、V1 重复启动、BCrypt cost12、Token 摘要、CLI 子进程及重复账号、幂等/重放、限流、CLOSED/epoch、30分钟/7天边界和并发。Clock 夹具控制时间。测试只删除其私有 HMAC 对应来源限流行；独立随机账号与会话夹具留在测试库，不清库、不删除未知数据。不要把测试配置的 schema 覆盖项用于普通开发启动。

## 逐账号 CLI 与启动

构建后，配置上面的开发环境，使用隐藏控制台输入创建账号：

```powershell
& "$env:JAVA_HOME/bin/java.exe" '-Dloader.main=com.gym.identity.local.LocalAccountCli' -cp target/gym-0.0.1.jar org.springframework.boot.loader.launch.PropertiesLauncher my-local-account
& "$env:JAVA_HOME/bin/java.exe" -jar target/gym-0.0.1.jar --spring.profiles.active=local-dev
```

无 console 时必须显式加 `--password-stdin`，通过受保护的 stdin 管道输入单行密码，例如由父进程隐藏读取后写入 `Process.StandardInput`；不要在命令行使用明文 echo。CLI 本身不打印密码、摘要或连接参数，不接受密码参数。非零退出表示配置、输入或唯一性拒绝。账号名归一化为小写，输入仅 ASCII 字母/数字/`.`/`_`/`-`，长度 3–64；密码 12–72 UTF-8 字节，BCrypt cost12。没有默认账号、共享密码、公共注册入口；CLI 不产生员工、平台或会员授予。

## 四路由

成功信封 `{data,requestId,mode:"SIMULATED",serverTime,contractVersion:"0.1"}`，敏感响应 `Cache-Control: no-store`。Cookie 被拒绝，本地仅 Bearer；这不构成生产 Cookie 方案。

| 请求 | 内容与成功结果 |
| --- | --- |
| POST `/v1/sessions` | 必填 Idempotency-Key；`{credential:{kind:"LOCAL_PASSWORD",login:"my-local-account",proof:"<private-password>"},entry:"user-web",clientKind:"WEB"}`；201 data 含 sessionId、sessionToken、expiresAt、refreshToken、refreshExpiresAt、authnSource:"LOCAL_DEV"、**generation:0** |
| POST `/v1/sessions/refresh` | 必填 Idempotency-Key；`{refreshToken:"<refresh-token>",generation:0}`；201 Token 字段及 generation+1，sessionId 不变 |
| GET `/v1/me/contexts` | `Authorization: Bearer <session-token>`；200 `{accountId,staffScopes:[],entitlementSources:[],asOf}` |
| DELETE `/v1/sessions/{id}` | 有效本人 Bearer；200 `{sessionId,revoked:true}`；他人目标404，旧Bearer401 |

entry 支持 user-web/user-mini/merchant/coach/platform-admin/platform-support，clientKind 支持 WEB/MINI_PROGRAM；它们均不授予角色。多余字段（例如 role/accountId）、缺失字段或非法参数422。错误信封 `{code,message,requestId,retryable,details}` 不含证明、SQL 或账号存在性；错误证明/不存在/禁用统一401 AUTHENTICATION_FAILED。只有验证证明后才可返回403 ACCOUNT_CLOSED。

Access 为32随机字节、30分钟；refresh 也是32随机字节，家族绝对7天，刷新不延期。库中仅保留 SHA256 Token 摘要；请求指纹采用服务端 HMAC，禁止无密钥口令指纹。刷新成功立即更换 access 摘要。旧叶同键同体重试409 REAUTH_REQUIRED，异体409 IDEMPOTENCY_MISMATCH；异键重放401并撤销家族与对应 access。无法重取秘密，不缓存或重发原 Token。账号/来源分别5/30次失败每15分钟，429附 Retry-After，不信任 X-Forwarded-For。

IAM 锁顺序 Account→Session→Family→Node；认证失败限流写入先锁来源再锁账号摘要。预期业务拒绝会提交防护计数/重放撤销，数据库异常全事务回滚。登录幂等范围为账号，刷新也按账号约束（操作类型与登录分开）；不得用同一刷新 key 提交下一代请求。CLOSED/epoch 只经隔离夹具验证，没有注销/全局吊销或角色管理 API。

## 独立进程复核

在已有开发环境中执行 `./tools/verify-local-process.ps1`。脚本使用打包 CLI 新进程创建独立随机测试账号，在 gym_dev 留下本任务 IAM 验证记录；执行登录→上下文→刷新→旧access拒绝→重启→新access/幂等回执持久化→退出→刷新拒绝，再以默认 profile 启动新进程验证 health 和拒绝边界。仅输出检查标签，Token/密码留在内存；finally 只停止自身进程。脚本不清库，日志位于忽略的 target/。

生产 Cookie、微信短信、公共注册/找回/合并、员工与平台授予、会员/健康/账号注销均未实现。后续模块仍需独立批准和验收，不能根据空 scopes 当作已完成业务权限。
