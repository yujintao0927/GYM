# 接口草案 v0.1（待批准，未实现）

内部契约与HTTP入口共享权限，不模拟真实渠道协议。所有C编号的REQ、场景反向映射见[scenarios.md](scenarios.md)；唯一负责人及迁移见[domain.md](domain.md)。

## 公共约定

路径/v1；ID字符串不可猜测但仍须鉴权。Authorization会话解析accountId/sessionEpoch；entry为user-web/user-mini/merchant/coach/platform-admin/platform-support；merchantId/storeId由服务端与资源核对，不能信任客户端角色。内部服务调用另认证scope。写请求Idempotency-Key+expectedVersion，键范围actor+operation+mode，保留期限待支付审计要求确认；同键同体返回原结果，异体409。版本冲突409，调用方刷新而不覆盖。

响应信封{data,requestId,mode,serverTime,contractVersion}；mode=SIMULATED/LIVE_RESERVED/LIVE_VERIFIED，当前只有设计，无任何模式实现；LIVE_VERIFIED只能由独立真实渠道验收配置启用，客户端不能自选。错误{code,message,requestId,retryable,details}，不泄露资源存在或健康字段。401 SESSION_INVALID；403 ROLE_DENIED/ENTITLEMENT_REQUIRED/CONSENT_REQUIRED；404 RESOURCE_NOT_FOUND（含跨归属）；409 VERSION_CONFLICT/IDEMPOTENCY_MISMATCH/REFUND_IN_PROGRESS/CAPACITY_FULL；422 POLICY_UNAPPROVED/INVALID_INPUT；503 ADAPTER_UNAVAILABLE。业务未知状态返回202及查询URL，不冒充成功。

列表采用limit默认20最大100，opaque cursor，稳定(createdAt,id)排序，响应{items,nextCursor,asOf}，游标绑定主体、租户与筛选；跨页重新鉴权。金额amountMinor为整数分，currency=CNY，禁止浮点和默认币种转换；时间RFC3339毫秒及zone，区间半开。分页/非分页字段中未出现的健康信息不返回。审计actor、scope、resource、decision、policyVersion、requestId，不记录凭据/精确坐标/健康正文。

## 入口与字段

以下每行响应均套公共信封，POST成功201或202、GET成功200；失败遵循公共错误及行内业务错误。字段必填，标?为可选；资源id用{}。每项依赖数据所有者命令，不允许调用方改表。

