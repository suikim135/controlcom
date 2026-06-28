using ControlCom.Agent.Models;
using ControlCom.Agent.Services;

namespace ControlCom.Agent.Middleware;

public sealed class PairingRateLimitMiddleware
{
    private readonly RequestDelegate _next;
    private readonly TokenService _tokenService;

    public PairingRateLimitMiddleware(RequestDelegate next, TokenService tokenService)
    {
        _next = next;
        _tokenService = tokenService;
    }

    public async Task InvokeAsync(HttpContext context)
    {
        var path = context.Request.Path.Value ?? string.Empty;
        if (!path.StartsWith("/api/auth/pair", StringComparison.OrdinalIgnoreCase))
        {
            await _next(context);
            return;
        }

        if (_tokenService.IsPairingLockedOut())
        {
            var retryAfter = _tokenService.GetPairingLockoutSecondsRemaining();
            context.Response.Headers.RetryAfter = retryAfter.ToString();
            context.Response.StatusCode = StatusCodes.Status429TooManyRequests;
            await context.Response.WriteAsJsonAsync(new ErrorResponse
            {
                Error = "pairing_locked",
                Message = $"Too many failed pairing attempts. Try again in {retryAfter} seconds."
            });
            return;
        }

        await _next(context);
    }
}
