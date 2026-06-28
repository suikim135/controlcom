namespace ControlCom.Agent.Handlers;

public interface IPowerHandler
{
    void Sleep();
    Task<Models.ShutdownResponse> ShutdownWithSaveAsync(int saveDelaySeconds, int shutdownGraceSeconds, CancellationToken cancellationToken);
}
