param()
$ErrorActionPreference='Stop'
if (!$env:GYM_DB_USERNAME -or !$env:GYM_DB_PASSWORD) { throw 'Inject GYM_DB_USERNAME/GYM_DB_PASSWORD in process environment; never pass credentials on CLI' }
$taskRoot=Resolve-Path (Join-Path $PSScriptRoot '../../..')
Push-Location (Join-Path $taskRoot 'backend')
try { & .\mvnw.cmd -f ../modules/support-core/pom.xml -B clean verify; if($LASTEXITCODE -ne 0){throw 'Verification failed'} } finally { Pop-Location }
