# TASK-BIZ002 应用与集成契约

这是计次权益与模拟交易的本地 Java 库，当前没有 HTTP/启动应用。冻结公共语义不变；未声称全量 TIME 生命周期或真实渠道可用。

## 精确 Java 入口

包 `com.gym.modules.tradecore.application.TradeService`，构造参数 `TradeDatabase, TradePorts`。除查询外返回 String 回执 ID；所有公开用例由 TransactionTemplate 包裹。参数中的 actor 必须由未来 IAM 适配器解析，不能直接信任客户端字符串。键为 actor/operation/key；长度前缀 SHA-256 请求摘要，同键异体409；回放前重新检查当次授权。

- `publish(String actor,String key,String store,String kind,String formula,long price,long purchased,long gift,String policyId,String policyVersion)`：不可变交易商品快照，kind=MEMBERSHIP/COACHING/PERSONAL；PERSONAL store 必须 null。COACHING 是供测试/适配器导入的交易快照，不拥有课程目录配置权。COUNT 才可落单；TIME 返回 TIME_LIFECYCLE_POLICY_UNAPPROVED。
- `createOrder(String actor,String key,String product,String beneficiary)`：从已保存快照取金额、归属、购买+赠送总单位，创建 AWAITING_PAYMENT，不接收客户端 merchant。
- `confirmPayment(String actor,String key,String order,long amount,String currency,String mode)`：仅 CNY/SIMULATED，专用授权；原价匹配，PAYMENT 流水、发放、Outbox、回执同事务；重复通知不重发。模拟激活须明确 SIMULATED_ACTIVATION purpose，不推断 FIRST_ENTRY/FIRST_FULFILLMENT 政策。
- `query(String actor,String order)` → `api.TradeView`：id/owner/store/kind/state/remaining/reserved/version/refundMinor/refundState/instructionId/mode。
- `resolve(String actor,String accountId,String store,String capability)` → `List<TradeView>`；PUBLIC_PAID 仅会员/个人，STORE_ENTRY 仅对应店会员，COACHING 仅本店私教；每个来源保留版本。默认便利方法最多100来源；resolvePage(String actor,String accountId,String store,String capability,int limit,String cursor)返回api.SourcePage(items,nextCursor)，limit1–100，按不可变ID排序；游标绑定actor/account/store/capability。过滤后页可为空，须跟随nextCursor；游标是定位器而非授权凭证，不承诺跨页静态快照。
- `consume(String actor,String key,String order,long units,long expected)`：正数、可用余额、门店ACTIVE/GRACE及当前政策；乐观版本与行锁。
- `reserve(String actor,String key,String order,String businessKey,long units,long expected)`；`finishReservation(String actor,String key,String order,String businessKey,boolean consume,long expected)`：源键唯一；消费扣剩余，释放仅减预占，不因停用/政策撤回而困住已有预占。不同幂等键重用已终结 businessKey 明确冲突。
- `requestRefund(String actor,String key,String order,long expected)`：快照剩余、总单位、实付、政策版本；COUNT 平台公式，无任意退款额；有预占拒绝，申请后hold；门店停用不阻断退款。个人普通退款拒绝并提供异常入口。
- `approve(String actor,String key,String order,long expected)`：一次生成 instructionId，APPROVED_NOT_SENT。
- `dispatch(String actor,String key,String order,long expected)`：本地CAS提交SUBMITTING与Outbox；只记录模拟发送意图，不执行网络。
- `refundFact(String actor,String key,String order,String instructionId,long observedVersion,long amount,String currency,String mode,String state)`：专用可信模拟回调；指令、金额、币种、mode绑定。非成功事实必须匹配当前版本，拒绝过时失败；原指令迟到成功可收敛，成功终态不降级，退款流水唯一。
- `restore(String actor,String key,String order,boolean withdraw,long expected)`：仅REQUESTED，COUNT恢复ACTIVE及原剩余；TIME精确恢复拒绝。同单只允许一个处理中申请；撤回/驳回后可重新申请，历次不可变快照保留在Outbox事件，不能删除这些历史事件后还宣称保留了完整申请历史。
- `retryRefund(String actor,String key,String order,boolean sameIdRetryVerified,long expected)`：FAILED_CONFIRMED且提供方显式supportsSameInstructionRetry；重试原ID；UNKNOWN只查/升级，不发送。
- `escalate(String actor,String key,String order,long expected)`：原指令转MANUAL_REVIEW，保留hold；历史渠道事实保留在追加事件，不自动恢复资金。
- `reconcile(String actor,String key,String order,String dimension,long observed)`：PAYMENT/REFUND/NET，以真实trd_money汇总；匹配返MATCH，差异持久化人工核查ID；SETTLEMENT等未知口径422。
- `raisePaymentException(String actor,String key,String order,String reason,long observed)`：DUPLICATE_PAYMENT/PAYMENT_ANOMALY，建立人工差异引用，保留权益，不执行普通退款。

