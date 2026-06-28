using ControlCom.Agent.Handlers;
using ControlCom.Agent.Middleware;
using ControlCom.Agent.Models;
using ControlCom.Agent.Services;
using ControlCom.Agent.Tray;
using System.Diagnostics;

var builder = WebApplication.CreateBuilder(args);

var port = builder.Configuration.GetValue("Agent:Port", 7847);
builder.WebHost.UseUrls($"http://0.0.0.0:{port}");

builder.Services.AddSingleton<TokenService>();
builder.Services.AddSingleton<IPowerHandler, PowerHandler>();
builder.Services.AddSingleton<IAudioHandler, AudioHandler>();
builder.Services.AddSingleton<IDisplayHandler, DisplayHandler>();

var app = builder.Build();
app.UseMiddleware<LanOnlyMiddleware>();
app.UseMiddleware<PairingRateLimitMiddleware>();
app.UseMiddleware<BearerAuthMiddleware>();

var tokenService = app.Services.GetRequiredService<TokenService>();
var powerHandler = app.Services.GetRequiredService<IPowerHandler>();
var audioHandler = app.Services.GetRequiredService<IAudioHandler>();
var displayHandler = app.Services.GetRequiredService<IDisplayHandler>();

app.MapGet("/api/health", () => Results.Ok(new HealthResponse()));

app.MapGet("/api/pairing/info", () =>
{
    var localIp = NetworkHelper.GetLocalIPv4() ?? "127.0.0.1";
    if (tokenService.HasToken)
    {
        return Results.Ok(new PairingInfoResponse
        {
            Paired = true,
            Ip = localIp,
            Port = port
        });
    }

    var code = tokenService.GetPairingCode();
    var payload = PairingQrService.BuildPayload(localIp, port, code);
    return Results.Ok(new PairingInfoResponse
    {
        Paired = false,
        Ip = localIp,
        Port = port,
        Code = code,
        QrPayload = payload
    });
});

app.MapGet("/pair", () =>
{
    if (tokenService.HasToken)
    {
        return Results.Content("<html><body style='background:#121318;color:#e4e1e9;font-family:sans-serif;text-align:center;padding:40px'><h1>이미 페어링됨</h1><p>ControlCom 앱에서 연결하세요.</p></body></html>", "text/html; charset=utf-8");
    }

    var localIp = NetworkHelper.GetLocalIPv4() ?? "127.0.0.1";
    var code = tokenService.GetPairingCode();
    var html = PairingQrService.BuildPairingHtml(localIp, port, code);
    return Results.Content(html, "text/html; charset=utf-8");
});

app.MapPost("/api/auth/pair", (PairRequest request) =>
{
    if (tokenService.HasToken)
    {
        return Results.BadRequest(new ErrorResponse
        {
            Error = "already_paired",
            Message = "This agent is already paired."
        });
    }

    if (!tokenService.TryPair(request.Code, out var token))
    {
        if (tokenService.IsPairingLockedOut())
        {
            return Results.Json(new ErrorResponse
            {
                Error = "pairing_locked",
                Message = "Too many failed pairing attempts."
            }, statusCode: StatusCodes.Status429TooManyRequests);
        }

        return Results.Json(new ErrorResponse
        {
            Error = "invalid_code",
            Message = "Pairing code is invalid or expired."
        }, statusCode: StatusCodes.Status401Unauthorized);
    }

    Console.WriteLine("ControlCom: pairing completed.");
    return Results.Ok(new PairResponse { Token = token });
});

app.MapPost("/api/power/sleep", () =>
{
    powerHandler.Sleep();
    return Results.Ok(new OkResponse());
});

app.MapPost("/api/power/shutdown", async (IConfiguration config, CancellationToken cancellationToken) =>
{
    var saveDelay = config.GetValue("Agent:ShutdownSaveDelaySeconds", 5);
    var grace = config.GetValue("Agent:ShutdownGraceSeconds", 10);
    var response = await powerHandler.ShutdownWithSaveAsync(saveDelay, grace, cancellationToken);
    return Results.Ok(response);
});

app.MapGet("/api/audio/mute", () => Results.Ok(new MuteResponse { Muted = audioHandler.IsMuted() }));

app.MapPost("/api/audio/mute/toggle", () =>
{
    var muted = audioHandler.ToggleMute();
    return Results.Ok(new MuteResponse { Muted = muted });
});

app.MapGet("/api/display/mode", () => Results.Ok(new DisplayModeResponse { Mode = displayHandler.GetMode() }));

app.MapPost("/api/display/mode", (DisplayModeRequest request) =>
{
    try
    {
        var mode = displayHandler.SetMode(request.Mode);
        return Results.Ok(new DisplayModeResponse { Mode = mode });
    }
    catch (ArgumentException ex)
    {
        return Results.BadRequest(new ErrorResponse
        {
            Error = "invalid_mode",
            Message = ex.Message
        });
    }
    catch (Exception ex)
    {
        return Results.Problem(ex.Message, statusCode: StatusCodes.Status500InternalServerError);
    }
});

PrintStartupBanner(tokenService, port);

using var trayHost = new AgentTrayHost(
    tokenService,
    port,
    app.Services.GetRequiredService<IHostApplicationLifetime>());
trayHost.Start();

OpenPairingPageIfNeeded(tokenService, port);
app.Run();

static void OpenPairingPageIfNeeded(TokenService tokenService, int port)
{
    if (tokenService.HasToken)
    {
        return;
    }

    try
    {
        Process.Start(new ProcessStartInfo
        {
            FileName = $"http://localhost:{port}/pair",
            UseShellExecute = true
        });
    }
    catch
    {
        // Browser launch is best-effort.
    }
}

static void PrintStartupBanner(TokenService tokenService, int port)
{
    Console.WriteLine("========================================");
    Console.WriteLine(" ControlCom Agent");
    Console.WriteLine($" Listening on http://0.0.0.0:{port}");
    Console.WriteLine("========================================");

    if (!tokenService.HasToken)
    {
        var code = tokenService.GetPairingCode();
        Console.WriteLine($" Pairing code: {code}");
        Console.WriteLine(" Enter this code in the Android app to pair.");
    }
    else
    {
        Console.WriteLine(" Already paired. Ready for commands.");
    }

    Console.WriteLine();
    Console.WriteLine(" Firewall: allow inbound TCP on this port for private networks.");
    Console.WriteLine(" Auto-start: run scripts/install-startup-task.ps1 once as admin.");
    Console.WriteLine("========================================");
}
