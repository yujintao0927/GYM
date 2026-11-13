# 自动条件覆盖矩阵

固定003B开发期契约预期；每行对应一次独立断言，顺序标签对应run.py。账号失败1–5及来源失败6–30是阈值序列，不作为独立安全能力计数；其他结构、状态、回执、跨主体及重启条件已超过40种。

| 条件 | 预期 | 实际 | 结果 |
| --- | --- | --- | --- |
| login WEB generation0 | 201 | 201 | PASS |
| generation0 | 不变量成立 | 成立 | PASS |
| contexts empty grants | 200 | 200 | PASS |
| no grants | 不变量成立 | 成立 | PASS |
| login MINI_PROGRAM | 201 | 201 | PASS |
| missing credential | 422 | 422 | PASS |
| missing entry | 422 | 422 | PASS |
| missing clientKind | 422 | 422 | PASS |
| extra role | 422 | 422 | PASS |
| extra accountId | 422 | 422 | PASS |
| extra unknown | 422 | 422 | PASS |
| credential kind 6 | 422 | 422 | PASS |
| credential login 7 | 422 | 422 | PASS |
| credential login 8 | 422 | 422 | PASS |
| credential login 9 | 422 | 422 | PASS |
| credential proof 10 | 422 | 422 | PASS |
| credential proof 11 | 422 | 422 | PASS |
| credential proof 12 | 422 | 422 | PASS |
| credential proof 13 | 422 | 422 | PASS |
| credential proof 14 | 422 | 422 | PASS |
| credential login 15 | 422 | 422 | PASS |
| credential kind 16 | 422 | 422 | PASS |
| invalid entry 17 | 422 | 422 | PASS |
| invalid entry 18 | 422 | 422 | PASS |
| invalid clientKind 19 | 422 | 422 | PASS |
| invalid clientKind 20 | 422 | 422 | PASS |
| invalid credential 21 | 422 | 422 | PASS |
| key missing | 422 | 422 | PASS |
| key space | 422 | 422 | PASS |
| key long | 422 | 422 | PASS |
| JSON broken | 422 | 422 | PASS |
| JSON array | 422 | 422 | PASS |
| JSON null | 422 | 422 | PASS |
| auth missing | 401 | 401 | PASS |
| auth lowercase | 401 | 401 | PASS |
| auth junk | 401 | 401 | PASS |
| auth cookie | 422 | 422 | PASS |
| cookie plus bearer | 422 | 422 | PASS |
| entry merchant | 201 | 201 | PASS |
| entry grants merchant | 不变量成立 | 成立 | PASS |
| entry coach | 201 | 201 | PASS |
| entry grants coach | 不变量成立 | 成立 | PASS |
| entry platform-admin | 201 | 201 | PASS |
| entry grants platform-admin | 不变量成立 | 成立 | PASS |
| entry platform-support | 201 | 201 | PASS |
| entry grants platform-support | 不变量成立 | 成立 | PASS |
| valid password 12 bytes | 201 | 201 | PASS |
| valid password 72 bytes | 201 | 201 | PASS |
| valid password 72 UTF8 bytes | 201 | 201 | PASS |
| valid key length 1 | 201 | 201 | PASS |
| valid key length 128 | 201 | 201 | PASS |
| refresh extra field | 422 | 422 | PASS |
| refresh missing generation | 422 | 422 | PASS |
| login Cookie rejected | 422 | 422 | PASS |
| login first key | 201 | 201 | PASS |
| login receipt | 409 | 409 | PASS |
| login mismatch | 409 | 409 | PASS |
| refresh generation0 | 201 | 201 | PASS |
| rotation expiry fixed | 不变量成立 | 成立 | PASS |
| old access rejected | 401 | 401 | PASS |
| refresh same key receipt | 409 | 409 | PASS |
| refresh changed generation receipt | 409 | 409 | PASS |
| refresh different key reuse | 401 | 401 | PASS |
| reuse revokes access | 401 | 401 | PASS |
| cross session key | 409 | 409 | PASS |
| generation negative | 401 | 401 | PASS |
| generation future | 401 | 401 | PASS |
| generation string | 422 | 422 | PASS |
| generation boolean | 422 | 422 | PASS |
| generation float | 422 | 422 | PASS |
| cross account logout | 404 | 404 | PASS |
| victim remains active | 200 | 200 | PASS |
| logout | 200 | 200 | PASS |
| logout access | 401 | 401 | PASS |
| logout refresh | 401 | 401 | PASS |
| logout repeated old bearer | 401 | 401 | PASS |
| restart access persistence | 200 | 200 | PASS |
| restart refresh receipt | 409 | 409 | PASS |
| restart new access | 200 | 200 | PASS |
| fixture accessExpired | 不变量成立 | 成立 | PASS |
| accessExpired contexts | 401 | 401 | PASS |
| fixture familyExpired | 不变量成立 | 成立 | PASS |
| family expiration | 401 | 401 | PASS |
| fixture epoch | 不变量成立 | 成立 | PASS |
| epoch contexts | 401 | 401 | PASS |
| fixture closed | 不变量成立 | 成立 | PASS |
| closed proven login | 403 | 403 | PASS |
| closed contexts | 401 | 401 | PASS |
| fixture disabled | 不变量成立 | 成立 | PASS |
| disabled credential | 401 | 401 | PASS |
| concurrent login same key | 不变量成立 | 成立 | PASS |
| concurrent refresh True | 不变量成立 | 成立 | PASS |
| concurrent refresh False | 不变量成立 | 成立 | PASS |
| concurrent reuse revocation | 401 | 401 | PASS |
| logout refresh bounded ordering | 不变量成立 | 成立 | PASS |
| logout race final old access | 401 | 401 | PASS |
| logout winner prevents refresh | 不变量成立 | 成立 | PASS |
| cross account mutual logout | 不变量成立 | 成立 | PASS |
| DB digest generation invariant True | 不变量成立 | 成立 | PASS |
| DB digest generation invariant False | 不变量成立 | 成立 | PASS |
| account failure 0 | 401 | 401 | PASS |
| account failure 1 | 401 | 401 | PASS |
| account failure 2 | 401 | 401 | PASS |
| account failure 3 | 401 | 401 | PASS |
| account failure 4 | 401 | 401 | PASS |
| account limit ignores XFF | 429 | 429 | PASS |
| source failure 6 | 401 | 401 | PASS |
| source failure 7 | 401 | 401 | PASS |
| source failure 8 | 401 | 401 | PASS |
| source failure 9 | 401 | 401 | PASS |
| source failure 10 | 401 | 401 | PASS |
| source failure 11 | 401 | 401 | PASS |
| source failure 12 | 401 | 401 | PASS |
| source failure 13 | 401 | 401 | PASS |
| source failure 14 | 401 | 401 | PASS |
| source failure 15 | 401 | 401 | PASS |
| source failure 16 | 401 | 401 | PASS |
| source failure 17 | 401 | 401 | PASS |
| source failure 18 | 401 | 401 | PASS |
| source failure 19 | 401 | 401 | PASS |
| source failure 20 | 401 | 401 | PASS |
| source failure 21 | 401 | 401 | PASS |
| source failure 22 | 401 | 401 | PASS |
| source failure 23 | 401 | 401 | PASS |
| source failure 24 | 401 | 401 | PASS |
| source failure 25 | 401 | 401 | PASS |
| source failure 26 | 401 | 401 | PASS |
| source failure 27 | 401 | 401 | PASS |
| source failure 28 | 401 | 401 | PASS |
| source failure 29 | 401 | 401 | PASS |
| source failure 30 | 401 | 401 | PASS |
| source limit XFF cannot bypass | 429 | 429 | PASS |
| retry after bounded | 不变量成立 | 成立 | PASS |
| default health | 不变量成立 | 成立 | PASS |
| default denies POST/v1/sessions | 不变量成立 | 成立 | PASS |
| default denies POST/v1/sessions/refresh | 不变量成立 | 成立 | PASS |
| default denies GET/v1/me/contexts | 不变量成立 | 成立 | PASS |
| default denies DELETE/v1/sessions/unknown | 不变量成立 | 成立 | PASS |