示例（全部是测试fixture，不是价格批准）：publish(...MEMBERSHIP,COUNT,10000,10,2,fixture,v1)→createOrder→confirmPayment(...10000,CNY,SIMULATED)→consume(...3,1)→requestRefund(...2)，申请金额7500分。

## 端口与授权

`ports.TradePorts` 本地等价类型：authorize(actor,action,merchant,store,owner)→ALLOW/DENY/UNAVAILABLE；requireActive(id)→Account(id,epoch)；requireStore(id)→Store(id,merchant,status,version)；policy(id,version)→Policy(id,version,approval,purposes)。默认DENY、依赖未配置失败、policy UNAPPROVED、safe retry false。测试源码显式提供权限和CONFIGURED_FIXTURE；没有运行时管理员或测试账号。

Policy Purpose 当前 CARD_PRODUCT/PERSONAL_PRICE/COACHING_PRICE/NORMALIZED_COUNT/SIMULATED_ACTIVATION；普通批准不含purpose不能发布。日期、日卡、补偿分母、结算/佣金均无生产默认。平台个人套餐仍仅fixture演示，不构成真实价格批准。

授权action（均加 `trade.`）：product.publish、order.create、order.read、rights.resolve、rights.consume、rights.reserve、rights.finish、payment.confirm、payment.exception、refund.request、refund.approve、refund.dispatch、refund.fact、refund.withdraw、refund.reject、refund.retry、refund.escalate、reconcile。生产角色映射未批准，不在模块硬编码。

错误当前为安全错误码的IllegalStateException（带401/403/404/409/422/503前缀），数据库错误统一503且不传播SQL；依赖异常统一503，不携带提供方正文。纯领域函数使用IllegalArgumentException/IllegalStateException。未来HTTP适配必须显式映射，不能直接公开堆栈。不存在资源404；资源owner不同且授权拒绝返回404，当前主体自身撤权返回403，均不返回正文。

## 数据与事件

V4表：trd_product（不可变交易商品快照）、trd_order（订单/独立来源/申请快照/原指令聚合）、trd_receipt、trd_money、trd_outbox、trd_reservation、trd_difference。PERSONAL 内部以空store/merchant哨兵存储，API暴露null，授权使用平台null范围。无其他模块FK/写入。会员与私教以order.kind分账。

Outbox信封 eventId/type/schemaVersion=1/producer=trade-core/aggregateId/version/occurredAt/merchantId/storeId/mode=SIMULATED，payload携带orderId、kind、paidMinor、refundMinor、remainingUnits、reservedUnits、refundId、instructionId、state。事件和源状态同事务；没有渠道发送或Outbox投递工作者。回调业务去重使用trd_receipt和唯一资金约束，未实现通用跨生产者Inbox，不能声称完整消息总线集成。

LossIntervals提供半开区间并集、与原可用区间交集、减已分配区间；测试验证hold与停用重叠不重复。当前没有落库损失账本/补时用例；P03分母、精确恢复组合未批，算法通过不代表该业务已完成。

## 后续统一装配

1. 单一集成责任者将本库接入主服务、真实IAM/Store/Account/课程商品提供方；本批未修改backend。
2. 同库事务共享DataSource/TransactionManager与账号栅栏；本库当前数据库适配器仅接受专属测试库，须集成任务提供经审查的生产适配器，不能移除白名单后直接运行。
3. 使用主服务统一迁移序列V1–V8，显式引入本模块V4；本次不迁移gym_dev。不能重新改写已执行V4。
4. booking/ranking必须加入同一本地事务：模块使用默认REQUIRED，本库fixture只证明本模块回滚。远程网络调用不得放进持锁事务；分布式失败需获批编排/补偿策略。
5. 审查事件schema、实现通用Inbox/投递与保留；真实渠道需要验签/交易ID/指令证据/对账协议。observedVersion是本地模拟栅栏，不能冒充真实渠道协议。
6. 完成时间政策及归一时间生命周期、持久化补偿后再申请完整M验收；I真实提供方/U获批UI/L真实上线另验。
