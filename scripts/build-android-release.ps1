# Android release builds (AAB for Play Store)
$ErrorActionPreference = "Stop"
$root = Join-Path $PSScriptRoot "..\android"
$keystoreScript = Join-Path $PSScriptRoot "create-release-keystore.ps1"

if (-not (Test-Path (Join-Path $root "keystore.properties"))) {
    & $keystoreScript
}

$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
Push-Location $root
.\gradlew.bat bundleRelease assembleRelease --no-daemon
Pop-Location

$aab = Join-Path $root "app\build\outputs\bundle\release\app-release.aab"
$apk = Join-Path $root "app\build\outputs\apk\release\app-release.apk"
$dist = Join-Path $PSScriptRoot "..\dist"
New-Item -ItemType Directory -Force -Path $dist | Out-Null

if (Test-Path $aab) { Copy-Item $aab (Join-Path $dist "ControlCom-1.0.0.aab") -Force }
if (Test-Path $apk) { Copy-Item $apk (Join-Path $dist "ControlCom-1.0.0-release.apk") -Force }

Write-Host "AAB: $dist\ControlCom-1.0.0.aab"
Write-Host "APK: $dist\ControlCom-1.0.0-release.apk"
