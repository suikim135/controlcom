using ControlCom.Agent.Services;

namespace ControlCom.Agent.Middleware;

public sealed class BearerAuthMiddleware
{
    private readonly RequestDelegate _next;
    private readonly TokenService _tokenService;

    public BearerAuthMiddleware(RequestDelegate next, TokenService tokenService)
    {
        _next = next;
        _tokenService = tokenService;
    }

    public async Task InvokeAsync(HttpContext context)
    {
        var path = context.Request.Path.Value ?? string.Empty;
        if (path.StartsWith("/api/health", StringComparison.OrdinalIgnoreCase)
            || path.StartsWith("/api/auth/pair", StringComparison.OrdinalIgnoreCase)
            || path.StartsWith("/api/pairing/info", StringComparison.OrdinalIgnoreCase)
            || path.StartsWith("/pair", StringComparison.OrdinalIgnoreCase))
        {
            await _next(context);
            return;
        }

        if (!_tokenService.HasToken)
        {
            context.Response.StatusCode = StatusCodes.Status401Unauthorized;
            await context.Response.WriteAsJsonAsync(new Models.ErrorResponse
            {
                Error = "pairing_required",
                Message = "Pair this device before calling protected endpoints."
            });
            return;
        }

        if (!TryGetBearerToken(context.Request, out var token) || !_tokenService.IsValid(token))
        {
            context.Response.StatusCode = StatusCodes.Status401Unauthorized;
            await context.Response.WriteAsJsonAsync(new Models.ErrorResponse
            {
                Error = "unauthorized",
                Message = "Missing or invalid bearer token."
            });
            return;
        }

        await _next(context);
    }

    private static bool TryGetBearerToken(HttpRequest request, out string token)
    {
        token = string.Empty;
        if (!request.Headers.TryGetValue("Authorization", out var values))
        {
            return false;
        }

        var header = values.ToString();
        const string prefix = "Bearer ";
        if (!header.StartsWith(prefix, StringComparison.OrdinalIgnoreCase))
        {
            return false;
        }

        token = header[prefix.Length..].Trim();
        return !string.IsNullOrWhiteSpace(token);
    }
}
