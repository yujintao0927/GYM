# Execution ledger: docs/coordination/plans/NIGHT-BATCH.md / NIGHT001
Base 290e098dda56b9fd6fca1b42d75f560f3a07cd3a. Explicit user plan and autonomous execution override additional design gate.
Workspace: C:/Users/28795/.codex/visualizations/2026/11/13/01a1d560-5e3e-72a3-a240-82ce0c273768/task-night001
Branch: codex/task-night001-health-foundation
Preflight: IAM defensive-rejection transaction must not wrap health. Health uses TransactionTemplate directly and Account/Session authority locks in same transaction.
Database: gym_dev_test_night001 created with CREATE DATABASE IF NOT EXISTS. No gym_dev or 003b writes.
First red attempts blocked by sandbox then malformed private credential regex (encoding); these were configuration failures, not successful red proof. Initial integration run then failed 2 inherited IAM tests due to subclass Clock injection mismatch. Fixed by independent health test context. Do not claim perfect TDD.
verify2: Maven verify exit 0, 33 tests, 0 failure/error/skip. Original 28 retained plus 5 health tests. MySQL26.7/Flyway warning preserved. V1 unchanged; applied V1+V2; repeat migrate zero.
Ruling: retain canonical bounded decimal strings in record JSON in this revision rather than changing an already-applied V2 checksum ¡ª fails literal DECIMAL storage requirement; report as outstanding, never claim full acceptance readiness.

Follow-up to decimal ruling: resolved storage using six STORED generated DECIMAL(10,3) columns. Known task-owned unpublished V2 table was ALTERed without deletion; Flyway repair changed V2 checksum only to -1419392502. V1 unchanged. One-shot helper moved to ignored .tools; not delivered as normal test. Fresh final V2 first-migration proof remains absent.
Review: fresh reviewer identified explicit insert columns, accidental one-shot test, OpenAPI response/kind binding, RFC3339 seconds validation. All corrected; timestamp failure reproduced (422 expected, 201 actual), then strict seconds regex passed whole suite. Added cross-owner PATCH/DELETE, process second actor/paging and forced Account-fence logout orderings.
Final-verify: clean verify exit0, 37 tests passed, 0 skips/failures/errors. Additional pure unit boundary tests and exact migration versions added afterward; follow-up verification pending.

Delivery verification: Maven verify exit0, 40 tests, zero failures/errors/skips. Final process probe exit0 including two actors/filter/cursor. Source implementation saved as 942ca2dd4e963306011949d519dcfae4408929da. Final report/evidence pending commit and push.