| 契约/唯一负责人 | 方法及入口 | 请求字段 | 响应data字段 | 权限与归属/额外错误 |
| --- | --- | --- | --- | --- |
| C01 身份TASK-003 | POST /v1/sessions；GET /v1/me/contexts；DELETE /v1/sessions/{id}；各独立端共用 | 登录credential(实际认证方式待选)、entry；退出sessionId | sessionToken、expiresAt；accountId、staffScopes[{role,merchantId,storeIds,validUntil}]、entitlementSources[] | 仅本人，entry不授予权限；注销后旧epoch401且新登录/刷新拒绝；上下文不是长期授权凭证 |
| C02 商家TASK-004 | GET /v1/stores/{id}/service；POST /v1/stores/{id}/renewal-orders | storeId；套餐版本、expectedVersion | state、endAt、graceEndAt、serviceVersion、renewalOrderId | 店主范围或必要会员状态投影；金额由交易商品快照计算，不信任客户端价格 |
| C03 权益TASK-004 | GET /v1/me/entitlements（不开放独立activate接口，见REV-002-01）；内部 POST /v1/internal/entitlement-commands | sourceId、command=ISSUE/HOLD/RESUME/CLOSE/COMPENSATE、orderId、policyVersion、intervals? | entitlementId、status、startAt、endAt、remainingUnits、version、capabilities、sourceStoreId | 本人可查；内部命令限定交易/恢复服务主体；重复orderId不重复发放 |
| C04 出入场TASK-004 | POST /v1/stores/{id}/visits；POST /v1/visits/{id}/exit | entitlementId、qrChallenge或manualReason、subjectId（前台）、expectedVersion | visitId、enteredAt、leftAt?、consumptionId?、estimated、version | 用户本人本店可用卡或本店前台代登；QR一次性短期挑战建议；ALREADY_IN幂等/无在场签退409 |
| C05 交易TASK-005 | POST /v1/orders；POST /v1/orders/{id}/payment-attempts；GET /v1/orders/{id} | productId、productVersion、acceptedPolicyVersion、storeId?；channel=SIMULATOR/WECHAT/ALIPAY/BANK、mode | orderId、beneficiary、paidMinor、currency、productSnapshot、paymentState、fulfillmentState、attemptId、nextAction? | 本人订单/最小商家订单权限；宽限拒新收费办卡；BANK路径未定返回POLICY_UNAPPROVED，真实无配置503 |
| C06 退款TASK-005 | POST /v1/orders/{id}/refunds；POST /v1/refunds/{id}/review或withdraw；GET /v1/refunds/{id} | reason、expectedVersion；decision=APPROVE/REJECT、reason；不得传任意退款额 | refundId、state、requestedAt、reviewDeadline、snapshot{paidMinor,totalUnits,remainingUnits,unitType,policyVersion,compensationRefs}、refundMinor、entitlementHold、channelState | 本人申请/撤回（仅REQUESTED）；商家指定审核范围；个人普通退款拒绝BUSINESS_REFUND_UNAVAILABLE但可异常申诉；成功不等到账 |
| C07 支付适配TASK-005 | 内部 acceptNotification/queryPayment/submitRefund/reconcile（非真实渠道URL协议） | adapterId、mode、eventId、channelTransactionId、orderId、amountMinor、currency、beneficiary、channelState、occurredAt、verifiedEvidenceRef | accepted、duplicate、normalizedState、differenceId? | 适配器先验签/重放校验，再金额币种受益方匹配；渠道消息不符隔离；模拟服务凭证不能访问真实空间 |
| C08 课程唯一子任务 | GET /v1/stores/{id}/classes；POST /v1/classes/{id}/bookings；POST /v1/bookings/{id}/cancel或check-in | slotId、entitlementId或orderId、acceptedCancellationPolicyVersion、expectedVersion | bookingId、state、holdExpiresAt、capacityRemaining、chargeRef?、policySnapshot | 本人预约/教练本课/本店授权运营；容量不足409、时段冲突409；取消与退款是两条命令 |
| C09 健康TASK-006 | POST/GET /v1/me/health-records；POST /v1/me/recognition-drafts、/plans、/ai-conversations；POST /v1/me/consents；DELETE /v1/me/consents/{id}；GET /v1/coach/subjects/{id}/health-records（账号注销确认入口统一归C01，见REV-002-02） | category、measuredAt、value、unit；imageRef、confirmedItems[{food,portion}]；goal、experience、time、equipment；coachId、categories、expiresAt；confirmationToken | recordId、trend、mealDraft{requiresConfirmation}、plan{actions,editable}、quota、consentId、epoch、deletionJobId、retentionNotice | 手动及本人历史免费；AI/识别须付费并预占额度，识别草稿未确认不存正式饮食；教练当前授权限定类别；注销二次确认撤会话授权，法定保留待核查 |
| C10 TASK-007唯一榜单输入 | 内部 ingestBraceletEvent；consumeVisitEvent | eventId、source=SIMULATED_BRACELET/VISIT、deviceRef?、subjectId、storeId?、startedAt、endedAt、metrics{trainingDurationMs}、mode、producerVersion、visitId? | accepted、duplicate、qualification=PENDING_POLICY/VALID/INVALID、effectiveDate?、reason | 手环仅TASK-007生产并消费；到店由TASK-004生产TASK-007消费；TASK-005无生产消费职责。原始事件去重与有效日去重分开 |
| C11 榜单TASK-007 | POST /v1/me/participations；POST /v1/me/region；GET /v1/leaderboards；POST /v1/ranking-appeals | scope、enabled、nickname?；locationAuthorizationRef及短暂坐标；scope=STORE/COUNTRY/PROVINCE/CITY/DISTRICT、period=MONTH/ALL；eventRef、reason | participationId、regionCode、changeCount、rows[{displayName,validDays,rank}]、mode、appealId | 默认关闭；本店榜限本店资格；个人榜需个人权益，定位拒绝保全国；只存地区；同分rank相同，后序建议竞赛排名待P05 |
| C12 器械唯一子任务 | GET /v1/stores/{id}/equipment；POST /v1/equipment/{id}/reports；PATCH /v1/equipment/{id} | description、imageRefs?；state、maintenanceNote、expectedVersion | equipmentId、state、reportId、maintenanceHistory、version | 本店有效会员报修/安全入口，授权员工更新；故障→维修→可用或停用，保留轨迹；跨店404 |
| C13 客服唯一子任务 | POST /v1/tickets；GET /v1/tickets/{id}；POST /v1/complaints/{id}/actions | text、privateImageRefs、orderRef?；action、reason、evidenceRef | ticketId、status、assignedScope、actionAuditId、appealAllowed | 本人/分派客服；平台限制拉黑需专权、提案双人复核；附件签名短期URL，健康不默认附带 |
| C14 经营消息唯一子任务 | GET /v1/stores/{id}/metrics；GET /v1/me/messages；POST /v1/admin/merchant-reviews | from、to、mode；cursor；decision、evidenceRef | receivedMinor、refundedMinor、netMinor、memberCount、todayVisitors、presentCount、estimated；messages[]；reviewState | 商家本范围最小财务权限；消息仅本人；人工入驻审核平台专权；微信短信未接入不返回送达成功 |

