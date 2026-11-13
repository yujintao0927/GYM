# TASK-BIZ004 report

Status: M-layer implementation delivered for review; user/COORD-01 acceptance pending. No composite REQ is marked complete. I/U/L unverified. Scope: REQ-009/013/022/055/056/057/058/063 operations subset only.

Baseline full SHA b3ce453e92cc378a25dac27f7d1591eb01274baf contains DEC-027/PAR-BIZ/frozen ports/task prompt. Remote main readback matched; remote task branch absent before push. Local shared main was never switched; untracked AGENTS.md and WEB001 materials preserved. No chat inspection capability was available, so no claim that all other chats were inspected.

Worktree: C:/Users/28795/.codex/visualizations/2026/11/13/01a1d576-b3d7-7861-b0d9-f566c8800f90/task-biz004.
Branch: codex/task-biz004-operations-core. Full implementation SHA and push evidence appended after commit; final report commit SHA handed over in chat (avoids self-reference).

Inputs: current requirements; coordination README/decisions/tasks/sessions/questions-risks/requirements-trace/local-development; PAR-BIZ and business-module-ports-v1; TASK-002 domain/contracts/states/report and QA001 strategy/risk-cases; backend POM and local IAM guard/transaction code read-only. Java21/Boot4.1.2 and JDBC/Flyway versions checked against owning POM/BOM.

Changes confined to modules/operations-core/** and this delivery directory: independent POM/.gitignore, api records/error, ports, domain state/money rules, transactional application services, JDBC repository/database guard, one V6 migration, meaningful domain+MySQL tests; plan ledger, contract/integration, verification, replay script and raw logs. No frontend code or image approval scope applies.

Implemented: scoped equipment archive/state/report/private-reference delivery/maintenance timeline; receipts and equipment invalidation outbox; versioned fact Inbox/current snapshots, exact integer revenue/refund/net and distinct population/visit/presence counters with estimated marker; approved-policy station reminder engine, silence eligibility, consent/opt-out/frequency and own inbox. All external providers remain explicit fixtures. See contract.md for exact Java API, action list, input/output/event examples, snapshot semantics and integration requirements; verification.md for replay, tables/checksums and RED/GREEN evidence.

Verification: required backend-cwd clean verify, real gym_dev_test_biz004/MySQL26.7.0 only; final verification.log records 21 tests, zero skipped/failures, V6 checksum -261215964, repeat migration and reconnect. Test process exit and Git checks recorded below. No credentials or real personal data in fixtures. No Flyway validation bypass/clean or modification of installed V6; database data retained.

Independent review found and fixed stale reminder eligibility, SQL exception leakage, missing reminder receipts, report retrieval, history pagination and historical query overclaim. Additional real MySQL frequency race reproduced and fixed using current locking reads. Full application TDD process was not achieved on initial service implementation; this is a disclosed verification-process gap, not hidden by passing tests.

Remaining/limits: historical as-of reconstruction is explicitly unsupported (422); current-snapshot metric definitions require explicit trusted approved policy, test definitions are fixtures only. Candidate enumeration/scheduler and merchant-term timezone/date normalization require real fact/policy adapters; no invented recall thresholds or runtime sender. No private file hosting; only authorized opaque reference handoff. Outbox dispatcher/ack/retries, real IAM/member/store/trade/ranking integration, shared database atomicity, unified HTTP/migrations and provider consent/revocation fences remain I-layer. No UI, real payments/SMS/WeChat/hardware/AI/precise location/health-body access or commercial deployment.

Rulings: keep ledger in allowed delivery directory; current snapshot metrics only (future historical reconstruction needs approved persistence design); retain worktree and push task branch as instructed. No deferred minor finding. Independent reviewer declined I/U/L, rightly outside this task. These limitations must be considered in M review; passing fixtures do not prove full business acceptance.

Please carry this report, final full SHA, branch and this directory back to COORD-01. No automatic notification or acceptance claim.
