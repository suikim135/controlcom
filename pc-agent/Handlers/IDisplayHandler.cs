namespace ControlCom.Agent.Handlers;

public interface IDisplayHandler
{
    string GetMode();
    string SetMode(string mode);
}
