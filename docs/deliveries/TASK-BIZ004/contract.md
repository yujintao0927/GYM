# TASK-BIZ004 contract / integration

Java package: `com.gym.modules.operationscore`. All types below are library types, no HTTP application. `api.Models` records and `ports.Dependencies` interfaces are local equivalents of frozen ports v1, not imports from other in-flight modules.

## Application API

All services require `Repository` and `Access`; all public use cases execute within its JDBC transaction. Context is trusted IAM input: `Context(String actorId,String requestId,String idempotencyKey)`; no client role or merchant ownership is accepted. Every operation rechecks AccountDirectory and authorization; resource scope is resolved from stored equipment or StoreDirectory. Defaults deny, unknown dependencies fail closed. Error `Failure.status`: 401/403/404/409/422/503; SQL exceptions have no original cause or SQL text in exposed Failure.

| Service | Exact method / return | Action |
| --- | --- | --- |
| EquipmentService | `String create(Context c,String store,String name)` | equipment.manage |
| EquipmentService | `Equipment get(Context c,String id)` | equipment.read |
| EquipmentService | `List<Equipment> list(Context c,String store,Cursor cursor,int limit)` | equipment.read |
| EquipmentService | `String report(Context c,String id,String description,String imageRef)` | equipment.report + Membership EQUIPMENT_REPORT + Images |
| EquipmentService | `String change(Context c,String id,long expected,String state,String note)` | equipment.manage |
| EquipmentService | `List<Map<String,Object>> history(Context c,String id,Cursor cursor,int limit)` | equipment.history |
| EquipmentService | `List<Map<String,Object>> reports(Context c,String id,Cursor cursor,int limit)` | equipment.reports + Images per returned ref |
| ProjectionService | `void accept(Context c,Event e)` | operations.project |
| ProjectionService | `Metrics metrics(Context c,String store,Instant from,ZoneId zone,String mode)` | operations.metrics + MetricPolicies |
| ProjectionService | `Metrics metrics(Context c,String store,Instant from,Instant to,ZoneId zone,String mode)` | always 422 HISTORICAL_SNAPSHOT_UNSUPPORTED after authorization |
| ReminderService | `String evaluate(Context c,String store,String account,String kind,String policyId,long version)` | operations.remind |
| ReminderService | `List<Message> messages(Context c,Cursor cursor,int limit)` | operations.messages; SQL always filters actor |

`Equipment(id,storeId,name,state,version)`. `Cursor(actorId,storeId,filter,after)` uses stable ascending id; limit 1..100. History/report cursors use equipment ID in the scope field and filter history/reports; equipment list uses actual store and equipment; messages scope null/filter messages. Cursor fields are validated against trusted scope; changing an after-id cannot expand SQL scope. Last returned id is next after-id; an empty/short page ends enumeration. Reports expose only id/description/private image_ref/created_at; history only id/state/note/created_at. Images must be private opaque references, never long-lived public URLs; this module does not host or download images. Provider must implement current per-delivery authorization.

Equipment states: AVAILABLE -> FAULT or DISABLED; FAULT -> MAINTENANCE or DISABLED; MAINTENANCE -> AVAILABLE or DISABLED. DISABLED terminal in this version. Reports do not silently change equipment state. Staff records explicit fault/repair/return and optimistic version; history, state, receipt and invalidation Outbox commit together. Same command key/body replays, changed body conflicts. Concurrent initial identical keys may receive sanitized 409; retry original key replays sole committed result.

## Input event schema

`Event(String eventId,String type,int schemaVersion,String producer,String aggregateId,long version,Instant occurredAt,String merchantId,String storeId,String mode,String accountId,long amountMinor,boolean present,boolean reliable)`.

Only schemaVersion 1 and SIMULATED are accepted. Types RECEIVED/REFUNDED/NEW/RENEWAL/MEMBER come from trade-core; VISIT from ranking-core. Producer identity must be authenticated by integration adapter: producer text alone is not authentication. Account/store external IDs only; no copied authority tables. `occurredAt` on VISIT is visit start (not delivery time); present/reliable describe current visit snapshot. Membership present means included in the configured member population. Revenue/refund are **cumulative snapshots per stable source aggregate**, not additive deltas; source adapter uses distinct stable aggregate identities for each fact kind and preserves account/store/type. NEW/RENEWAL each represent one configured business fact. Providers must review these local payload mappings before I integration; no frozen envelope semantics changed.

