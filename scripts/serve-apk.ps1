# ControlCom APK를 같은 Wi-Fi 폰으로 전송합니다.
# Google Drive는 APK 다운로드를 차단하는 경우가 많습니다.

param(
    [int]$Port = 8765,
    [string]$ApkPath = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ApkPath)) {
    $ApkPath = Join-Path $PSScriptRoot "..\dist\ControlCom-1.0.0.apk"
}

$ApkPath = (Resolve-Path $ApkPath).Path
$apkName = Split-Path $ApkPath -Leaf

# PC LAN IP 찾기
$ip = Get-NetIPAddress -AddressFamily IPv4 |
    Where-Object {
        $_.IPAddress -notlike "127.*" -and
        $_.PrefixOrigin -ne "WellKnown"
    } |
    Select-Object -First 1 -ExpandProperty IPAddress

if (-not $ip) {
    throw "LAN IP를 찾지 못했습니다. PC가 Wi-Fi에 연결되어 있는지 확인하세요."
}

$url = "http://${ip}:${Port}/"

Write-Host "========================================"
Write-Host " ControlCom APK 전송 서버"
Write-Host "========================================"
Write-Host " APK: $ApkPath"
Write-Host ""
Write-Host " 폰 브라우저(Chrome)에서 아래 주소를 여세요:"
Write-Host " $url"
Write-Host ""
Write-Host " 같은 Wi-Fi에 연결되어 있어야 합니다."
Write-Host " 종료: Ctrl+C"
Write-Host "========================================"

$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://+:$Port/")
$listener.Start()

try {
    while ($listener.IsListening) {
        $context = $listener.GetContext()
        $request = $context.Request
        $response = $context.Response

        try {
            if ($request.Url.AbsolutePath -eq "/" -or $request.Url.AbsolutePath -eq "/index.html") {
                $html = @"
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>ControlCom 설치</title>
  <style>
    body { font-family: sans-serif; background:#121318; color:#e4e1e9; padding:24px; }
    a.btn { display:block; background:#7c9cff; color:#0b1b3a; text-align:center;
             padding:20px; border-radius:12px; font-size:20px; font-weight:bold;
             text-decoration:none; margin-top:24px; }
    p { line-height:1.6; }
  </style>
</head>
<body>
  <h1>ControlCom</h1>
  <p>아래 버튼을 눌러 APK를 다운로드한 뒤 설치하세요.</p>
  <p>설치가 막히면:<br>설정 → 보안 → <b>내 파일</b> 또는 <b>Chrome</b>에서 알 수 없는 앱 설치 허용</p>
  <a class="btn" href="/download">APK 다운로드</a>
</body>
</html>
"@
                $bytes = [System.Text.Encoding]::UTF8.GetBytes($html)
                $response.ContentType = "text/html; charset=utf-8"
                $response.ContentLength64 = $bytes.Length
                $response.OutputStream.Write($bytes, 0, $bytes.Length)
            }
            elseif ($request.Url.AbsolutePath -eq "/download") {
                $fileBytes = [System.IO.File]::ReadAllBytes($ApkPath)
                $response.ContentType = "application/vnd.android.package-archive"
                $response.Headers.Add("Content-Disposition", "attachment; filename=`"$apkName`"")
                $response.ContentLength64 = $fileBytes.Length
                $response.OutputStream.Write($fileBytes, 0, $fileBytes.Length)
                Write-Host "다운로드 요청 처리됨: $($request.RemoteEndPoint)"
            }
            else {
                $response.StatusCode = 404
            }
        }
        finally {
            $response.Close()
        }
    }
}
finally {
    $listener.Stop()
    $listener.Close()
}
