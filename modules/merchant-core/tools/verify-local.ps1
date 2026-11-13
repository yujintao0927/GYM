param([switch]$UsePersonalRules)
$ErrorActionPreference='Stop'
if($UsePersonalRules){
 $rules=Get-Content (Join-Path $env:USERPROFILE '.codex/AGENTS.md') -Raw
 $env:GYM_DB_USERNAME=[regex]::Match($rules,'用户名[：:]\s*([^\r\n]+)').Groups[1].Value.Trim()
 $env:GYM_DB_PASSWORD=[regex]::Match($rules,'密码[：:]\s*([^\r\n]+)').Groups[1].Value.Trim()
}
if(!$env:GYM_DB_USERNAME -or !$env:GYM_DB_PASSWORD){throw 'Inject GYM_DB_USERNAME and GYM_DB_PASSWORD privately'}
$env:GYM_BIZ001_DB_URL='jdbc:mysql://localhost:3306/gym_dev_test_biz001'
$repoRoot=(Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
Push-Location (Join-Path $repoRoot 'backend')
try { & .\mvnw.cmd -f ../modules/merchant-core/pom.xml -B clean verify; $result=$LASTEXITCODE }
finally{Pop-Location}
exit $result
