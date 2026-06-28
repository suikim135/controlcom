# Drive 업로드용 - APK를 .bin으로 복사 (다운로드 후 이름만 .apk로 변경)
$src = Join-Path $PSScriptRoot "..\dist\ControlCom-1.0.0.apk"
$dest = Join-Path $PSScriptRoot "..\dist\ControlCom-1.0.0.bin"
Copy-Item $src $dest -Force
Write-Host "Created: $dest"
Write-Host "Drive에 이 .bin 파일을 올린 뒤, 폰에서 다운로드하고 이름을 ControlCom-1.0.0.apk 로 바꾸세요."
