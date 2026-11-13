# TASK-BIZ006 应用契约（M 层候选）

包 com.gym.modules.rankingcore；Java 库，无启动类/HTTP入口。仅 SIMULATED，调用者 actor 必须来自可信 IAM 适配器，字符串不是登录凭证。不得向 HTTP 原样开放传入 actor 的方法。

## 精确应用端口

`VisitService(Database, Dependencies, Clock)`：

- `String challenge(String actor,String store,Instant expiresAt)`：有效期显式输入，绑定主体和店；实际批准的挑战时限由入口适配器决定，不提供运行默认期限。
- `Visit enter(String actor,String subject,String store,String key,String challenge,String manualReason,long expectedRightVersion)`：QR/人工二选一；人工原因必填；同 key 原响应重放，异体409，重复在场409。首入场只通过 consume 端口激活扣1，失败全部回滚。
- `Visit exit(String actor,String id,String key,long expectedVersion)`：不再查询付费资格，仍检查账号/当前操作权限，允许已到期卡签退。
- `Visit correct(String actor,String id,long expectedVersion,Instant leftAt,String reason)`：有理由、版本约束的签退时间更正；不返还真实消费。更正前后值写 Outbox。
- `Visit read(String actor,String id)`；`List<Visit> list(String actor,String subject,String store,String afterId,int limit)`：UUID稳定排序，afterId必须属于同主体/店，limit 1–100。Visit 字段见 api/Visit.java。

`RankingService(Database,Dependencies,RankingPolicy,Clock)`：

- `String bracelet(String actor,String event,String owner,String device,Instant start,Instant end)`：唯一可信模拟设备绑定必须由 requireSimulatedDevice 验证。拒绝内部 visit:/region: 前缀；原始事件键异体409；有效日独立唯一。
- `String visit(String actor,String visitId)`：读取本模块权威到店记录；事件键 visit:id:version；更正后旧版本标记SUPERSEDED，再重建本主体/本店有效日。返回 PENDING_POLICY/VALID/INVALID。
- `void participate(String actor,String type,String store,boolean enabled,String nickname,long expectedVersion)`：type PERSONAL/STORE；未建行等同关闭。新建 expectedVersion=-1；空昵称从 Account 端口获取用户名，不猜用户名。
- `List<Row> board(String actor,String type,String store,String regionLevel,String regionCode,LocalDate from,LocalDate until,int limit,int offset)`：自然日半开区间，null/null累计。月份由获批窗口策略返回；运行政策未批周期/并列模式拒绝 PENDING_POLICY。fixture可计算。区域 COUNTRY/PROVINCE/CITY/DISTRICT；只输出昵称/天数/名次；相同天数相同名次。每次请求稳定排序；offset跨请求不保证动态榜单快照连续性。
- `RegionResolver.Region region(String actor,String authorizationReference,RegionResolver resolver)`：只接收授权引用和返回地区码；不接收坐标。首次是否计入月次数仍未批，因此运行时PENDING_POLICY；显式fixture将首次计为一次，验证每月最多一次。地区存在 participant 的 REGION:* 私有行，enabled=false，绝不参榜。
- `String appeal(String actor,String event,String reason)`；`Appeal appealRead(String actor,String id)`；`void appealReview(String actor,String id,String evidence)`：待核查→已记录审核轨迹；不据此自动改成绩或处罚。异常成绩裁决政策未批，实际成绩处置待后续政策实施。

授权 action：visit.challenge、visit.enter.qr、visit.enter.manual、visit.read、visit.exit、visit.correct、ranking.bracelet.ingest、ranking.visit.ingest、ranking.participate、ranking.board、ranking.region、ranking.appeal、ranking.appeal.read、ranking.appeal.review。Authorization 默认 DENY；UNAVAILABLE→503。依赖默认失败闭合。错误仅安全代码，不携SQL/凭据/Token；401/403/404/409/422/503区分见 Failure.status。

## 外部端口映射

Dependencies.active 必须实时校验账号状态；store 返回权威店和商家归属；authorize返回 ALLOW/DENY/UNAVAILABLE；right必须通过真实Rights.resolve选择本人、本店、本能力的明确来源，不得合并异店余额；consume收到当前 JDBC Connection、Right(id,version)、owner/store、visitId业务键和serverNow，必须加入该连接事务原子激活并扣1，使用权威商品激活政策并返回消费引用。禁止独立提交或持锁网络调用。requireSimulatedDevice验证当前唯一设备/主体绑定，未配默认503。运行包不含设备/权益/授权fixture。

本地声明没有复制交易实体，真实提供方的 Account epoch/Store status/多来源列表/激活政策映射及账号栅栏锁序，须在I层适配中补齐并评审；当前接口本身不证明与IAM或trade同库原子性。Account用户名只为默认昵称提供，不进入Outbox。

RankingPolicy.Config 状态为 UNAPPROVED/CONFIGURED_FIXTURE/APPROVED_RUNTIME，显式阈值、时区、最少人数。运行 board 额外要求 approvesWindow(from,until) 和 tieMode；不发布20分钟/10人。region当前仅支持fixture算法，首次计数政策缺批准。数据以epoch毫秒存储，无隐含30天月。

## 表和事件

V8：vis_lock、vis_challenge、vis_visit、rnk_receipt、rnk_outbox、rnk_raw、rnk_day、rnk_participant、rnk_appeal；只外部ID，无外部表/FK。rnk_raw主键作为持久Inbox；rnk_day主体/范围/日期主键防同日重复；vis_visit生成列唯一约束保护本店单一在场。

Outbox和源写同事务：VisitEntered/VisitExited/VisitCorrected/BraceletObserved/AppealReviewed。信封列 event_id/type/schema_version=1/producer=ranking-core/aggregate_id/aggregate_version/occurred_at/merchant_id?/store_id?/mode=SIMULATED/payload。payload示例：VisitExited `{ "visitId":"synthetic-id", "subjectId":"synthetic-subject", "source":"QR", "enteredAt":0, "leftAt":1000 }`；BraceletObserved包含sourceEventId/subjectId/source/qualification；更正包含actorId/reason/previousLeftAt/leftAt；审核含actorId/evidenceRef。无精确定位、设备密钥或健康正文。示例不是生产事件。

Outbox投递器、重试认领和真实上游事件信封适配属于统一装配；当前不宣称端到端消息已投递。visit()为显式消费入口，需由装配者在更正事件后调用，不是自动调度。
