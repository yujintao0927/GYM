# TASK-BIZ003 应用契约与集成边界

当前为单体内 Java 库，包 com.gym.modules.bookingcore；无 HTTP 启动器，不装配到 backend。所有调用模式仅 SIMULATED，IAM 适配器必须构造可信 Context，客户端 role/merchantId 不参与授权。

## 精确 Java 入口

BookingApi.Context(actorId, requestId, key, mode)。BookingService(DataSource, BookingPorts)，当前构造即检查 JDBC URL，拒绝本任务库之外地址。

| 方法 | 命令/结果 | 授权 action |
| --- | --- | --- |
| schedule(Context, Schedule) → String | Schedule(storeId,coachId,kind,start,end,capacity,policyId,policyVersion)；返回 sessionId | booking.schedule |
| book(Context, Book) → String | Book(sessionId,sourceId,expectedRightsVersion,orderId)；返回 bookingId | booking.reserve |
| change(Context,String id,long version,String command) → String | command=CANCEL/CHECK_IN；返回 bookingId | booking.CANCEL / booking.CHECK_IN |
| find(Context,String id) → View | id,sessionId,ownerId,state,version,reconciliation | booking.read |
| listSessions(Context,String store,String cursor,int limit) → SessionPage | limit 1–100，items 含教练、开始/结束 Instant、政策版本及容量；nextCursor；游标绑定 actor/store/查询类型 | booking.list |

示例：先由获授权商家 schedule 深入指定 Instant 半开区间、kind=GROUP 或 PRIVATE（私教容量须1），然后本人用 sourceId/version、已存在订单引用 book。使用返回 bookingId 与 version=0 调用 CHECK_IN。相同主体、操作、key、相同命令返回原 id；异体409。重放仍重新鉴权。BOOKED→FULFILLED 或 CANCELLED，终态不可互转。期限/次数商品由可信订单快照 kind=TERM/COUNT 指定，不换算默认月、日或收费。

BookingFailure 的 status/code 为稳定错误，401主体、403授权、404不存在/跨商家、409容量/版本/幂等、422非法/未批政策、503未知依赖。无请求/健康正文和凭据透传异常。

## 外部端口

BookingPorts 中所有默认方法拒绝或依赖不可用；没有运行时万能角色、有效卡或默认政策实现。仅 src/test 的 fixture 明确允许合成主体和政策。

- authorize(actor,action,merchant,store,owner) → ALLOW/DENY/UNAVAILABLE，归属由 session/booking 与 requireStore 交叉核对。
- requireActive(actor)、requireStore(store)、requireCoach(coach,store) → Store(storeId,merchantId,status,version)。每次调用当前提供方，不缓存角色。
- getPolicy(policyId,version) → CONFIGURED_FIXTURE/APPROVED_RUNTIME/UNAPPROVED；未知和未批失败闭合。
- requirePurchase(order,owner,store,session,coach,courseKind,policy,version) → Purchase：订单/主体/门店/productId/商品类型/政策版本/付款前明示与接受标记/整数取消费。必须查询 trade 的不可变原始快照，不信客户端声明；order_id 是接受证据的权威外部引用，原始证据由订单所有者保留。
- resolve(owner,store,capability) → Right(sourceId,version,orderId,productId,coachId) 列表；逐项绑定订单/商品/适用教练。reserve/consume/release 明确 quantity、expectedVersion、businessKey，返回 RightsResult(reference,version)，存回新版本；后续命令在事务内重新 resolve 当前版本，避免另一笔预占后旧版本失效。COURSE_RESERVATION_MUTATION 表示查询已有预占所属源以操作；是否允许 hold 下释放或消费仍由权益写者裁决，不能当作消费授权。只由权益提供方写权益。
- createOrder(CancellationOrder) → 交易引用。字段 bookingId、原 orderId、beneficiary、policyId/version、amountMinor、mode、businessKey。只有明示且接受才生成取消费订单。生成订单不等于扣款；FEE_ORDER_CREATED 等待交易侧对账。预约取消不创建退款，不修改平台退款公式。

## 数据与事件

V5__booking.sql，显式 classpath:db/modules/booking-core。五张本地表：bkg_coach_locks（串行教练时段）、bkg_sessions（容量/版本）、bkg_bookings（状态、来源/订单/政策快照引用、消耗/取消交易引用及对账状态）、bkg_receipts（主体/操作/键唯一）、bkg_outbox（事件）。加 Flyway 历史表；无外模块 FK 或写入。

数据库 Flyway checksum：1511461192。SQL SHA256：ABECEFF60E2918CF9E346FBFE810379A20837924BD24104EF40CF677B7E32852。该已安装迁移不改写、不 clean。Flyway checksum 以库内 flyway_schema_history 为执行证据。

事件信封映射 outbox 列 event_id/event_type/schema_version=1/producer=booking-core/aggregate_id/aggregate_version/occurred_at/merchant_id/store_id/mode=SIMULATED/payload；事件类型 SessionScheduled、BookingReserved、BookingFULFILLED、BookingCANCELLED；payload 示例 {"bookingId":"synthetic-booking-id"}。与源状态和回执同事务，不含健康正文、定位或凭据。当前不消费外模块异步事件，故无需伪建 Inbox；权益/交易以同步本地端口核验。未来任何新增入站事件必须独立 Inbox 去重后才可更新投影。

## I 层装配约束

统一服务需适配 IAM/商家/权益/交易/政策真实提供方，将单一 DataSource 和事务管理器绑定在同一本地事务。持锁期间禁止外部网络和独立提交。当前 String 引用 fixture 不能证明真实权益/资金原子性。遇外部 UNKNOWN 不允许更换资金业务键重试；需统一责任方设计经批准编排/补偿及对账。

统一迁移由 I 层按 V1–V8 排序显式装配；本库严格测试 URL 守卫需在授权集成任务中替换为受审的统一库守卫，不直接删校验。先完成真实角色/账号栅栏、版本映射、首次私教履约激活、消费/退款 hold 互斥、费用支付确认及事件派发测试，再开放统一 HTTP。价格/结算、激活期限、未明示罚款和未批商业政策不设运行时默认。没有前端效果图或页面批准范围；本任务无 UI。
