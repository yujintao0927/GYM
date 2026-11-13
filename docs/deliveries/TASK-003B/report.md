# TASK-003B 交付报告

EXEC-003B，2026-11-13。状态：完整本地账号会话实现已完成验证，提交后仅进入待验收；不自授通过、不解锁其他模块、不合并 main。请用户将本报告、完整最终 SHA 与材料路径人工带回 COORD-01；本聊天没有自动派发或回传工具。

## 输入、起点与工作区

业务5f5e091；TASK-002验收稿c781088、TASK-003D身份草案2ef475e、TASK-003A已验收374a4e9及当前DEC-018–020。创建前核对共享仓库status/分支/origin/历史、任务/会话登记、全部工作区与远端分支，未发现003B已开工分支或工作区；无法全局检索其他聊天，不声称证明其不存在。

- 实际起点：`3c649a1a290b696684980ddfd6c93a26e3efcf84`，创建时 fetch 后 main=origin/main，含本轮DEC、local-development.md、完整计划及Prompt。
- 独立工作区：`C:/Users/28795/.codex/visualizations/2026/11/13/01a1d4c4-dfef-7920-9f27-8b643f826afe/task-003b`。
- 分支：`codex/task-003b-local-sessions`；origin `https://github.com/yujintao0927/GYM.git`。共享 D:/GYM 未切换分支、未改用户文件。
- 本轮按用户补充，完整读取共享目录更新后的计划；当时共享 main 为 `2b48c1e272ca3ab30d1b626b9aabb5b20a973f44`。登录响应增加 generation=0，测试与运行说明同步；未重开任务或改协调文件。
- 适用规则为用户全局规则及用户 `.codex/AGENTS.md`；项目与父目录未发现其他AGENTS.md。凭据仅从规则在进程中提取，不进源码、参数或材料。

## 兼容策略及执行过程

此前检查提交 `dda9197fe3f5dc50e444b4d3d1c0d36c30caee45` 已推送，记录只读探测成功与Flyway版本警告。当时用户选择先由COORD-01确认；本轮用户明确要求执行者自行确认，解除该等待。

执行决定：保持既有MySQL服务和固定Boot BOM，保留警告、保持验证开启，以真实隔离库迁移/事务/并发及开发库新进程验证为本任务有限兼容证据。不得把这些结果表述为厂商认证或生产批准。实际SELECT VERSION()再次为26.7.0，事务隔离REPEATABLE-READ；未改服务器配置。

解析结果：Java21（实际JDK21.0.2）、Boot4.1.2、Wrapper Maven3.10.0；spring-jdbc/spring-tx **7.0.10**、Connector/J **9.7.0**、flyway-core/flyway-mysql **12.4.0**，新增依赖全部由固定BOM管理，无LATEST/SNAPSHOT和未管理依赖。

