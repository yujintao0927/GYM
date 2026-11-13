# TASK-BIZ001 应用与端口契约

包根 com.gym.modules.merchantcore；应用入口 application.MerchantService。api.MerchantApi 内为不可变 record DTO，错误 MerchantFailure.status()/getMessage()。没有 HTTP；Context.actorId 必须由可信 IAM 解析，不能来自客户端 role/merchantId；requestId/key 非空，所有业务写均在本地事务中重新授权、查归属、查版本和幂等回执。

## 精确 Java 用例

以下类型均来自 MerchantApi，Role 来自 domain.EmployeeGrant；时间 java.time.Instant，集合 java.util.Set。

| 签名 | 边界/action |
| --- | --- |
| Result submitMerchant(Context c,String name,String evidence) | merchant.submit；资料为私有引用；PENDING/version0 |
| Result reviewMerchant(Context c,String merchant,long expected,boolean approve) | merchant.review；必须额外平台来源 ALLOW；PENDING 单次终审 APPROVED/REJECTED |
| Merchant merchant(Context c,String id) | merchant.read；仓储查 applicant/merchant，再授权；资料只授权返回 |
| Result createStore(Context c,String merchant,String name) | store.create；商家须 APPROVED；不授予任何员工新门店权限 |
| Result updateStore(Context c,String id,long expected,String name) | store.update；仓储归属，不支持改商家 |
| Result deleteStore(Context c,String id,long expected) | store.delete；保留审计/授权历史的逻辑删除；当前再授权后可重放原回执；普通读404 |
| Store store(Context c,String id) | store.read；无期限时 end/graceEnd=null，不代表正常 |
| StorePage stores(Context c,String merchant,int limit,String cursor) | store.list + 每项store.read；limit1..100；id稳定排序，cursor绑定actor/merchant/active筛选；每页重新授权 |
| Result setTerm(Context c,String id,long expected,Instant end,Instant grace) | store.term + 平台来源；显式归一端点，end<=grace；不推导日历、不收费、不补偿权益 |
| Result grant(Context c,String merchant,String account,Role role,Set<String> stores) | staff.grant + 平台来源 + 批准GrantPolicy；目标账号当前活动；每店仓储归属一致；immutable显式集合 |
| Result revoke(Context c,String id,long expected) | staff.revoke + 平台来源；查授权归属再决策；当前version+1，立即不再放行 |
| Grant employeeGrant(Context c,String id) | staff.read；含授权版本/撤销状态/显式门店快照；无会员信息 |
| Decision currentStaffDecision(String account,String merchant,String store,Role role) | 可信 IAM 内部提供方查询；复核当前活动账号、门店归属及未撤销显式角色授权；无角色自动继承，无平台或付费资格 |
| StoreSnapshot requireStore(String id) | 实现 ports.MerchantPorts.StoreDirectory；可信内部目录提供方；只返storeId/merchantId/status/version；缺期限PENDING_POLICY |

Context(actorId,requestId,key)；Result(id,version)；Store(id,merchantId,name,deleted,end,graceEnd,version)；Merchant(id,applicant,name,evidenceRef,state,version)；Grant(id,merchantId,accountId,role,stores,revoked,version)；StorePage(items,nextCursor)。分页游标为无权限意义的opaque编码，不是能力凭证，伪造位置不能跳过当前资源授权；增加数据不承诺跨页数据库快照。

示例（均合成）：submitMerchant(c,"Fixture Gym","private-ref") → reviewMerchant(c2,id,0,true) → createStore(c3,id,"Fixture Store")；对结果storeId用新的Context调用setTerm(c4,storeId,0,normalizedEnd,normalizedGraceEnd)。每次不同命令须用不同key，重试须保留key与原请求体。不能用私有引用作为公开附件URL。

## 外部依赖及未批政策

ports.MerchantPorts 内：AuthorizationPort.authorize(String actorId,String action,String merchantId,String storeId,String resourceOwnerId) → Decision(ALLOW/DENY/UNAVAILABLE)；AccountDirectory.requireActive(String accountId) → ActiveAccount(accountId,epoch)；PlatformPermissionSource.authorize(String actorId,String action) → Decision；GrantPolicy.metadata() → PolicyMetadata(id,version,status)，mayGrant(String actorId,Grant proposed) → Decision。StoreDirectory.requireStore(String storeId) → StoreSnapshot(storeId,merchantId,status,version)。本地等价类型，无跨在途模块编译依赖。

PolicyStatus=CONFIGURED_FIXTURE/APPROVED_RUNTIME/UNAPPROVED。默认6参数MerchantService构造器拒绝fixture。7参数构造器最后boolean allowFixturePolicies必须显式true才接受fixture，测试独占fixture实现均在src/test；运行库不提供批准策略或管理员。未知/未批/无效版本返回422 POLICY_UNAPPROVED，来源不可用503 POLICY_UNAVAILABLE。批准的生产策略仍由OD-02责任方提供。Grant事件保存批准来源与版本，不能把布尔开关当政策批准。Policy id只允许安全短标识，不含正文。

Authorization DENY=403 ACTION_DENIED，未知/异常=503 AUTHORIZATION_UNAVAILABLE；Account不存在/不活动=401 SUBJECT_INVALID，异常=503 ACCOUNT_UNAVAILABLE；不存在/跨门店商家归属=404 RESOURCE_NOT_FOUND；版本/异体key/状态竞争409；非法端点/授权集合422。数据库失败503 PERSISTENCE_UNAVAILABLE，锁竞争409 CONCURRENT_CONFLICT；不向调用方附原始SQL/依赖异常cause。所有普通业务异常回滚，不采用003B防御计数的预期异常提交方式。

## 状态、存储与事件

ServiceTerm.at(now)：now<end=NORMAL，end<=now<graceEnd=GRACE，now>=graceEnd=DISABLED；端点完全由调用方显式提供。本算法不批准P01自然日/时区细节。30天提醒、15天宽限主体保留，上游精确归一与运营投递待集成；完整能力矩阵P04不在本模块猜默认。

V3__merchant_core.sql：mch_mutex（模块事务串行栅栏）、mch_merchant、mch_store、mch_grant、mch_grant_store、mch_receipt、mch_audit、mch_outbox。外部账号仅ID，无外部FK/复制表。幂等唯一键actor+key，fingerprint包含operation及全部业务参数；结果、状态、审计、Outbox同事务。稳定删除保留人员授权和历史，删除门店在当前判断中拒绝。

实际Flyway checksum=-407201789；SQL SHA256=9178FA92EC8DE0AFE44A4443B0F93C0B9F314186CC1CE6BF03EFEC1C31CCEA7E。

事件表列为冻结信封的snake_case映射：event_id/type/schema_version=1/producer=merchant-core/aggregate_id/aggregate_version/occurred_at/merchant_id/store_id/mode=SIMULATED/payload。type为成功写action名称；普通payload={"changed":true}，授权payload示例={"policyId":"fixture-subset","policyVersion":1,"policyStatus":"CONFIGURED_FIXTURE"}。不带凭据、证件/健康正文或附件URL；真实提供方未联调，Outbox未投递，不代表实际渠道。

后续装配、事务和失败补偿约束见integration.md。
