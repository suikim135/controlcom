# Generate Android release keystore (run once, keep backup safe)
$ErrorActionPreference = "Stop"
$androidDir = Join-Path $PSScriptRoot "..\android"
$keystoreDir = Join-Path $androidDir "keystore"
$keystoreFile = Join-Path $keystoreDir "controlcom-release.jks"
$propsFile = Join-Path $androidDir "keystore.properties"

if (Test-Path $keystoreFile) {
    Write-Host "Keystore already exists: $keystoreFile"
    exit 0
}

$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
New-Item -ItemType Directory -Force -Path $keystoreDir | Out-Null

$keytool = Get-Command keytool -ErrorAction SilentlyContinue
if (-not $keytool) {
  $javaHome = (Get-Command java -ErrorAction SilentlyContinue).Source | Split-Path | Split-Path
  $keytool = Join-Path $javaHome "bin\keytool.exe"
}

& $keytool -genkeypair -v `
  -keystore $keystoreFile `
  -alias controlcom `
  -keyalg RSA -keysize 2048 -validity 10000 `
  -storepass controlcom123 `
  -keypass controlcom123 `
  -dname "CN=ControlCom, OU=Mobile, O=ControlCom, L=Seoul, ST=Seoul, C=KR"

@"
storeFile=keystore/controlcom-release.jks
storePassword=controlcom123
keyAlias=controlcom
keyPassword=controlcom123
"@ | Set-Content -Path $propsFile -Encoding UTF8

Write-Host "Created keystore: $keystoreFile"
Write-Host "Created properties: $propsFile"
Write-Host "IMPORTANT: Change passwords before Play Store production release."
