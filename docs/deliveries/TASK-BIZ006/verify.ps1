$ErrorActionPreference = 'Stop'
if ($env:RNK_DB_URL -ne 'jdbc:mysql://localhost:3306/gym_dev_test_biz006') { throw 'Explicit BIZ006 URL required' }
if (!$env:GYM_DB_USERNAME -or !$env:GYM_DB_PASSWORD) { throw 'Inject credentials through process environment' }
$taskRoot = Resolve-Path (Join-Path $PSScriptRoot '../../..')
Push-Location (Join-Path $taskRoot 'backend')
try { & .\mvnw.cmd -f ../modules/ranking-core/pom.xml -B clean verify; if ($LASTEXITCODE -ne 0) { throw 'BIZ006 verify failed' } } finally { Pop-Location }
