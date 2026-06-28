using NAudio.CoreAudioApi;

namespace ControlCom.Agent.Handlers;

public sealed class AudioHandler : IAudioHandler, IDisposable
{
    private readonly MMDevice _device;

    public AudioHandler()
    {
        using var enumerator = new MMDeviceEnumerator();
        _device = enumerator.GetDefaultAudioEndpoint(DataFlow.Render, Role.Multimedia);
    }

    public bool IsMuted() => _device.AudioEndpointVolume.Mute;

    public bool ToggleMute()
    {
        var endpoint = _device.AudioEndpointVolume;
        endpoint.Mute = !endpoint.Mute;
        return endpoint.Mute;
    }

    public void Dispose() => _device.Dispose();
}