Flyway实际保留警告：MySQL26.7高于其已验证最高9.4；JDBC连接、V1安装/校验、重复启动及并发均通过。官方边界参照[Flyway MySQL](https://documentation.red-gate.com/fd/mysql-277579322.html)和[Connector/J 9.7手册](https://downloads.mysql.com/docs/connector-j-en.pdf)。本报告区分「实测可用」和「厂商已验证」。

## 文件摘要与实现

- backend/pom.xml：仅新增BOM管理的JDBC、驱动、Flyway core/mysql；未引入H2，无框架整体升级。
- backend/src/main/java/com/gym/identity/local/：BCrypt cost12、规范化/验证、安全随机Token与HMAC；逐账号Provisioner和隐藏输入/显式安全stdin CLI。
- identity/persistence/：显式本地DataSource/schema及未知表守卫、Flyway迁移、JdbcTemplate和事务封装。默认profile不建立这些Bean。
- identity/session/：账号栅栏下的登录、幂等回执、轮换、上下文、本人退出、摘要限流和持久化撤销。
- identity/api/：四路由、无秘密错误信封、no-store、仅精确路由/方法的本地安全链及CSRF例外；其他路径仍走原默认拒绝。Cookie明确拒绝。
- FoundationSecurityConfiguration：仅增加Web应用条件，使CLI非Web进程可启动，原六项测试未删除或削弱。
- application-local-dev.yml、V1__iam_local_sessions.sql、两个identity测试类；README及tools/verify-local-process.ps1。
- 本目录CompatibilityProbe.java保留前次只读探测工具。根.gitignore未修改；已有backend/.tools/和target/忽略规则涵盖私有key、缓存及日志。

## 接口与数据变更

成功信封维持data/requestId/mode=SIMULATED/serverTime/contractVersion=0.1。authnSource仅LOCAL_DEV，entry/clientKind不授角色，多余role/accountId等字段422；错误不含凭据、SQL、堆栈或账号存在性。

| 路由 | 实际行为 |
| --- | --- |
| POST /v1/sessions | LOCAL_PASSWORD，必填Idempotency-Key；201返回sessionId/sessionToken/expiresAt/refreshToken/refreshExpiresAt/authnSource/**generation:0**；同键同体409 REAUTH_REQUIRED，异体409 IDEMPOTENCY_MISMATCH，无重复会话 |
| POST /v1/sessions/refresh | 必填refreshToken/generation/Idempotency-Key；201同sessionId、新Token及generation+1；不延长7天家族；旧access立即失效；同键重试409，旧叶异键重放401并撤家族及access |
| GET /v1/me/contexts | 有效Bearer；200 accountId、空staffScopes/entitlementSources、asOf；匿名/伪造/过期/撤销/CLOSED/epoch不符401 |
| DELETE /v1/sessions/{id} | 有效本人Bearer，200 sessionId/revoked；他人404，旧Bearer401；撤目标会话及家族，不能以回执绕过鉴权 |

登录名仅ASCII字母/数字/._-、长3–64，归一化小写；密码12–72 UTF-8字节、BCrypt12。32随机字节opaque access/refresh，仅SHA256摘要入库；幂等请求指纹使用私有至少32字节随机HMAC key，不保存无密钥口令指纹或原始秘密。登录及刷新幂等分别按账号/操作约束，不允许同账号跨会话复用刷新key绕过。

CLI仅创建普通本地账号，不开放注册HTTP，不内置共享密码或默认账号，不授员工/平台权限。无console须明确--password-stdin，由父进程/受保护stdin提供秘密；密码不支持参数，不输出明文或摘要。私有HMAC key保存于已忽略文件并收紧ACL，应用重启复用该key。

V1仅新增本任务七张IAM表：iam_account、iam_local_credential、iam_session、iam_refresh_family、iam_refresh_node、iam_login_receipt、iam_auth_failure，加Flyway自有历史表。账号/凭据与Token摘要唯一；节点(family_id,generation)唯一；登录回执(account_id,key_digest)唯一；外键均为IAM内部。

主锁序Account→Session→Family→Node。限流先来源、再账号摘要锁；来源不信任X-Forwarded-For。预期拒绝提交失败计数/重放撤销；数据库异常回滚全事务。账号摘要5失败/15分钟，来源30失败/15分钟，429带Retry-After。

gym_dev创建前已核实空表，本轮新增IAM V1及随机新进程验证账号/会话；测试库原先不存在，按授权创建utf8mb4/utf8mb4_unicode_ci。最终两库均只有上述八张表，V1历史success=1；未DROP、清库、覆盖未知表或执行Flyway clean。测试只删除当前私有HMAC对应的已知来源限流行，随机隔离账号/会话保留为本任务夹具，不操作开发库记录。

## 测试：失败、定位、修正及最终结果

| 命令/阶段 | 退出与事实 |
| --- | --- |
| 原始 mvnw.cmd -B clean verify | 0；原六项通过，默认无DB |
| 早期dependency:get未引号-D参数 | 非0；PowerShell拆分参数导致metadata错误，修正引号后解析成功，不是依赖不存在 |
| migration初始红测 | 1；Expected size1, actual0；安装V1后通过 |
| sessions初始红测 | 导入错误修正后运行，因无实现ClassNotFound及预期状态失败；随后真实实现令业务用例转绿 |
| 首轮全测/并发复现 | 1；同键并发登录201/503而非201/409，重复6轮捕获CannotAcquireLockException；定位限流INSERT IGNORE共享锁升级，改为直接取得写锁的upsert，并锁内当前读回执 |
| 刷新限流红测 | 1；预期429实际401；补齐来源失败计数及Retry-After后转绿 |
| 三项边界红测 | 1；跨代复用key预期409实际201、非ASCII预期422实际401、跨账号互相退出预期404/404实际503/404；分别修正幂等检查、原始ASCII校验、所有权判定先于他人Session锁 |
| 独立审查两项红测 | 1；旧叶异键被后代key遮挡预期401实际409、同账号跨会话刷新key碰撞预期409实际201；修正撤销优先级和账号级键约束，审查者复核无新增严重问题 |
| **最终 mvnw.cmd -B clean verify** | **0，BUILD SUCCESS；28项，Failures0/Errors0/Skipped0：Foundation6、LocalSession21（含重复并发）、Migration1** |
| dependency:tree固定includes | 0；版本如上，无覆盖BOM |
| tools/verify-local-process.ps1最终执行 | **0**；四组检查全部PASS，见下 |

28项测试以真实MySQL与随机端口HTTP运行，涵盖CLI子进程创建/重复及无秘密输出、散列/摘要、未知/禁用凭据防枚举、来源与账号限流、伪造身份/字段、本人/他人退出、CLOSED与epoch、可控Clock的30分钟/7天边界、同键/异键并发刷新、登录竞争、退出刷新竞争及重复迁移。没有H2、mock数据库或通过跳过测试制造成功。

独立新进程最终输出仅有：

```text
CLI_FRESH_PROCESS=PASS
HTTP_LOGIN_CONTEXT_ROTATION_OLD_ACCESS_REJECT=PASS
RESTART_PERSISTENCE_RECEIPT_LOGOUT_REFRESH_REJECT=PASS
DEFAULT_PROFILE_NEW_PROCESS_BOUNDARY=PASS
```

该脚本实际执行打包CLI创建随机账号→local-dev登录generation0→本人上下文→刷新→旧access401→停止自身应用并重启→新access和登录幂等回执仍有效→退出→refresh401→默认profile新进程最小health及登录/上下文拒绝。所有自己启动的进程均在finally停止，未停止用户Docker或其他服务。

进程验证早期3次在默认health解析处失败，前序业务/重启检查均已通过；诊断显示PowerShell将vendor JSON内容视为字节数组、逐字节ConvertFrom-Json产生Object[]，改为UTF-8解码后最终全流程退出0。这是验证脚本修正，未削弱health断言或修改health响应。

完整私有日志位于backend/.tools/final-clean-verify.log、.tools/dependency-versions.log和.tools/evidence/，不提交；运行日志位于忽略的target/。报告仅保留脱敏事实，HTTP示例与运行步骤见backend/README.md，Token使用占位符。

## 风险、边界与下游

- MySQL26.7超出Flyway厂商最高已验证版本的警告仍存在；本次实测证明的是该组合上的本任务功能和事务行为，不是所有SQL特性或生产负载兼容承诺。
- 本地Bearer开发方案不等于Web生产Cookie；CSRF仅精确本地路由例外，未来Cookie需单独设计/验收。当前无连接池，限流同来源会串行，适用于本地开发，未承诺生产吞吐。
- CLOSED/epoch通过隔离夹具验证，无账号注销API、经济处置、健康撤权或清理实现；无真实微信/短信、公共注册/找回/合并、员工或平台授予、收费退款、前端。OD-02/03/04未批分支未实现。
- staffScopes/entitlementSources为空体现模块尚未实现；后续授权与权益所有者仍须独立集成，不能把上下文或entry当长期授权。
- 幂等/失败记录的生产保留与清理政策尚未批准；本地测试夹具不清库。HMAC key需持续安全保管；服务运行在loopback，尚无生产TLS/密钥托管方案。
- UX001、QA001继续独立；本任务未修改其目录、总台账、需求或旧交付。后续由COORD-01验收、决定集成与下游安排。

## Git交付

本轮按完整账号会话闭环暂存、提交和推送原任务分支；代码、迁移、测试、运行说明及本报告同一提交。首次上游已由前次检查提交设置，本轮普通push，无强推、改写历史或合并main。最终完整SHA及push结果在本聊天最终回复给出，可用 `git rev-parse HEAD` 与远端分支核对，避免提交内容自引用其SHA。

本报告为待验收材料；用户需人工带回COORD-01，不声称已自动通知。
