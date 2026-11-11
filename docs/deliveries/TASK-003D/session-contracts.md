# C01细化候选：请求、错误与并发

待验收的设计，不是已发布API。保留[C01](../TASK-002/contracts.md)既有三入口；下述refresh/reauth/closure等为REV-002-02所需的候选路由，需COORD-01确认C01细化版本后供003B使用，不修改原契约、不解锁实现。[模型](identity-model.md)提供字段所有权；[未决](open-decisions.md)列批准门槛。REQ-002/006/013/014/015/085/093/097，D01/D02/D16/D25。

## 共同传输和失败规则

沿用/v1及原信封：成功{data,requestId,mode,serverTime,contractVersion}；错误{code,message,requestId,retryable,details}。mode沿用原枚举但当前无任何模式实现；认证来源另列authnSource，不将本地认证称作真实微信认证。服务端决定mode/来源，不接受客户端升级。ID、枚举、长度、未知字段按锁定schema校验；拒收accountId/role作为登录授予字段。无凭据或已失效401 SESSION_INVALID；归属不符404 RESOURCE_NOT_FOUND；角色不足403 ROLE_DENIED；语法422 INVALID_INPUT；未批准分支422 POLICY_UNAPPROVED；锁超时409 CONCURRENT_CHANGE且retryable=true；适配器/事务依赖失败503 ADAPTER_UNAVAILABLE，不能返回假成功。敏感响应Cache-Control:no-store，日志移除秘密。

写接口继承Idempotency-Key、expectedVersion；登录/刷新首次创建无聚合版本，使用凭据挑战/刷新generation作为期望状态。幂等范围actor（未登录使用已验证identityRef）、operation、mode；同键异体409 IDEMPOTENCY_MISMATCH。不能在认证前仅凭key取回秘密；幂等命中仍检查账号和当前权限。关闭回执为唯一有限例外。有效期、速率阈值、重试回执留存均OD-05，未确认不能声称生产安全已验收。

## 操作表

每行成功均套信封；字段未标?为必填。用例I编号在[矩阵](authorization-matrix.md)反向列出；R为安全判定规则。以下状态码为C01细化建议，非运行证据。

| 编号 / 路由 | 权限与请求 | 成功data | 失败、原子性及并发 / 用例 |
| --- | --- | --- | --- |
| S01 POST /v1/sessions | 匿名可提交credential{kind,proof,challengeRef?}、entry、clientKind；适配器验证主体及重放挑战后检查Account | 201 {sessionId,sessionToken,expiresAt,refreshToken,refreshExpiresAt,authnSource}；Token原值只入受保护响应 | 未验证/不存在/错误/已停用本地凭据统一401 AUTHENTICATION_FAILED，近似一致响应和限流，不泄漏是否有账号；已验证身份确属CLOSED才403 ACCOUNT_CLOSED。Account栅栏内创建会话链；关闭先提交则不发令牌，登录先提交仍被后续关闭废止。重复挑战或请求不造第二链；无法安全重取秘密时409 REAUTH_REQUIRED重做证明。R1/R2；I01/I02/I10/I12/I16 |
| S02 GET /v1/me/contexts | 本人有效Session；无请求角色，entry?仅过滤展示 | 200 {accountId,staffScopes[{grantId,role,merchantId,storeIds,validUntil,version}],entitlementSources[{sourceId,kind,merchantId?,storeId?,capabilities,version}],asOf} | 旧epoch401；各所有者查询失败503，不能把未知当允许；每次读取当前授予和C03投影，不把上下文当最终权限。只含本人必要字段。R1–R5；I01/I02/I03/I04/I05/I06/I07/I08/I14 |
| S03 DELETE /v1/sessions/{id} | 本人会话，Idempotency-Key；expectedVersion针对目标Session | 200 {sessionId,revoked:true} | 非本人404；版本不符409 VERSION_CONFLICT；锁Account→Session→Family同步撤销访问及刷新。已撤销的同一请求可用专用退出回执重放最小结果，不能查询其他Session；无回执的旧Token401。退出与刷新串行，退出后新Token也无效。R1/R2；I09/I15 |
| S04 POST /v1/sessions/refresh | refreshToken、generation；无需有效access但需有效链/账号；Idempotency-Key | 201 同S01 Token字段及generation+1，sessionId不变 | 失效/关闭/过期统一401 SESSION_INVALID。锁Account→Session→Family→Node，消费旧叶与新叶创建同事务；不同key复用已消费叶为REFRESH_REUSE（对外401），撤整family。相同key同体只在有效链且受保护短期回执可验证时返同一替代；无法安全保留秘密则409 REAUTH_REQUIRED，不再次转链。R1/R2；I09/I10/I15/I16 |
| S05 POST /v1/me/session-revocations | 本人近期重新认证；target=ALL，expectedVersion=Account.version，Idempotency-Key | 200 {revoked:true,sessionEpoch}（包含当前Session失效） | 403 REAUTH_REQUIRED；409 VERSION_CONFLICT；锁Account将epoch+1及撤会话链同事务；不撤健康consent，不关闭账号。重放仅专用最小回执；普通请求401。R1；I09/I15 |
| S06 POST /v1/me/reauthentications | 有效本人Session；credential{kind,proof,challengeRef?}，Idempotency-Key；不得绑定另一个account | 201 {reauthProof,expiresAt,sessionId} | 错误证明401 AUTHENTICATION_FAILED；主体不匹配同通用错误；CLOSED/旧Session401；proof绑定account/session/epoch及用途，不能用作access Token。与关闭取Account栅栏，重复key不延长有效期。R1；I10/I11/I16 |
| S07 POST /v1/me/closure-preparations | 本人Session+近期reauthProof；Idempotency-Key、expectedVersion=Account.version | 201 {preparationId,closureToken,impactVersion,impactSummary,retentionNotice,noticeVersion,expiresAt,receiptCredential} | 尚未批准的未结交易/员工交接分支422 POLICY_UNAPPROVED；版本冲突409；依赖不可用503。按所有者取得影响引用并在Account栅栏下形成版本快照，预览不关账号；同键不延长token。receiptCredential须预先安全保存，用途仅S09。R1/R6；I11/I12/I13 |
| S08 POST /v1/me/closures | 本人有效Session、closureToken、confirmed=true、impactVersion、expectedVersion、Idempotency-Key | 201 {closureId,accountStatus:CLOSED,cleanupState:PENDING}；不发新会话 | 无近期验证403 REAUTH_REQUIRED；失效token422 INVALID_CONFIRMATION；版本变更409 VERSION_CONFLICT；影响变更409 IMPACT_CHANGED需重新预览；依赖失败503全回滚。按模型同事务关闭、epoch+1、健康同步全撤权、回执/Outbox。Token同键重放转S09而非绕开关闭鉴权；不同键不二次关闭。R1/R6；I10/I11/I12/I13/I16 |
| S09 POST /v1/account-closure-receipts/query | receiptCredential及原Idempotency-Key；只可匹配预绑定account/operation，不需旧Session | 200 {closureId,accountStatus:CLOSED,cleanupStateAtCommit:PENDING}，只返原最小回执，不暴露实时资料 | 无凭证/不匹配/未提交统一404 RECEIPT_NOT_FOUND，过期同样拒绝；限流；无账号健康/订单查询权限。提交响应丢失可取回；确认失败无成功回执，不能据此推断关闭成功。读已提交回执无需重跑关闭。R6；I11/I13 |

