# ControlCom Agent self-contained publish
$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$publishDir = Join-Path $projectRoot "publish"

Push-Location $projectRoot
dotnet publish ControlCom.Agent.csproj -c Release -r win-x64 --self-contained true -o $publishDir /p:PublishSingleFile=true /p:IncludeNativeLibrariesForSelfExtract=true
Pop-Location

Write-Host "Published to: $publishDir"
Write-Host "Run: $publishDir\ControlCom.Agent.exe"
