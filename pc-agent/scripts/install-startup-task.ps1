# Register ControlCom Agent to run at user logon (run once)
param(
    [string]$TaskName = "ControlComAgent"
)

$projectRoot = Split-Path -Parent $PSScriptRoot
$publishDir = Join-Path $projectRoot "publish"
$exePath = Join-Path $publishDir "ControlCom.Agent.exe"

if (-not (Test-Path $exePath)) {
    Write-Host "Publishing agent..."
    Push-Location $projectRoot
    dotnet publish -c Release -o publish
    Pop-Location
}

if (-not (Test-Path $exePath)) {
    throw "Agent executable not found at $exePath"
}

$action = New-ScheduledTaskAction -Execute $exePath -WorkingDirectory $publishDir
$trigger = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries
$principal = New-ScheduledTaskPrincipal -UserId $env:USERNAME -LogonType Interactive -RunLevel Limited

Register-ScheduledTask `
    -TaskName $TaskName `
    -Action $action `
    -Trigger $trigger `
    -Settings $settings `
    -Principal $principal `
    -Force

Write-Host "Scheduled task '$TaskName' registered."
Write-Host "Executable: $exePath"
