# TASK-BIZ004 execution ledger
Baseline b3ce453e92cc378a25dac27f7d1591eb01274baf; PAR-BIZ / frozen ports v1.
1. Domain: state transitions, exact monetary projection, policy gates. RED then GREEN.
2. JDBC: guarded V6 migration, transactional equipment/history/receipts/outbox.
3. Projection: typed fact envelopes, inbox and aggregate versions, scoped queries.
4. Reminder: approved facts/policy, consent, unsubscribe and frequency serialization.
5. Real MySQL verification, review, documentation, selective commit/push.
Ruling: ledger stays in allowed delivery directory instead of skill scratch outside scope.
Interfaces: all application services use local equivalent frozen authorization/account/store ports; source facts remain owned by providers.
No UI; approved images/scope: not applicable.

Domain RED: red-domain.log 2 assertion failures, then 9/9 GREEN verify-jdbc.log.
Process gap: initial application suite was authored before services, but not executed before implementation; no claim of full application RED/GREEN compliance.
Future-statistics RED: red-future.log expected 0 present but found 1. Root cause: current fact loop did not exclude facts >= query cutoff; filter added before all counters.

Final review: independent agent read-only review found six important issues: stale reminder due check, raw SQL errors, missing reminder key receipts, missing report retrieval, unsupported historical snapshots, unbounded history. Fixed within module, no SQL rewrite.
Additional concurrent frequency RED: verify-final.log expected one message, actual two. Root cause: REPEATABLE READ consistent count snapshot predates frequency lock; use locking current read after account/store fence. Recheck full suite.
Ruling: current-snapshot statistics only; reject historical cutoff, require explicit metric policy. Historical reconstruction needs future approved migration, not silent guesses or rewriting installed V6.
Ruling: preserve worktree and push task branch per user's instructions; no merge/PR/menu needed.
