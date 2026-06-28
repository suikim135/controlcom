using System.Diagnostics;

namespace ControlCom.Agent.Handlers;

public sealed class DisplayHandler : IDisplayHandler
{
    private string _mode = "dual";

    public string GetMode() => _mode;

    public string SetMode(string mode)
    {
        var normalized = mode.Trim().ToLowerInvariant();
        if (normalized is not ("single" or "dual"))
        {
            throw new ArgumentException("Mode must be 'single' or 'dual'.", nameof(mode));
        }

        var argument = normalized == "single" ? "/internal" : "/extend";
        var displaySwitchPath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.Windows),
            "System32",
            "DisplaySwitch.exe");

        if (!File.Exists(displaySwitchPath))
        {
            throw new FileNotFoundException("DisplaySwitch.exe was not found.", displaySwitchPath);
        }

        Process.Start(new ProcessStartInfo
        {
            FileName = displaySwitchPath,
            Arguments = argument,
            UseShellExecute = true
        });

        _mode = normalized;
        return _mode;
    }
}
