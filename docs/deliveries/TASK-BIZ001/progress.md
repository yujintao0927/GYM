# TASK-BIZ001 execution ledger
Plan: docs/coordination/plans/PAR-BIZ.md; spec: frozen business-module-ports-v1.
Base: b3ce453e92cc378a25dac27f7d1591eb01274baf; remote verified same; no duplicate task branch.
Scope: modules/merchant-core and this delivery only. V3/mch_/gym_dev_test_biz001.
Pre-flight: merchant ownership feeds stores; explicit store sets feed current authorization; service timestamps never derive calendar defaults.
Ruling: worktree under session writable visualization root, consistent with existing repository worktrees; no root ignore changes.
Step 1: explicit service endpoint tests written before domain code.
Environment: wrapper Maven 3.10.0; installed runtime observed Java22.0.1, Java21 runtime not yet located. Compiler target remains 21. Docker requires escalated access.
Step 2: real MySQL database initially absent; created only gym_dev_test_biz001 through explicit JDBC createDatabaseIfNotExist. V3 installed and repeated migration zero confirmed.
Step 3: first application RED raised UnsupportedOperationException at submitMerchant; first GREEN clean verify 9 tests, exit 0.
Step 4 RED: pagination unimplemented and delete replay failed RESOURCE_NOT_FOUND. Root cause: ordinary store lookup hides tombstones before receipt check. Fix: delete guard uses tombstone-inclusive lookup and reauthorizes current ownership; ordinary reads still hide deletion.
Ruling: bounded module-wide MySQL mutex serializes current decisions and writes. Correctness preferred over throughput for local M validation; integration must benchmark and preserve revocation ordering before changing lock granularity.
Ruling: Java22.0.1 with release21 is actual environment; user-provided JDK21 path reports22. Java21 runtime validation remains unproven.
Final review: one independent reviewer, no Critical; Important: normalize provider exceptions, map domain input to422, distinguish policy provenance. Added regression tests and verified RED logs red-review/red-policy/red-provider.
Final: fixed immutable input scope — scopeSnapshotSurvivesCallerMutationDuringPolicy RED→GREEN.
Final: fixed raw database/provider exception exposure and domain422 mapping — red-safety/red-review/red-provider to final suite.
Final: fixed policy provenance/fixture runtime isolation — fixturePolicyCannotEnterDefaultRuntimeBoundary RED→GREEN; grant event includes policy metadata.
Final: minor (deferred): compressed source formatting; no runtime behavior change, subsequent maintenance may format.
Final: Ruling: trusted StoreDirectory/currentStaffDecision reads intentionally have no HTTP actor parameter — frozen provider signatures require trusted in-process adapters, no public HTTP exists — integration must preserve that trust boundary.
Final verification: clean verify exit0, 27 tests (21 actual MySQL), no skipped; checksum -407201789. Source artifact class major65, runtime22.0.1. Private credential scan clean. Shared main remained b3ce453 with user's untracked AGENTS/WEB001 materials untouched.
Ruling: use this allowed delivery progress.md as executing-plans ledger rather than extra skill scratch — user file scope forbids unrelated paths — auxiliary skill scripts are not directly reusable, evidence is retained here.
Final whitespace gate: initial staged logs contained Maven trailing spaces; normalized only line-end whitespace/EOF blank lines; SQL untouched. Final baseline diff check clean.
