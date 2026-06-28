using ControlCom.Agent.Models;
using ControlCom.Agent.Services;

namespace ControlCom.Agent.Middleware;

public sealed class LanOnlyMiddleware
{
    private readonly RequestDelegate _next;
    private readonly bool _allowLanOnly;

    public LanOnlyMiddleware(RequestDelegate next, IConfiguration configuration)
    {
        _next = next;
        _allowLanOnly = configuration.GetValue("Agent:AllowLanOnly", true);
    }

    public async Task InvokeAsync(HttpContext context)
    {
        if (!_allowLanOnly)
        {
            await _next(context);
            return;
        }

        var remoteIp = context.Connection.RemoteIpAddress;
        if (!LanAddressHelper.IsPrivateLan(remoteIp))
        {
            context.Response.StatusCode = StatusCodes.Status403Forbidden;
            await context.Response.WriteAsJsonAsync(new ErrorResponse
            {
                Error = "forbidden",
                Message = "Only private LAN clients are allowed."
            });
            return;
        }

        await _next(context);
    }
}
