# TASK-BIZ005 module contract (M layer)

Java package `com.gym.modules.supportcore`. Standalone library; no HTTP/security entrypoint. Parent BOM Boot 4.1.2, release 21. Runtime has no fixture authorization or default administrator. Constructor requires all ports; `SupportPorts.denyAll()` is available for safe assembly.

## Application methods

`SupportService(DataSource, Authorization, Accounts, Stores, Trade)`.
Types nested in `api.SupportModels`; port types nested in `ports.SupportPorts`.

- `Ticket open(Context, Open)` creates TICKET/COMPLAINT/REFUND_REVIEW with text and optional authoritative refund reference.
- `Ticket get(Context, String ticketId)` returns metadata only.
- `Ticket change(Context, Change)` dispatches ASSIGN/REPLY/ESCALATE/DISPOSE/APPEAL/PROPOSE/EVIDENCE with expectedVersion.
- `List<Entry> history(Context, String ticketId, long afterSequence, int limit)` returns ordered private trajectory (1..100); sequence is scoped by explicit ticketId and fresh authorization, not a bearer cursor.
- `Attachment attach(Context, String ticketId, long expectedVersion, String mediaType, byte[] bytes)` accepts private PNG/JPEG, bounded bytes, validates decode.
- `byte[] download(Context, String attachmentId)` resolves ticket from DB and reauthorizes every delivery. No URL or path is returned.

Context contains trusted actorId/requestId/idempotencyKey, no client roles. Open scope resolves Stores; later scope/owner resolves sup_ticket. Non-owner read/write requires current assignee AND Authorization.ALLOW. Assignment and disposition require explicit corresponding authorization; no runtime grants are shipped. Appeal is owner-only plus explicit authorization. Every use case has local transaction boundary; current account and authorization ports are checked on replay too. Error codes use 401/403/404/409/422/503; internal dependency errors become generic 503.

Authorization actions: support.open, support.read, support.assign, support.reply, support.escalate, support.dispose, support.appeal, support.propose, support.evidence, support.attachment.write, support.attachment.read. Provider must define approved grants. Pending P04 grants are never silently enabled.

Example synthetic call: `open(new Context("fixture-user","request-1","key-1"), new Open("fixture-store","COMPLAINT","Synthetic issue",null))`. Changes carry current aggregate version. Replays return current aggregate metadata without repeating mutation; same actor/key with different body is 409. Keys are global across commands for an actor.

Refund fact: reference/ownerId/storeId/status/version. Only provider-confirmed REJECTED/OVERDUE/MANUAL_REVIEW facts allow referral; consumer does not calculate seven-day deadline or mutate holds/funds. Unknown policy/calendar stays with trade provider. No real trade integration evidence.

PROPOSE records pending review only. DISPOSE records support case outcome, never executes merchant restriction, blacklist, money restoration or rights writes. No punishment execution endpoint exists; ActionPolicy is reserved for future approved execution adapter. Appeals append trajectory and reopen review state. No health data are fetched from other modules.

## Persistence/event contract

V7 `db/modules/support-core/V7__support_core.sql`; sup_ticket, sup_entry, sup_receipt, sup_attachment, sup_outbox. No external FK. Private image bytes are held in module DB transactions, avoiding orphan files and file-system commit races. No external object storage or public file route. This is a private binary adapter; filesystem adapter is not provided.

Outbox envelope columns: eventId, type=SupportTicketChanged, schemaVersion=1, producer=support-core, aggregateId/version, occurredAt (UTC connection interpretation required), merchantId/storeId, mode=SIMULATED. Payload `{ "state":"ESCALATED", "action":"ESCALATE" }`; excludes ticket text/images/refund detail. Audit text is private and only returned through authorized history. Delivery dispatcher/consumer Inbox is not provided: this module currently consumes no events. Future dispatcher must not log private trajectories.

Tables, audit, outbox and idempotency receipt commit together. Row lock serializes expectedVersion mutations. Concurrent opening with same key may return 409; retry returns existing resource. No external network port calls under locks: future adapters must use local shared transaction; do not wire these ports to HTTP.

## Integration instructions and remaining gates

Do not automatically scan this migration from backend. `SupportDatabase.migrate` is a test-only-database guarded installer; accepts only localhost/gym_dev_test_biz005 and rejects unknown objects/history. Future integration owner explicitly assembles V1..V8 and shared DataSource/transaction manager after review. Main runtime migration assembly must not reuse this test-schema installer.

IAM adapters must provide transaction-participating account/authorization fences to serialize revocation with delivery; this consumer fixture cannot prove real IAM atomicity or recover already delivered bytes. Stores adapter maps frozen Store fields; Trade adapter maps authoritative refund projection. All must fail closed. No service URLs/new middleware.

I: real IAM/store/trade, unified DB/HTTP, account revocation races and outbound dispatcher remain unverified. U: no frontend changed and no page images approved for this task. L: no production deployment/payment/SMS/WeChat/hardware. P04 punishment/appeal grants, P01 calendar, P07 exceptional refunds, P08 retention and privacy remain independent approval gates.
Image resource bounds: 2,000,000 compressed bytes and 4,000,000 pixels; format inspected before decode and must match MIME. These are development resource limits, not business or retention policy.
