param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$backend = (Resolve-Path (Join-Path $PSScriptRoot '../../../backend')).Path
$logs = Join-Path $backend '.tools'
New-Item -ItemType Directory -Force $logs | Out-Null
$out = Join-Path $logs 'probe-stdout.log'
$err = Join-Path $logs 'probe-stderr.log'
$process = Start-Process -FilePath (Join-Path $JavaHome 'bin/java.exe') -ArgumentList @('-jar','target/gym-0.0.1.jar','--server.port=0','--server.address=127.0.0.1') -WorkingDirectory $backend -WindowStyle Hidden -PassThru -RedirectStandardOutput $out -RedirectStandardError $err
try {
    $deadline = [DateTime]::UtcNow.AddSeconds(60)
    $port = $null
    while ([DateTime]::UtcNow -lt $deadline) {
        if ($process.HasExited) { throw "Application exited: $($process.ExitCode)" }
        $match = [regex]::Match(('' + (Get-Content $out -Raw -ErrorAction SilentlyContinue)), 'Tomcat started on port (\d+)')
        if ($match.Success) { $port = $match.Groups[1].Value; break }
        Start-Sleep -Milliseconds 250
    }
    if (!$port) { throw 'Application did not start within 60 seconds' }
    $results = @()
    foreach ($path in @('/actuator/health','/actuator/health?showDetails=always','/v1/me/contexts','/v1/orders','/actuator/env','/actuator/configprops','/login','/logout','/unknown','/actuator/health/liveness')) {
        foreach ($forged in @($false,$true)) {
            $headers = @{}
            $requestPath = $path
            if ($forged) {
                $headers.Authorization = 'Bearer forged-identity'
                $requestPath += $(if ($path.Contains('?')) {'&'} else {'?'}) + 'entry=platform-admin&merchantId=other&storeId=other'
            }
            $r = Invoke-WebRequest -Uri "http://127.0.0.1:$port$requestPath" -Headers $headers -SkipHttpErrorCheck -TimeoutSec 10
            $body = if ($r.Content -is [byte[]]) { [Text.Encoding]::UTF8.GetString($r.Content) } else { [string]$r.Content }
            if ($path.StartsWith('/actuator/health') -and !$path.Contains('/liveness')) {
                if ($r.StatusCode -ne 200 -or $body -cne '{"status":"UP"}') { throw "Health mismatch: $($r.StatusCode) $body" }
            } elseif ($r.StatusCode -notin @(401,403)) { throw "Unexpected access: $path $($r.StatusCode)" }
            $results += [pscustomobject]@{path=$requestPath;forged=$forged;status=[int]$r.StatusCode;body=$body}
        }
    }
    foreach ($method in @('POST','HEAD','OPTIONS','PUT','DELETE')) {
        $r = Invoke-WebRequest -Uri "http://127.0.0.1:$port/actuator/health" -Method $method -SkipHttpErrorCheck -TimeoutSec 10
        if ($r.StatusCode -notin @(401,403)) { throw "Unexpected method access: $method $($r.StatusCode)" }
        $results += [pscustomobject]@{path='/actuator/health';method=$method;status=[int]$r.StatusCode}
    }
    $log = (Get-Content $out,$err -Raw) -join "`n"
    if ($log -match 'generated security password|jdbc:|HikariPool|Communications link failure') { throw 'Unexpected password or datasource startup log' }
    $results | ConvertTo-Json -Depth 4
} finally {
    if (!$process.HasExited) { Stop-Process -Id $process.Id; $process.WaitForExit() }
    Write-Host "Stopped own application PID $($process.Id)"
}
