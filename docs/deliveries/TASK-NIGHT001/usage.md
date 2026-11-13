# Local health API

Only local-dev exposes these routes. Default profile denies business access. Secrets/Authorization headers must be supplied from a private client environment, never command arguments or tracked files. Executable synthetic example: `python tools/verify-health-process.py` after `mvnw.cmd -B clean verify` and private environment injection (see backend README).

POST /v1/me/health-records with Idempotency-Key:
```json
{"kind":"WEIGHT","occurredAt":"2026-11-13T08:00:00.000+08:00","payload":{"weightKg":"70.125"}}
```
DIET payload: {foodName,portionText}; TRAINING: {name,durationMinutes}. Measurements POST /v1/me/body-measurements omits kind and accepts a nonempty subset of heightCm/bodyFatPercent/waistCm/hipCm/chestCm. Decimals are strings, positive, maximum three fractional digits and 9999999.999; bodyFatPercent alone permits 0 to 100. No diagnostic or nutritional calculation.

PATCH /{id} uses exactly occurredAt/payload/expectedVersion; DELETE /{id}?expectedVersion=1. Kind cannot change. Foreign id returns 404 for all three operations. Same normalized POST/key returns current record; changed payload is 409; deleted replay is 404 and cannot restore body. Delete retry is 404.

GET list optionally takes kind (records only), from/to RFC3339 ([from,to)), limit 1..100 and cursor. Result {items,nextCursor,asOf}. Order is ascending occurredAt/id. Cursor binds actor/category/filter/limit; follow it unchanged. Offset plus signs require URL encoding. Existing concurrent edits can move records; asOf is not an immutable historical snapshot. Maximum time precision is milliseconds, seconds and timezone required.

PUT /v1/me/weight-goal:
```json
{"targetWeightKg":"65","targetDate":"2027-01-01","expectedVersion":0}
```
Initial PUT version 0 creates version 1; subsequent PUT/DELETE requires current version. GET without goal is 404. Date is an optional local calendar date. No automatically generated target or advice.

Future frontend mapping (separate approval/integration): use opaque server id and version, preserve decimal strings, map timezone explicitly, keep cursor opaque, send no accountId/role/entry on health calls. No Web/Mini changes or real frontend integration are claimed.
