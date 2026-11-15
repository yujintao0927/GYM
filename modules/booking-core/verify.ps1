param([string]$JavaHome=$env:JAVA_HOME)
$ErrorActionPreference='Stop'
if($JavaHome){$env:JAVA_HOME=$JavaHome}
if(-not $env:GYM_DB_USERNAME){$env:GYM_DB_USERNAME='root'}
if(-not $env:GYM_DB_PASSWORD){
 $privateRules=Get-Content (Join-Path $env:USERPROFILE '.codex/AGENTS.md') -Raw
 $env:GYM_DB_PASSWORD=[regex]::Match($privateRules,'密码[：:]\s*([^\r\n]+)').Groups[1].Value.Trim()
 if(-not $env:GYM_DB_PASSWORD){throw 'Configure GYM_DB_PASSWORD privately'}
}
Push-Location (Join-Path $PSScriptRoot '../../backend')
try { & .\mvnw.cmd -f ../modules/booking-core/pom.xml -B clean verify; $result=$LASTEXITCODE }
finally {Pop-Location}
exit $result