## 内部事件、对账与补偿边界

事件信封{eventId,aggregateId,aggregateVersion,type,schemaVersion,producer,occurredAt,mode,merchantId?,storeId?,payload}。交易生产PaymentConfirmed/RefundSucceeded给权益和经营；权益生产EntitlementChanged给消费者失效投影；健康授权撤回不能仅等异步事件才拒绝新访问。课程调用权益消费，权益产生UsageCommitted供交易快照；出入场及手环按C10生产消费，榜单不会反写Visit。

支付适配器只给规范化事实；真实微信/支付宝/BANK字段、验签、证书轮换、重试确认响应码在渠道接入时按官方协议另定。本表不是任何真实协议已可用证明。对账按mode+channel+businessDate拉取账单，比较交易ID、金额币种、受益方和退款累计；长时间UNKNOWN进入人工差异队列，不自动吞差/默认到账。重试原退款指令ID；异常重复扣款不套个人普通拒退，转支付异常工单，金额与结算路径Q08待批准。

模拟订单、流水、回调、榜单和经营投影全程携带mode；禁止SIMULATED关联LIVE资金，模拟收据标“模拟，无真实收款”。本次仅接口设计，没有运行中的模拟适配器或真实功能。

## C03补充：消费与额度子命令

内部command另含CONSUME/RESERVE_QUOTA/COMMIT_QUOTA/RELEASE_QUOTA；请求{sourceId,entitlementId或accountId,capability?,period?,quantity,consumptionKey,expectedVersion,bookingId或visitId或aiRequestId}；quantity为正整数，源scope只开放课程、入场或健康调用各自能力。响应{commandId,consumptionId或reservationId,remainingUnits,reserved,consumed,version,replayed}。CONSUME以权益、服务状态和退款hold共同裁决，成功扣减与源业务状态在同一本地事务；重复consumptionKey不再扣。配额预占成功才发起AI，成功提交，失败或明确未发送则释放；结果未知不立即释放而查询原请求，超时回收须按同一aiRequestId对账，不重复计费。预占提交释放按reservationId幂等，已提交再释放409，额度不足403 QUOTA_EXCEEDED。健康不能直接写QuotaLedger。以上绑定D07/D09/D15/D20及REQ-019/034/039/043/044/094/096，P06未批时不得开放真实额度实现。

## 契约显式追踪索引

| 契约 | REQ（主项，复合项仍须场景全分支） | 设计场景 / TASK-001同号S |
| --- | --- | --- |
| C01 | REQ-002、REQ-006、REQ-013、REQ-014、REQ-097 | D01/D02/D16/D20 |
| C02 | REQ-001、REQ-004、REQ-022、REQ-023、REQ-024、REQ-025、REQ-027 | D03/D04/D23 |
| C03 | REQ-007、REQ-008、REQ-010、REQ-016、REQ-019、REQ-021、REQ-028、REQ-091、REQ-094、REQ-095 | D01/D02/D04/D07/D09/D13/D15 |
| C04 | REQ-009、REQ-018、REQ-020、REQ-056 | D01/D09/D17/D23 |
| C05 | REQ-004、REQ-005、REQ-061、REQ-064、REQ-080 | D10/D14/D20/D23 |
| C06 | REQ-029、REQ-032、REQ-033、REQ-034、REQ-035、REQ-036、REQ-037、REQ-038、REQ-039、REQ-084、REQ-094、REQ-096 | D05/D06/D07/D08/D09/D10/D11/D12/D13/D14 |
| C07 | REQ-065、REQ-066、REQ-067、REQ-075 | D08/D14/D20/D21 |
| C08 | REQ-059、REQ-060、REQ-061、REQ-081、REQ-096 | D09/D10 |
| C09 | REQ-011、REQ-012、REQ-015、REQ-040、REQ-041、REQ-042、REQ-043、REQ-044、REQ-045、REQ-079、REQ-085、REQ-092、REQ-097 | D02/D15/D16/D22 |
| C10 | REQ-047、REQ-048、REQ-053、REQ-068、REQ-074、REQ-088 | D17/D19/D20/D21 |
| C11 | REQ-046、REQ-049、REQ-050、REQ-051、REQ-052、REQ-083 | D01/D17/D18/D19 |
| C12 | REQ-058、REQ-071 | D01/D21/D23 |
| C13 | REQ-030、REQ-031、REQ-062、REQ-063、REQ-097 | D11/D12/D14/D16/D23 |
| C14 | REQ-054、REQ-055、REQ-056、REQ-057、REQ-063、REQ-076 | D10/D20/D21/D23 |