Inbox eventId detects body mismatch. Fact producer+aggregateId versions reject equal-version/different-body, ignore stale versions while recording Inbox, replace newer snapshots. Source must not reuse IDs across types. Example:

```json
{"eventId":"fixture-payment-1","type":"RECEIVED","schemaVersion":1,"producer":"trade-core","aggregateId":"fixture-order-paid","version":1,"occurredAt":"2026-11-13T10:00:00Z","merchantId":"fixture-merchant","storeId":"fixture-store","mode":"SIMULATED","accountId":"fixture-user","amountMinor":10000,"present":false,"reliable":true}
```

Metrics require explicit MetricPolicy provider; missing/unapproved returns 422. Constructor `(Repository,Access,MetricPolicies,Clock)`; the two-argument constructor has no approved policy. Policy approval must come from trusted configuration, never client DTO. Window from..current clock covers latest snapshots only: received/refunded integer cents and exact net; counts NEW/RENEWAL facts; distinct active MEMBER accounts; distinct VISIT accounts on explicit local day; distinct current present accounts. Any unreliable VISIT marks estimated (no invented duration cutoff). Future facts excluded. No historical/as-of reconstruction, settlement revenue, or claim of actual collection. `Metrics` exposes receivedMinor/refundedMinor/netMinor/newMembers/renewals/totalMembers/todayVisitors/presentCount/estimated/mode. Snapshot definitions must be approved by the responsible provider/business owner; tests configure fixture only.

Outbox is a state invalidation signal, not a full equipment snapshot: type EquipmentStateChanged, event_id, aggregate_id, version, store_id, occurred_at, mode. Future dispatcher maps immutable row into frozen envelope with schemaVersion=1, producer=operations-core, merchantId from authoritative StoreDirectory and payload `{equipmentId,version}`. Consumers must fetch authorized current state; event does not promise historical state. Source outbox row and state are atomic; dispatcher retry reuses event_id. Delivery/ack dispatcher belongs to I assembly and is not provided here. No health text, credentials or image URLs in events.

## Reminder rules

Facts.require(accountId,storeId) returns ContactFact(accountId,storeId,version,expiresAt,lastVisit,consent,unsubscribed). Policies.get(id,version) returns Policy(id,version,approval,expiryLead,silence,frequency); distinguish APPROVED_RUNTIME, CONFIGURED_FIXTURE, UNAPPROVED. No shipped numeric defaults or universal fixture provider. EXPIRY/STORE_EXPIRY due at expiresAt-expiryLead; SILENCE candidate at lastVisit+silence; missing lastVisit is not guessed. STORE_EXPIRY must receive store-service fact and approved recipient from adapter; exact 30-calendar-day schedule/timezone still requires approved normalization policy. Engine creates **station inbox records only**; return null means no new message, id means durable station message, never SMS/WeChat delivery.

Consent and unsubscribe checked before and after per-store/account frequency fence; due recomputed after fence. Current locking reads prevent repeatable-read stale frequency checks. Natural key store/account/kind/policy/version/factVersion deduplicates schedules; command receipt detects same key/different body. No-message outcome also persisted for deterministic command replay; new evaluation uses a new business key. Identity and authorization rechecked before receipt replay. A previously committed message is not recalled by subsequent opt-out; no new message is emitted by replay. Scheduler/candidate enumeration comes from approved fact provider; no implicit 7/30-day recall batch.

## Assembly constraints

Only gym_dev_test_biz004 is accepted by Database.migrate; unknown objects/versions stop. Explicit location classpath:db/modules/operations-core; clean disabled, no baseline. Nine ops_ tables plus Flyway history. Existing V6 was never rewritten after application.

Future unified service must separately assemble IAM Accounts/Authorization, StoreDirectory, Membership/Rights read, private Images, trade/ranking source adapters, Facts, Policies and MetricPolicies. Each use case must share one local transaction manager/data source with participating local providers. Facts and image/authorization calls used under transactions must be local joins/current reads, not network calls while holding locks. Integrator must define consistent authorization revocation and consent serialization with provider fences. Fixture failures prove only local rollback. Real cross-module transactions, provider payload compatibility and dispatch retries need I-layer tests; external delivery requires approved orchestration/compensation, not claims of distributed atomicity. Do not register these isolated test migrations automatically in backend startup. Unified V1..V8 assembly needs its own reviewed task.
