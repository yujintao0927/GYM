# Run after verify with GYM_DB_URL/USERNAME/PASSWORD/IAM_HMAC_KEY in this process.
# All assertions print status labels only; raw credentials/tokens stay in memory.
$ErrorActionPreference='Stop'
if($env:GYM_DB_URL -ne 'jdbc:mysql://localhost:3306/gym_dev'){throw 'Explicit gym_dev process verification URL required'}
$java=Join-Path $env:JAVA_HOME 'bin/java.exe'
$jar=Join-Path $PSScriptRoot '../target/gym-0.0.1.jar'
$login='process_'+[Guid]::NewGuid().ToString('N')
$password=[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
$info=[System.Diagnostics.ProcessStartInfo]::new($java)
$info.CreateNoWindow=$true;$info.UseShellExecute=$false;$info.RedirectStandardInput=$true;$info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true
foreach($arg in @('-Dloader.main=com.gym.identity.local.LocalAccountCli','-cp',$jar,'org.springframework.boot.loader.launch.PropertiesLauncher',$login,'--password-stdin')){$info.ArgumentList.Add($arg)}
$cli=[System.Diagnostics.Process]::Start($info)
$out=$cli.StandardOutput.ReadToEndAsync();$err=$cli.StandardError.ReadToEndAsync()
$cli.StandardInput.WriteLine($password);$cli.StandardInput.Close();$cli.WaitForExit()
$cliOutput=$out.Result+$err.Result
if($cli.ExitCode -ne 0 -or $cliOutput.Contains($password)){throw 'CLI verification failed (output withheld)'}
Write-Output 'CLI_FRESH_PROCESS=PASS'
$server=$null
function Start-Gym([bool]$local){
 $script:port=[System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback,0);$script:port.Start();$chosen=$script:port.LocalEndpoint.Port;$script:port.Stop();$script:port=$chosen
 $arguments=@('-jar',$jar,"--server.port=$chosen")
 if($local){$arguments+='--spring.profiles.active=local-dev'}
 $suffix=if($local){'local'}else{'default'}
 $script:server=Start-Process -FilePath $java -ArgumentList $arguments -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $PSScriptRoot "../target/process-$suffix-out.log") -RedirectStandardError (Join-Path $PSScriptRoot "../target/process-$suffix-err.log")
 for($attempt=0;$attempt -lt 100;$attempt++){
  if($script:server.HasExited){throw 'Application exited before readiness (inspect private log)'}
  try{$h=Invoke-WebRequest "http://127.0.0.1:$chosen/actuator/health" -TimeoutSec 2;if($h.StatusCode -eq 200){return}}catch{}
  Start-Sleep -Milliseconds 200
 }
 throw 'Application readiness timeout'
}
function Stop-Gym {if($null -ne $script:server -and !$script:server.HasExited){Stop-Process -Id $script:server.Id;$script:server.WaitForExit()};$script:server=$null}
function Request-Gym([string]$method,[string]$path,$body,[string]$bearer,[string]$key,[int]$expected){
 $headers=@{};if($bearer){$headers.Authorization="Bearer $bearer"};if($key){$headers['Idempotency-Key']=$key}
 $params=@{Uri="http://127.0.0.1:$script:port$path";Method=$method;Headers=$headers;SkipHttpErrorCheck=$true}
 if($null -ne $body){$params.ContentType='application/json';$params.Body=($body|ConvertTo-Json -Depth 5 -Compress)}
 $response=Invoke-WebRequest @params
 if($response.StatusCode -ne $expected){throw "Unexpected status for $method $path expected $expected got $($response.StatusCode) (body withheld)"}
 $raw=if($response.Content -is [byte[]]){[System.Text.Encoding]::UTF8.GetString($response.Content)}else{[string]$response.Content}
 return ($raw|ConvertFrom-Json)
}
try{
 Start-Gym $true
 $body=@{credential=@{kind='LOCAL_PASSWORD';login=$login;proof=$password};entry='user-web';clientKind='WEB'}
 $tokens=(Request-Gym POST '/v1/sessions' $body '' 'process-login' 201).data
 if($tokens.generation -ne 0){throw 'Initial generation missing'}
 $context=(Request-Gym GET '/v1/me/contexts' $null $tokens.sessionToken '' 200).data
 if($context.staffScopes.Count -ne 0 -or $context.entitlementSources.Count -ne 0){throw 'Unexpected grants'}
 $rotated=(Request-Gym POST '/v1/sessions/refresh' @{refreshToken=$tokens.refreshToken;generation=$tokens.generation} '' 'process-refresh' 201).data
 $null=Request-Gym GET '/v1/me/contexts' $null $tokens.sessionToken '' 401
 Write-Output 'HTTP_LOGIN_CONTEXT_ROTATION_OLD_ACCESS_REJECT=PASS'
 Stop-Gym
 Start-Gym $true
 $null=Request-Gym GET '/v1/me/contexts' $null $rotated.sessionToken '' 200
 $retry=Request-Gym POST '/v1/sessions' $body '' 'process-login' 409
 if($retry.code -ne 'REAUTH_REQUIRED'){throw 'Receipt did not persist'}
 $null=Request-Gym DELETE "/v1/sessions/$($tokens.sessionId)" $null $rotated.sessionToken '' 200
 $null=Request-Gym POST '/v1/sessions/refresh' @{refreshToken=$rotated.refreshToken;generation=$rotated.generation} '' 'process-after-logout' 401
 Write-Output 'RESTART_PERSISTENCE_RECEIPT_LOGOUT_REFRESH_REJECT=PASS'
 Stop-Gym
 Start-Gym $false
 $health=Request-Gym GET '/actuator/health' $null '' '' 200
 if($health.status -ne 'UP' -or @($health.PSObject.Properties.Name).Count -ne 1){throw 'Unexpected health payload'}
 $null=Request-Gym GET '/v1/me/contexts' $null $rotated.sessionToken '' 403
 $null=Request-Gym POST '/v1/sessions' $body '' 'default-disabled' 403
 Write-Output 'DEFAULT_PROFILE_NEW_PROCESS_BOUNDARY=PASS'
}finally{Stop-Gym;$password=$null;$tokens=$null;$rotated=$null;$cliOutput=$null}
