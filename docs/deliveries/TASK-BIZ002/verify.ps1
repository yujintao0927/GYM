$ErrorActionPreference = 'Stop'
if (-not $env:GYM_DB_USERNAME -or -not $env:GYM_DB_PASSWORD) { throw 'Inject local MySQL credentials through environment; never print them.' }
$taskRoot = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
Push-Location (Join-Path $taskRoot 'backend')
try {
    & ./mvnw.cmd -f ../modules/trade-core/pom.xml -B clean verify
    if ($LASTEXITCODE -ne 0) { throw "Verification failed: $LASTEXITCODE" }
} finally { Pop-Location }
