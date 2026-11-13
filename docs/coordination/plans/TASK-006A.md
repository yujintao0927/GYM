# TASK-006A 本人三类记录完整后端闭环实施计划

> 执行者使用executing-plans在独立工作聊天完成一个整体修改单元；协调者不写业务代码，不按单接口拆分新任务。

**Goal:** 本地会话认证用户可真实持久化、分页查询、修正、删除三类本人手动记录，附权限/事务/迁移/重启测试。
**Architecture:** identity独占认证，health独占记录/幂等表；复用同一MySQL与事务，增加V2 HLT迁移。Web仍独立进行原视觉/前端任务，互不等待或改文件。
**Tech Stack:** 保持003B已实测Java21/Boot4.1.2/Maven3.10.0/JDBC/Flyway版本，不引新中间件或整体升级。
**Spec:** health-records-contract.md v1、当前需求免费手动/历史及ACC-009身份实现；未批AI、教练授权、付费及注销全部排除。

## 边界、文件及所有权

只修改backend/**及docs/deliveries/TASK-006A/**；不改web/mini/其他交付/协调文件/root配置。health/{api,record,persistence}分别放控制器、业务/DTO、DAO；测试放相应health目录。新增db/migration/V2__hlt_manual_records.sql；禁止改V1及其校验和。可最小扩展identity只读主体解析接口、数据库允许schema/对象清单和迁移测试，必须保留原28测试及其语义，不重写IAM表。

新表hlt_manual_record（ownerAccountId外键/索引、kind、类型化字段/受约束payload、时间、version）和hlt_record_receipt（actor/操作/键摘要/指纹/记录id，不存正文）；具体可维护字段由实现者确定，不能混成任意JSON未验证自由字段。不记录健康正文或Token到日志，不复制资料给客服/商家。

开发只gym_dev；新增隔离测试库gym_dev_test_006a不存在可创建，已有未知对象先报告，不DROP/清库/clean。003B原测试库继续保留，V2随本工程迁移升级时兼容原IAM；MysqlMigrationTest由仅1项改成精确V1/V2成功和重复执行0，并明确V1 checksum保持；不得以放宽到“有一些迁移”掩盖错误。

## 一个任务的实施步骤

- [ ] 核对独立分支/起点和数据库归属，读C09-MANUAL v1及现有SessionService/Repository的事务语义；设计最小身份主体解析与health写的同事务边界，不另做第二套认证。
- [ ] 先写真实MySQL/HTTP失败测试：三类型CRUD、类型/长度/小数/时区/未知字段、本人/他人/未认证/过期、旧游标跨主体与筛选、默认profile拒绝、重复POST及冲突PATCH/DELETE、DB失败回滚；用固定Clock及并发屏障，不用长sleep。
- [ ] 增V2记录/回执表与查询索引、DTO及精确校验，schema白名单加入已知HLT表和专用测试库，不允许全库通配跳过检查。保留Flyway版本警告和cleanDisabled，真实26.7兼容按本任务再验证。
- [ ] 实现五类操作POST/list/detail/PATCH/DELETE及所有者约束、稳定游标、HMAC幂等、CAS和不可恢复的单条正文删除。保持每个必要权限在服务器执行，不靠前端隐藏。
- [ ] 认证提取、Record/Receipt及更新同事务；测试原会话退出/epoch撤销与写竞争，已提交写允许保留、拒绝后无新写。预期校验失败不得因沿用IAM事务catch策略而提交部分health写。
- [ ] 用原样已有28测试+新增health测试全量clean verify，两个授权测试库各自具名，不能接不到DB就skip/H2代替。对V1校验和、V2重复启动及未知对象守卫给证据。
- [ ] 新进程用CLI合成账号登录，新增三类→查询/过滤分页→修改版本→停止自身服务重启→仍可查→删除→重放旧POST不恢复；另一合成账号对详情/改/删/游标全部拒绝。默认profile再次只health公开。
- [ ] 报告解析版本/迁移/范围、命令结果、前后状态及未实现项，不输出真实凭据/Token/健康内容。diff/敏感检查通过，按完整可理解修改单元提交推送，不每小改机械提交，不混用户工作。

## 验收重点与限制

范围包括代码、迁移、测试、运行说明和新进程证据一并交付。实际MySQL隔离、CAS并发、幂等及跨账号拒绝缺任一关键证据不得宣称完成。CRUD是本人免费手动记录，不是热量营养估算/趋势、AI、教练查看、付费资格或个人排行榜。记录删除不代表注销清理/商业保留政策完成。

同一任务独占backend全部共享配置；若发现还有后端执行者并行改同文件先报协调者，不抢迁移编号。Web/MINI保持原mock契约，后续记录真实联调另组织，不能跨范围替前端改代码。
