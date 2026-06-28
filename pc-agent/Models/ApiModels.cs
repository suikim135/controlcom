namespace ControlCom.Agent.Models;

public sealed class HealthResponse
{
    public bool Ok { get; init; } = true;
    public string Service { get; init; } = "controlcom-agent";
    public string Version { get; init; } = "1.0.0";
}

public sealed class OkResponse
{
    public bool Ok { get; init; } = true;
}

public sealed class PairRequest
{
    public string Code { get; init; } = string.Empty;
}

public sealed class PairResponse
{
    public string Token { get; init; } = string.Empty;
}

public sealed class PairingInfoResponse
{
    public bool Paired { get; init; }
    public string? Ip { get; init; }
    public int Port { get; init; }
    public string? Code { get; init; }
    public string? QrPayload { get; init; }
}

public sealed class ErrorResponse
{
    public string Error { get; init; } = string.Empty;
    public string? Message { get; init; }
}

public sealed class MuteResponse
{
    public bool Muted { get; init; }
}

public sealed class DisplayModeRequest
{
    public string Mode { get; init; } = string.Empty;
}

public sealed class DisplayModeResponse
{
    public string Mode { get; init; } = string.Empty;
}

public sealed class ShutdownResponse
{
    public bool Ok { get; init; } = true;
    public string Message { get; init; } = "shutdown_scheduled";
    public int WindowsNotified { get; init; }
    public int DelaySeconds { get; init; }
}
