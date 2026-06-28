namespace ControlCom.Agent.Handlers;

public interface IAudioHandler
{
    bool IsMuted();
    bool ToggleMute();
}
