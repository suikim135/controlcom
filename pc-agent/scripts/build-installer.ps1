# Build ControlCom Agent installer (requires Inno Setup 6)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

& (Join-Path $PSScriptRoot "publish.ps1")

$iscc = @(
    "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe",
    "$env:ProgramFiles\Inno Setup 6\ISCC.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1

if (-not $iscc) {
    Write-Host "Inno Setup 6 not found. Published files are in pc-agent\publish" -ForegroundColor Yellow
    Write-Host "Install Inno Setup: https://jrsoftware.org/isinfo.php" -ForegroundColor Yellow
    exit 0
}

& $iscc (Join-Path $root "installer\setup.iss")
Write-Host "Installer: $root\installer\Output\ControlComAgent-Setup.exe" -ForegroundColor Green