REQ-003/069/086是集成配置限制，REQ-093是架构限制，REQ-017/026/087/090是排除行为，REQ-070–078/089是延期及上线限制，所有契约共同遵守D20–D23的范围门槛，不为延期行为增设可执行接口。

## ACC-002修订字段与权限（保留C01–C14编号）

C04 EnterStore请求新增entryOperationId、activationPolicyVersion、subjectId、challengeId（或manualReason），响应新增activationApplied、activationId?、entitlementVersion、consumptionId?、visitId、replayed。C03内部ActivateForEntry请求{entryOperationId,visitId,subjectId,storeId,serverNow,activationPolicyVersion,expectedVersion}；仅C04服务scope，要求挂接当前事务、无独立提交；首次入场局部资格和失败原子回滚见states REV-002-01。C08内部ActivateForFulfillment绑定bookingId和completionOperationId，个人激活绑定paidOrderId，均由权益所有者写；用户直调activate不在开放路由（404），不能伪造internal scope（403）。绑定REQ-016/018/020/038、D11/D24；D24为S11/S01/S09扩展。

C01新增POST /v1/me/account-closure-previews，请求{entry,reauthProof}响应{closureToken,impactVersion,expiresAt,entitlementImpact,unsettledTransactionNotice,retentionNotice}；POST /v1/me/account-closure-confirmations请求{closureToken,impactVersion,confirmed:true,expectedSessionEpoch,Idempotency-Key}响应{closureId,accountStatus:CLOSED,confirmedAt,cleanupState}。TASK-003唯一编排；C09仅内部RevokeAllConsents{closureId,accountId,expectedConsentEpoch}及后续CleanupHealth{closureId,retentionPolicyVersion}，响应{revokedEpoch,cleanupJobId,state}，前者加入关闭事务，后者可靠异步重试。原C09公开deletion-confirmations入口移除，不存在第二套仅删健康却称账号注销的路径。新登录/刷新ACCOUNT_CLOSED，确认token过期422、影响版本变更409、跨主体404；未提交关闭事务失败503且账号原状。绑定REQ-015/085/097、D16/D25（S16/S20扩展）。

C06响应新增instructionId、normalizedState（states完整枚举）、lastChannelState、holdState、retryable、nextAction、manualCaseId?、lastError{category,safeMessage}、version、confirmedAt?、arrivalState=UNKNOWN/CONFIRMED（真实到账语义待渠道核验）。内部retry请求{refundId,instructionId,expectedVersion,reason,evidenceRef}仅自动工作者或refundResolve专权；manual-takeover同字段并返回manualCaseId；abort-unsent另需reviewerId及neverDispatchedProof，受P07批准限制。C07通知/查询新增instructionId、attemptId?、snapshotHash、normalizedOutcome=ACCEPTED_PROCESSING/FAILED_CONFIRMED/SUCCEEDED/UNKNOWN、retryability=SAFE_SAME_ID/NO_RETRY/UNVERIFIED、evidenceRef。适配器映射负责TASK-005，不能把timeout映射FAILED_CONFIRMED；发送前失败由本地状态而非虚构渠道码记录。恢复hold权限与状态约束见states REV-002-03，403无处理权、409状态竞争或原ID不符、202确认/人工处理中；同一指令成功回调200 duplicate=true不再记账。绑定REQ-035/039/065/094、D08/D12/D13/D14/D26（S08/S09/S13/S14扩展）。