S05是本人全会话吊销；员工撤授由未来授权管理命令更新StaffGrant并递增authzEpoch，平台风险吊销权限与管理路由待OD-02，不开放匿名或客户端自称管理员的吊销入口。健康授权撤回仍C09，不另建IAM授权API。

## 防枚举与跨端适配

先验证身份再允许暴露ACCOUNT_CLOSED；未验证失败采用相同状态、文案、响应形状，不区分未知账号、错误密码、错误第三方code。对IP/设备及归一化主体摘要分层限流，429 AUTH_RATE_LIMITED给retryAfter但不回显主体；阈值OD-05。不要锁死已知账号以供攻击者拒绝服务；请求日志只保留关联ID。证明验证不可用503不尝试本地自动降级，跨来源自动合并禁止。

建议核心使用服务端可吊销随机opaque sessionToken及轮换refresh，不依赖JWT永久声明。Web推荐同站安全HttpOnly Cookie传输（Secure、SameSite及CSRF来源/令牌校验）；小程序推荐Authorization Bearer通过受保护存储。两种传输适配到同一Session判断；Cookie模式不把Token放JS可读响应，Bearer模式不混用Cookie；两种同时出现且不一致拒绝422 AMBIGUOUS_AUTH。CSRF失败403 CSRF_REJECTED；CORS精确来源白名单。实际部署域、存储能力、Token时限仍需实现核查，本文未依赖任何特定框架API或版本能力。

认证候选A：显式本地开发Profile的逐账号口令验证，需受保护密码散列、独立种子账号、无默认共享口令、无万能Token；无第三方账号时Web及小程序开发工具均可走同一核心接口。种子账号角色通过受审查授予流程设置，不能在登录请求传role。非开发环境禁用适配器且启动拒绝误启。只验证本地测试身份，不证明微信或短信身份。

候选B：真实来源适配器，微信小程序code经服务端向官方服务交换且验证应用/主体命名空间；Web选择独立真实登录来源或微信官方适用流程，不能把小程序code直接当Web会话。短信需实际服务和号码验证流程另审。未知提供方行为不假设，接入时以对应官方文档核查交换、重放、UnionID条件、Web差异；本轮没有配置、协议实现或真实身份验证。建议先批准A用于本地开发、B另验收，但OD-01未决，不能自授批准。

## 关闭与交付线性化

CLOSED立即拒所有入口、刷新、静默第三方登录；清理失败为RETRYING/NEEDS_ATTENTION且仍CLOSED。并发读取/导出在响应交付及下载时取得与关闭相同Account栅栏，再由健康所有者检查consent epoch；必须覆盖授权检查到交付的临界段，不能检查后释放锁再无限期发送。关闭先提交则零新交付，读取先完成交付则无法追回；长流分批栅栏，不承诺撤回网络上已送达字节。账号栅栏与外部网络不可无限持有，超时中止交付，由实现专项验证。关闭后内部资金查询、可信回调及退款继续用限定服务主体，不恢复用户会话；未结交易改变impactVersion，确认需重新预览。S08依赖健康同步撤权接口可参与同事务，TASK-006未就绪不能假装关闭闭环可用。
