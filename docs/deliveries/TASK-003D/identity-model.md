# TASK-003D 身份逻辑模型（待验收）

EXEC-003D，2026-11-11。仅设计，无数据库或认证实现。逻辑数据库占位 `<logical_identity_database>`，不是数据库名称配置，不创建数据库。字段与约束均为供 TASK-003B 审阅的建议，不能据此执行迁移。

## 依据与所有权

业务输入5f5e091；设计输入c7810882c31e842ceb8e998a08e72b8136bc750b，86161fe已集成；技术批准678b873；实际起点9f268b7d0a3bdbc15b2348eb31928653cfc11eb9。以[需求](../../requirements.md)、[协调决定](../../coordination/decisions.md)最新明确状态覆盖历史描述；TASK-002 [领域](../TASK-002/domain.md)、[C01](../TASK-002/contracts.md)、[REV-002-02](../TASK-002/states.md)、[D25](../TASK-002/scenarios.md)为边界。P01–P08未因集成而批准。

REQ-002/006/013/014/015/085/093/097；D01/D02/D16/D25。身份 TASK-003 是下列 IAM 对象唯一写入者；后续003B承接实现需另派发。商家/门店归属只调用TASK-004查询，权益来源只引用C03，不建立第二份会员卡、余额或有效期权威表。健康授权及健康数据唯一所有者仍TASK-006；身份只有关闭编排权。

## 逻辑字段、约束与查询建议

ID均为不透明字符串；时间服务端生成，时间精度与超期边界在实现契约锁定，权限按当前时刻判断，不能依赖延迟定时任务。version为并发CAS，epoch为权限失效代数，二者不可混用。下表非SQL。

| 对象（唯一写者均IAM） | 字段与语义 | 唯一约束 / 索引建议 |
| --- | --- | --- |
| Account | accountId，status=ACTIVE/CLOSED，createdAt，closedAt?，sessionEpoch（非递减），authzEpoch，version；无年龄准入字段 | PK accountId；status/closedAt供清理；CLOSED不可恢复，epoch不回绕 |
| IdentityBinding | bindingId，accountId，provider，issuer，clientNamespace，subjectProtected，subjectLookupDigest，status，verifiedAt，linkedAt，unlinkedAt?，version；第三方证明只在适配器验证后使用 | 唯一(provider,issuer,clientNamespace,subjectLookupDigest)包含关闭占位；accountId/status索引；subject规范化依提供方规则，不以昵称/手机号猜合并 |
| LocalCredential（候选A专用） | credentialId，accountId，loginLookupDigest，verifierHash，algorithmVersion，status，changedAt，failedAttemptRef；不存明文密码 | loginLookupDigest唯一；accountId索引；仅本地身份命名空间，不冒充第三方绑定 |
| StaffGrant | grantId，accountId，merchantId引用，role=OWNER/MANAGER/FRONT_DESK/COACH，scopeType=EXPLICIT_STORES/MERCHANT，status，validFrom，validUntil?，issuedBy，approvedBy?，reasonRef，revokedAt?，version | grantId唯一；accountId/status/merchantId索引；建议同账号商家角色至多一条有效授予，历史另留；需锁内排除有效区间重叠，普通唯一键不足 |
| StaffGrantStore | grantId，storeId引用；显式范围枚举，新增门店不自动加入 | 唯一(grantId,storeId)；写入时及每次使用复核store.merchantId；MERCHANT范围是否允许及新增店继承待OD-02 |
| PlatformGrant | grantId，accountId，permissionCodes，assignedWorkScopeRef，status，validUntil?，issuedBy，version | accountId/status索引；与商家员工分离；无默认健康读取权限；权限清单及授予审批待OD-02 |
| Session | sessionId，accountId，entry，clientKind，authnSource，authnStrength，authenticatedAt，reauthenticatedAt?，accountEpoch，sessionVersion，accessDigest，accessExpiresAt，absoluteExpiresAt，revokedAt?，revocationReason | accessDigest唯一；accountId/revokedAt索引；原始随机会话秘密仅返回一次，日志不可记录 |
| RefreshFamily | familyId，sessionId，accountId，accountEpoch，familyEpoch，status，absoluteExpiresAt，revokedAt? | familyId唯一；sessionId索引；一个会话一条当前链 |
| RefreshNode | nodeId，familyId，generation，tokenDigest，parentNodeId?，state=ACTIVE/CONSUMED/REVOKED，expiresAt，consumedAt?，replacementNodeId? | tokenDigest、(familyId,generation)、parentNodeId非空值唯一；锁family确保最多一个活动叶，不能只靠时间比较 |
| ClosurePreparation | preparationId，accountId，sessionId，sessionEpoch，impactVersion，impactRefs，noticeVersion，reauthProofRef，tokenDigest，expiresAt，consumedAt? | tokenDigest唯一；accountId/expiry索引；预览不改Account状态，不存健康正文或交易金额副本作为权威 |
| AccountClosure | closureId，accountId，state=CONFIRMED，confirmedAt，oldEpoch，newEpoch，impactVersion，noticeVersion，cleanupState=PENDING/RETRYING/NEEDS_ATTENTION/DONE，version | accountId唯一（单向关闭一次）；cleanupState/nextAttemptAt作业索引（nextAttemptAt可在Outbox） |
| OperationReceipt | receiptId，actorRef，operation，mode，idempotencyKeyDigest，requestDigest，resultRef，createdAt，expiresAt，closureReceiptSecretDigest? | 唯一(actorRef,operation,mode,keyDigest)；同键异体409；关闭凭证仅取最小回执；保留期限待OD-03 |
| IdentityOutbox / Audit | eventId，aggregateId，version，type，closureId?，ownerTask，attempts，nextAttemptAt，deliveryState；审计actor/scope/decision/reason/requestId/policyVersion | eventId唯一；投递状态/nextAttemptAt索引；消费方Inbox去重；无凭据、健康正文及完整第三方响应 |

外键为逻辑引用，不赋跨模块写权限。绑定摘要应使用独立受保护密钥的查找摘要，密钥不入仓库；原subject如确需留存则受保护存储，确切保留方式仍OD-03，不声称摘要等于匿名。手机号不天然等于同一人；不同提供方相同subject也不合并。解绑、找回、合并账号没有现成授权，不开放候选接口。

## 不变量与编排

员工授权不产生会员权益；会员到期不撤员工角色。商家与权益模块返回带来源和版本的只读投影，IAM只缓存展示，不凭投影永久授权。staffScopes和entitlementSources不是授权令牌。每次资源使用由所有者复核本人、商家、门店、服务状态、卡/授权当前状态。

关闭锁序继承REV-002-02：Account栅栏→门店服务（涉及时）→order→entitlement→refund/consent等子集。IAM事务中改CLOSED、sessionEpoch+1、撤销Session及RefreshFamily，调用健康RevokeAllConsents同步增加授权epoch，写AccountClosure、Outbox、回执及审计；任一步失败全回滚。各模块只写自身对象。清理异步失败不恢复账号，内部退款继续按原指令处理。实现须确保同一事务参与与读交付栅栏，不能用异步事件替代同步撤授权。

关闭后binding最小占位用于拒绝旧身份登录，不自动删除后重新开通；保留期限/再注册政策待OD-03/OD-04。员工交接、权益经济处置待OD-02/OD-04；不得借关闭自动退款、免债或恢复旧账号。

接口字段消费见[会话契约](session-contracts.md)，判定与反例见[授权矩阵](authorization-matrix.md)，未决见[决策清单](open-decisions.md)。
