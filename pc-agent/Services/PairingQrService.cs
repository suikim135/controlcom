using QRCoder;

namespace ControlCom.Agent.Services;

public static class PairingQrService
{
    public static string BuildPayload(string ip, int port, string code)
    {
        return $"controlcom://pair?ip={Uri.EscapeDataString(ip)}&port={port}&code={Uri.EscapeDataString(code)}";
    }

    public static string ToBase64Png(string payload)
    {
        using var generator = new QRCodeGenerator();
        using var data = generator.CreateQrCode(payload, QRCodeGenerator.ECCLevel.Q);
        var png = new PngByteQRCode(data);
        var bytes = png.GetGraphic(12);
        return Convert.ToBase64String(bytes);
    }

    public static string BuildPairingHtml(string ip, int port, string code)
    {
        var payload = BuildPayload(ip, port, code);
        var qrBase64 = ToBase64Png(payload);
        return $@"<!DOCTYPE html>
<html lang=""ko"">
<head>
  <meta charset=""utf-8"">
  <meta name=""viewport"" content=""width=device-width, initial-scale=1"">
  <title>ControlCom 페어링</title>
  <style>
    body {{ font-family: Segoe UI, sans-serif; background:#121318; color:#e4e1e9; text-align:center; padding:32px; }}
    .card {{ background:#1b1b1f; border-radius:16px; padding:24px; max-width:420px; margin:0 auto; }}
    img {{ width:260px; height:260px; background:#fff; padding:12px; border-radius:12px; }}
    .code {{ font-size:32px; letter-spacing:8px; margin:16px 0; color:#7c9cff; }}
    p {{ line-height:1.6; }}
  </style>
</head>
<body>
  <div class=""card"">
    <h1>ControlCom PC 연결</h1>
    <p>Android 앱에서 QR 코드를 스캔하거나 아래 코드를 입력하세요.</p>
    <img alt=""pairing qr"" src=""data:image/png;base64,{qrBase64}"">
    <div class=""code"">{code}</div>
    <p>PC IP: {ip}<br>포트: {port}</p>
  </div>
</body>
</html>";
    }
}
