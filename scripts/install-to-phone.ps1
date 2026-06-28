# 폰 설치 파일 준비 + Wi-Fi 전송 서버 시작
# 카카오톡/구글드라이브는 APK 업로드를 막습니다.

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$dist = Join-Path $root "dist"
$apk = Join-Path $dist "ControlCom-1.0.0.apk"

if (-not (Test-Path $apk)) {
    Write-Host "APK가 없습니다. 먼저 android 폴더에서 빌드하세요." -ForegroundColor Red
    exit 1
}

# 채팅앱 우회용 (확장자만 다름, 내용은 APK와 동일)
$aliases = @(
    "ControlCom-설치파일.zip",
    "ControlCom-설치파일.txt",
    "ControlCom-설치파일.bin"
)
foreach ($name in $aliases) {
    Copy-Item $apk (Join-Path $dist $name) -Force
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " APK 전송 방법 (카톡/드라이브 APK 차단 우회)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "[방법 1] Wi-Fi 직접 전송 (가장 확실)" -ForegroundColor Yellow
Write-Host "  아래 서버가 켜지면 폰 Chrome에서 주소를 여세요."
Write-Host ""
Write-Host "[방법 2] Snapdrop (설치 없음)" -ForegroundColor Yellow
Write-Host "  PC/폰 같은 Wi-Fi → 둘 다 snapdrop.net 접속 → 파일 드래그"
Write-Host ""
Write-Host "[방법 3] USB" -ForegroundColor Yellow
Write-Host "  adb install $apk"
Write-Host ""
Write-Host "[방법 4] 카톡 우회 (안 될 수도 있음)" -ForegroundColor Yellow
Write-Host "  dist\ControlCom-설치파일.txt 를 카톡으로 전송"
Write-Host "  폰에서 받은 뒤 이름을 ControlCom.apk 로 변경 후 설치"
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 방화벽 (이미 있으면 스킵)
$ruleName = "ControlCom APK 8765"
$existing = Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue
if (-not $existing) {
    try {
        New-NetFirewallRule -DisplayName $ruleName -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8765 -Profile Private | Out-Null
        Write-Host "방화벽 규칙 추가됨 (Private Wi-Fi)" -ForegroundColor Green
    }
    catch {
        Write-Host "방화벽 규칙 추가 실패. 관리자 PowerShell로 다시 실행하세요." -ForegroundColor Red
    }
}

& (Join-Path $PSScriptRoot "serve-apk.ps1")
