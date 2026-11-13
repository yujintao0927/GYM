# TASK-NIGHT002 external identity regression

Run from the isolated repository root after building unchanged backend with Java21 / Maven3.10.0:

```powershell
$env:JAVA_HOME='D:/java_environment'
Set-Location backend
./mvnw.cmd -B -DskipTests package
Set-Location ..
python quality/identity-regression/local-run.py
python -m unittest discover -s quality/identity-regression -p test_tool.py -v
python quality/identity-regression/verify.py
```

`local-run.py` reads the approved password from the user's existing private global rules into child environment without printing it. Alternatively inject GYM_DB_PASSWORD and optionally GYM_DB_USERNAME into the current process securely, then execute `run.py` directly. Do not put secrets in shell arguments. No new packages required: Python3.10+ standard library and existing Java21/JDBC in the built jar.

Only the exact URL jdbc:mysql://localhost:3306/gym_dev_test_003b is accepted. The database must already exist; no schema creation, cleanup, DROP or unknown object mutation occurs. Startup explicitly sets schema and URL; Flyway performs its existing V1 validation/migration. The preflight rejects tracked backend differences from e20e499. Build the jar immediately before running; the tool does not independently attest arbitrary jar binaries. No old verify-local-process script is used.

The process uses an ephemeral loopback port, random HMAC per run, random synthetic CLI accounts and unique idempotency keys. Its own subprocesses only are terminated in finally. Fixture.java permits five bounded state mutations restricted to this run's random account names; it never changes schema. All synthetic rows remain. Source/account rate rows belong to this private HMAC. No rate-row deletion occurs. Do not run two instances in the same tool directory concurrently (private logs/results share paths).

A run is bounded: four two-thread barrier scenarios plus mutual cross-account logout, one round each; no throughput or duration target. Results contain only case labels, expected/actual status, error codes, request IDs and booleans. No body, password, account name, token or HMAC is delivered. Private process diagnostics remain ignored under .private; do not publish that directory. Runtime output is checked against every generated/received secret before evidence is saved; verify.py scans source and delivery against the locally configured password and opaque token patterns.

`run.py --reject-target` with an unauthorized URL returns nonzero before starting Java; test_tool.py proves changed expectations, leaked errors, malformed error envelopes and cache policy violations fail assertions. A regression failure exits 1, target rejection exits 2. An exception never counts as a passing case. results.json is replaced by the latest run, so retain earlier evidence separately when investigating failures.

See the delivery report and matrix for scope and gaps. These checks cover local simulated authentication, not production security certification.
