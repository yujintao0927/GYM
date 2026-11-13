# TASK-BIZ005 execution ledger
Baseline b3ce453e92cc378a25dac27f7d1591eb01274baf. Plan: PAR-BIZ, user TASK-BIZ005.
Scope: modules/support-core and docs/deliveries/TASK-BIZ005 only; V7 sup_; gym_dev_test_biz005.
1. Domain transitions and independent module build: in progress (test first).
2. JDBC transactions, idempotency, audit, evidence/appeal and private attachment authorization: pending.
3. MySQL failure/concurrency/reconnect and integration contract: pending.
4. Review, verify, commit and push: pending.
Remote query failed due unavailable configured proxy. Shared main untouched.
Review: image dimensions/type issue reproduced as assertion failure, corrected with ImageReader metadata validation before bounded decode. Added post-attachment failure rollback assertions for blob/version/audit/outbox/receipt. Whole suite 13/13 passed on initial environment; final exact JDK21 verification pending.
Final: JDK21.0.2 clean verify exit0, 15/15; no skipped tests. Review issues fixed and verified. Only permitted task paths. Pending commit/push outcome.
