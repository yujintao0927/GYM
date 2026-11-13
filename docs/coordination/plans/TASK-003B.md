# TASK-003B 本地账号会话闭环实施计划

> 执行方式：独立工作聊天使用executing-plans，协调者不写业务代码；沿用适用技能和用户Git规则，不改用子代理或新建细碎任务。

**Goal:** 一个任务交付本地逐账号配置、登录/刷新/退出/本人上下文、持久化及安全集成测试。

**Architecture:** Java单体中identity模块唯一写IAM数据，已有health骨架保持；只在显式local-dev启用本地认证。外部认证适配接口可保留，但不实现真实来源或角色/权益服务。

**Tech Stack:** Java21/Boot4.1.2/Maven3.10.0保持；新增JDBC、MySQL Connector/J、Flyway core及MySQL支持，优先固定Boot BOM管理的解析版本；BOM未管理者按官方兼容资料选定显式版本并记录，不使用LATEST/SNAPSHOT。不自行更新整个框架。

**Spec:** 当前DEC-018–020、TASK-003D候选S01–S04/C01、已验收003A；本计划只批准该开发期子集，其他OD/P提案不实现。

## 约束与文件

允许backend/**、docs/deliveries/TASK-003B/**，根.gitignore仅本任务本地秘密/产物忽略；不改协调文档/旧交付。只在gym_dev新增IAM初始表，首个V1版本唯一归本任务；本地测试gym_dev_test_003b不存在可创建，现有对象须先核实归属。禁止DROP库、清未知表、Flyway clean、非项目数据重置；不得把SQL测试凭据打印/跟踪。

修改backend/pom.xml、安全配置及README；新增backend/src/main/java/com/gym/identity/{local,session,persistence,api}/下聚焦类和backend/src/main/resources/db/migration/V1__iam_local_sessions.sql、application-local-dev.yml；测试放backend/src/test/java/com/gym/identity/。类建议LocalAccountProvisioner、LocalAccountCli、LocalCredentialAuthenticator、SessionService、SessionRepository、SessionController及DTO；具体小类拆分可常规调整，不要一个万能类。

数据：Account(id,status,sessionEpoch,version)、LocalCredential(accountId,loginNormalized,passwordHash)、Session(id,accountId,accountEpoch,accessDigest,accessExpiresAt,absoluteExpiresAt,revokedAt,version)、RefreshFamily/Node（digest/generation/consumed及旧键摘要）、幂等最小回执/失败限流计数；由实现者明确字段、唯一约束与锁顺序并测试。无明文password/access/refresh入库。对象/表名不与未来商家/健康/交易模块抢所有权。

## 接口锁定子集

所有成功沿用{data,requestId,mode:SIMULATED,serverTime,contractVersion}；authnSource=LOCAL_DEV明确本地开发身份，不声称微信认证。错误信封不含秘密、堆栈、SQL或账号存在性。

- POST /v1/sessions：JSON {credential:{kind:LOCAL_PASSWORD,login,proof},entry,clientKind}，Idempotency-Key必填；201 data={sessionId,sessionToken,expiresAt,refreshToken,refreshExpiresAt,authnSource}。entry不授角色；禁止客户端传accountId/role来授予。错误证明/不存在/禁用凭据统一401 AUTHENTICATION_FAILED；验证证明后确认账号关闭才403 ACCOUNT_CLOSED。重复同键同体不多发会话，无法安全重取秘密409 REAUTH_REQUIRED；同键异体409 IDEMPOTENCY_MISMATCH，不存原文证明。
- POST /v1/sessions/refresh：JSON {refreshToken,generation}，Idempotency-Key；201同类Token及新generation、sessionId不变。锁Account→Session→Family→Node，旧叶消费与新叶同事务；不同key重用旧叶401并撤family；同key重试不生成第二叶，返回409 REAUTH_REQUIRED而非伪造可重取明文。过期/关闭/撤销401。刷新后旧access失效，不延长family7天上限。
- DELETE /v1/sessions/{id}：Bearer本人会话，只能撤本人目标Session并撤其refresh家族；200 data={sessionId,revoked:true}。他人目标404；已失效旧Bearer401，不为最小回执绕过鉴权。不实现平台风险吊销和员工管理。
- GET /v1/me/contexts：有效Bearer，200 data={accountId,staffScopes:[],entitlementSources:[],asOf}；不伪造尚未实现角色/卡。匿名/伪造/过期/撤销401。所有其他业务路由保持拒绝，切入口/伪租户/伪role不获权。

account CLOSED与epoch拒绝通过隔离测试数据验证，无关闭API或经济处理实现。过期依据服务端时钟，测试注入Clock而非sleep半小时。所有参数按DEC-020；限流失败窗口/账号摘要与来源分开、429带Retry-After。

## 一个交付单元内的实施步骤

- [ ] 核对独立工作区/版本/来源，明确数据库连接变量名；用适用全局凭据只置当前进程/私有忽略文件，不写tracked YAML。查询实际MySQL版本/现有schema，验证JDBC/Flyway能连接26.7.0；不兼容先报告，不降低校验或改服务器。
- [ ] 先写真实MySQL集成测试及有效失败，覆盖迁移初装/重复启动、散列/摘要持久化、CLI账号重复与秘密不输出、成功登录和防枚举、限流、本人/他人退出、关闭/epoch/过期、刷新单键/异键重放和并发。配置隔离库标识，测试只删自身已知夹具记录，不能误连gym_dev后清库。
- [ ] 实现初始IAM迁移、repository及事务服务；LocalAccountProvisioner.create(login,char[] password)由显式local-dev CLI调用，密码交互隐藏输入（无console须明确安全stdin方式），不放命令参数/日志。无内置默认账号，不开放公共注册或给CLI账号平台角色。
- [ ] 实现四路由及Bearer校验，local-dev仅绑定127.0.0.1；登录/刷新仅该profile开放且受限流，其他profile不注册本地认证、保持默认拒绝。敏感响应no-store，禁止日志记录凭据/Token。CSRF只针对明确不使用Cookie的本地Bearer认证路径作精确处理，不能全局关闭未来Cookie保护。
- [ ] 用可控Clock验证30分钟与7天边界；并发两刷新同叶验证无双活动叶/重复会话，退出与刷新竞争后不能留有效新Token。测试实际DB事务，H2或mock不能代替MySQL兼容和并发证据。
- [ ] 运行backend/mvnw.cmd -B clean verify（私有settings按环境显式传入），保留全部旧骨架回归；测试配置与运行local-dev分离，默认无DB仍可启动且health最小。原测试已假设匿名可拒绝，可新增profile专用测试，不为过关删除/削弱旧用例。
- [ ] 新进程local-dev实际配置独立测试账号，走登录/上下文/刷新/旧Token拒绝/退出/再次刷新拒绝，重启确认持久化；普通无local-dev新进程仍只health公开，DB/Token不入输出材料。停止自己启动的进程，不停用户服务。
- [ ] 记录依赖解析版本、迁移验证、表归属和测试命令/结果、接口示例（只用占位Token）；diff/敏感扫描、范围、忽略规则检查通过，整体commit/push并提交完整报告。

## 重点审查与出口

账号闭环全部功能/迁移/测试一起验收，不按文件拆任务。失败输入、伪身份、Token重放、关闭epoch和进程重启必须有证据；没有真实DB或并发测试不能宣称完成。健康、员工/门店权限、生产Cookie和真实身份仍未实现。完成只进入待验收，不自行解锁其他模块；UI/QA当前任务不等待本任务，也不能修改backend。

## 秘密与配置补充

连接变量统一GYM_DB_URL/GYM_DB_USERNAME/GYM_DB_PASSWORD；只在当前进程或忽略文件设置。测试与运行URL必须分别显式指向gym_dev_test_003b和gym_dev，测试启动校验schema名与环境，不可从开发连接静默降级。幂等请求指纹不得保存无密钥SHA256(password/body)形成比BCrypt更弱的离线口令校验旁路；使用服务端密钥HMAC等可验证的安全指纹方案。若用本地密钥，安全生成至少32字节并仅存私有忽略配置/环境，重启持续可用，不写日志/参数或固定种子；不得缓存原始口令或Token来实现幂等。

密码存储依据：[Spring Security Password Storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)、[BCryptPasswordEncoder API](https://docs.spring.io/spring-security/reference/api/java/org/springframework/security/crypto/bcrypt/BCryptPasswordEncoder.html)，2026-11-13核查。cost12是本项目开发选择，不冒充官方唯一推荐值；生产仍需按实机性能和安全核查。
