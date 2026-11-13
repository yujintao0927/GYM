# TASK-BIZ004 verification

Replay from worktree: `python docs/deliveries/TASK-BIZ004/run-verify.py replay.log`. It privately reads applicable local credentials or use GYM_TEST_DB_URL/GYM_DB_USERNAME/GYM_DB_PASSWORD with wrapper manually. Credentials never printed, committed or passed as command arguments. Equivalent required command, cwd backend: `.\mvnw.cmd -f ../modules/operations-core/pom.xml -B clean verify`. Script limits process to 180 seconds. JDBC creates only approved missing schema with utf8mb4-compatible UTF-8 configuration; no DROP/clean/reset/global SQL. UUID fixtures accumulate in own schema; no cleanup of other data.

Baseline b3ce453e92cc378a25dac27f7d1591eb01274baf. POM reuses Boot parent/BOM 4.1.2, Java release21, Spring JDBC 7.0.10, Flyway core/mysql 12.4.0, Connector/J 9.7.0 (local BOM evidence). No HTTP application/plugin or dependency on other in-flight modules. Single Surefire JVM, bounded two-thread concurrency; DriverManagerDataSource has no pool, tests at most two active workers. Actual server MySQL 26.7.0; Flyway newer-than-verified warning retained, no production compatibility claim.

Migration V6__operations.sql SHA256: 90abe7ab508ad203f6238f5b40dc661bf59c9e0f99e01f0362bbc63edd14e98f. Installed Flyway checksum -261215964. Tables: ops_equipment, ops_history, ops_report, ops_receipt, ops_outbox, ops_inbox, ops_fact, ops_contact_lock, ops_message. Unknown tables or foreign migration versions rejected. Repeated startup validates and runs zero migrations; independent data source reconnection retains created archive.

Evidence logs:

- red-domain.log: 2 meaningful assertions failed against placeholders; GREEN verify-jdbc.log 9/9.
- red-future.log: future VISIT incorrectly present (expected0 actual1); green-future.log 11/11 after cutoff filter.
- red-reminder.log: refreshed expiry still emitted; red-review.log additionally catches missing report reads and SQL exception leakage.
- red-reminder-key.log: same reminder key/different kind not rejected; all four defects fixed, green-review.log 16/16.
- verify-final.log: real concurrent frequency race produced2 messages expected1. Root cause REPEATABLE READ prior snapshot; locking current read fix, verify-final-green.log 20/20.
- verification.log: final full build, migration evidence and 21 tests; no skips. Author checks final exit before reporting.

Tests cover repair lifecycle/history/private-ref authorization, replay/key conflict, current revoke/unavailable authorization, persisted merchant scope, cursor scope, optimistic two-writer competition, exact 10000-3000=7000 projection, Inbox/equal-version/older-version cases, future facts, policy gates, consent/unsubscribe/frequency/own inbox, due recheck, same-key competition, transaction failure rollback of state+history+receipt+outbox, migration repeat/reconnect and wrong schema rejection, safe SQL errors. Independent reviewer reviewed source read-only; parent fixed findings and reran tests. No real providers or UI/channel assertions.

Process limitation: initial application tests were written before services but were not run before implementing those services. Do not claim full application TDD compliance. Subsequent concrete defects have actual RED/GREEN evidence above. Wrapper accidentally invoked from worktree root failed before build; correct backend cwd used for successful runs. Initial sandbox cache/Git writes required scoped escalation. Docker inspection hung; actual database work uses direct task-only JDBC, not Docker management.

Stored logs normalize encoding/newlines and trailing whitespace for Git diff hygiene; original test results and messages are preserved. First staged diff check found log whitespace, fixed in follow-up.
