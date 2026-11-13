# 并行业务模块端口契约 v1（协调者唯一维护）

单体分模块、未来统一数据库不变。新模块是供主服务装配的Java库，不是六个独立微服务；本批不改backend/POM/启动配置/现有迁移。每库独立POM，Java21、固定Boot4.1.2 BOM、现有JDBC/Flyway版本；从backend目录使用已有Wrapper -f ../modules/<name>/pom.xml 构建，避免根聚合或共享锁文件。领域包com.gym.modules.<合法Java包名>（如merchantcore，具体见任务Prompt），主服务后续单一集成任务负责装配。

## 可信输入与失败闭合

模块不开放未经鉴权HTTP接口、不自建登录/万能用户/管理员。应用端口接收可信actorId/requestId/idempotencyKey/mode=SIMULATED，身份必须由未来IAM适配器解析。所有者从仓储/StoreDirectory读取，不相信请求传merchantId/ownerId就授予权限。每次资源写事务内复核归属与版本。

AuthorizationPort.authorize(actorId, action, merchantId?, storeId?, resourceOwnerId?)返回ALLOW/DENY/UNAVAILABLE；默认实现DENY或未配置失败，无默认管理员。生产角色/授予尚未批准分支返回POLICY_UNAPPROVED。测试可注入显式fixture权限，必须标明不是运行时批准。受保护服务方法不能只接收一个客户端可伪造role字符串。

StoreDirectory.requireStore(storeId)→{storeId,merchantId,status,version}；AccountDirectory.requireActive(accountId)→{accountId,epoch}，不存在/不活动拒绝。可以各模块本地声明等价类型，不能编译依赖另一个在途模块；集成适配器负责映射。领域接口不能返回可修改的其他模块实体。

RightsPort：resolve(accountId,storeId?,capability)返回独立来源列表及版本；reserve/consume/release以businessKey幂等、请求量/expectedVersion明确；写入者只有trade-core。Booking/attendance只能请求权益命令，不能写卡表。未来适配器必须加入同一本地事务或经批准状态编排，不能持数据库锁调外部服务；本批fixture不能作为跨模块原子性验收。

TradePort：createOrder(product/policy snapshot,beneficiary,mode,key)及query/refundStatus，只由trade-core写资金。其他模块只存引用。未知价格/结算不能默认零费用或零佣金。

事件信封：eventId、type、schemaVersion=1、producer、aggregateId/version、occurredAt（Instant）、merchantId/storeId可选、mode=SIMULATED、payload。生产者独占业务负载，Outbox与源状态同事务，消费Inbox按eventId去重；禁止健康正文/凭据/Token/精确定位入事件/日志。测试分别证重放，不宣称已与真实生产者联调。

规则输入：PolicyProvider.get(policyId,version)必须区分CONFIGURED_FIXTURE/APPROVED_RUNTIME/UNAPPROVED。尚未获用户批准的时间、日卡、配额、处罚、打卡阈值、触达政策无运行时默认；算法允许显式fixture参数测试，但不可将测试值当已批政策。已确认状态/公式照常实现，未知子项不得锁死整个无关模块。

## 版本与事务

各模块自己的异常/错误码至少区分401主体无效、403无操作权、404跨归属/不存在、409版本/幂等/容量竞争、422非法/政策未批准、503依赖不可用。金额整数分或BigInteger/BigDecimal精确算术，时间Instant及显式时区/区间，不采用浮点/隐含30天月。分页稳定键、cursor绑定主体/范围/筛选；敏感输出不缓存。

业务状态及幂等回执同事务，正常失败不产生部分写入；各模块只写自己前缀表。外部结果UNKNOWN不得当失败重发新资金指令。端口失败必须可测试，无静默mock降级。任何契约修改先报告，不由多个会话同步改本文件。

## 迁移与数据库隔离

| 模块 | 生产迁移预留/表前缀 | 唯一测试库 |
| --- | --- | --- |
| merchant-core | V3 / mch_ | gym_dev_test_biz001 |
| trade-core | V4 / trd_ | gym_dev_test_biz002 |
| booking-core | V5 / bkg_ | gym_dev_test_biz003 |
| operations-core | V6 / ops_ | gym_dev_test_biz004 |
| support-core | V7 / sup_ | gym_dev_test_biz005 |
| ranking-core | V8 / rnk_、vis_ | gym_dev_test_biz006 |

脚本放各模块src/main/resources/db/modules/<name>/，不是backend/db/migration；只显式本模块locations的测试可执行，不自动进入主服务或gym_dev。各测试库不存在可创建，禁止连接gym_dev、night001或003b测试库。未知对象/迁移先报告，不DROP库/清库/clean，不修改现有V1/V2。未来统一集成时再按V1–V8顺序装配并真实跨模块验证。

## 验收分层

M=模块业务+真实本模块MySQL/事务/权限端口/测试；I=真实提供方与统一服务集成；U=获批UI后的前端与跨端闭环；L=资质渠道/部署上线。当前只派M层。模块M通过不代表I/U/L通过，消费者fixture模拟与真实提供方必须分别报告。后端/前端入口未接通就不能把需求标已完成。
