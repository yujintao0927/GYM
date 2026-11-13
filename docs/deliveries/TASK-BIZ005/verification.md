# Verification — TASK-BIZ005

Final exact command from isolated worktree/backend: `.\mvnw.cmd -f ../modules/support-core/pom.xml -B clean verify`. JAVA_HOME=D:/java_environment (Oracle JDK21.0.2). Maven Wrapper 3.10.0. Exit 0; 15 tests, zero failures/errors/skips, 2026-11-13 23:33 Asia/Shanghai. Full output: [verify-final.log](verify-final.log). Credentials injected through GYM_DB_USERNAME/GYM_DB_PASSWORD environment only; replay with [verify.ps1](verify.ps1).

Resolved from actual Surefire classpath: spring-jdbc/spring-tx 7.0.10, Connector/J 9.7.0, flyway-core/flyway-mysql 12.4.0, fixed Boot4.1.2 parent/BOM.

Real JDBC MySQL26.7: created only gym_dev_test_biz005, installed only V7 and five sup_ tables plus Flyway history. Metadata guard rejects unknown objects, migrations and databases. V7 first install succeeded, subsequent validate/migrate reports no migration necessary. Tests retain UUID synthetic records between runs; no table truncation/drop/clean. Single test JVM, concurrent test uses two threads; DriverManagerDataSource opens short-lived connections, no shared pool. No other business test schemas touched.

SQL SHA256: B9AA96D522F0CD2AC55723DEA7C64A82117FFCAF5BB5BE4ED8071CCA32D4394E. Installed V7 remains unchanged.

## Assertion evidence

- Domain: invalid premature appeal rejected; escalation/disposition/appeal state route.
- Idempotency: same body same key returns same ticket; differing body conflicts; reconnect via independent DataSource connections preserves ticket and single history entry.
- Isolation/current authority: revoked DENY, unavailable authorization503, foreign owner404, other-store403, old assignee after reassignment404. Default-deny runtime port fixture test.
- Refund: foreign fact reference404 and missing store dependency503; no funds or entitlement command exists.
- Case trajectory: recommendation leaves state OPEN, disposition then appeal retains four history entries. Pagination stable sequence and ticket scope.
- Attachment: malformed content rejected with zero blobs, private valid PNG roundtrip, foreign attachment denied; wrong MIME and enormous pixel header rejected before decode.
- Failure injection after attachment insert: blob absent, version unchanged, audit/outbox counts unchanged, no receipt, same key retry succeeds. Post-open failure: no ticket/receipt partial commit.
- Concurrency: barrier synchronizes two ESCALATE commands for version0, exactly one succeeds and other409; version1 and exactly two trajectory entries.
- Event privacy: exactly two events for two mutations, payload excludes synthetic/private text. Database guard refusal tested with metadata fixture without connecting forbidden schemas.

## Failure-to-fix record

1. Initial domain/application test-first build failed on missing implementation classes, exit1; then implemented module.
2. First complete real MySQL run: 11 tests, 10 pass, INVALID_IMAGE on broken base64 positive fixture. Replaced fixture with JDK ImageIO-generated PNG; no decoder validation removed.
3. Independent review identified decode allocation before dimension validation and missing post-insert attachment rollback assertions. Wrong-media regression actually failed (expected SupportFailure, none thrown). Added metadata format/pixel validation then suite13/13 passed. Added scope/event/guard checks and exact JDK21 final suite15/15 passed.
4. Docker CLI probes hung; interrupted only those client processes. Shared Docker/MySQL was not stopped. Bounded TCP probe succeeded; JDBC performed authorized database work directly.

Warnings retained: MySQL26.7 newer than Flyway verified9.4, Maven optional repository metadata401, Mockito/JDK dynamic agent warning. No validation disabled; no production compatibility guarantee.

Real provider IAM/store/trade transactions not tested; all external ports are explicit test-source fixtures. See contract.md for I/U/L gates and revocation fencing requirement.
