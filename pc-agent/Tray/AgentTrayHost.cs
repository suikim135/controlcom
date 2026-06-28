using ControlCom.Agent.Services;
using System.Diagnostics;
using System.Windows.Forms;

namespace ControlCom.Agent.Tray;

/// <summary>
/// System tray UI only. Does not own pairing or API state.
/// </summary>
public sealed class AgentTrayHost : IDisposable
{
    private readonly TokenService _tokenService;
    private readonly int _port;
    private readonly IHostApplicationLifetime _lifetime;
    private Thread? _thread;
    private volatile bool _disposed;

    public AgentTrayHost(TokenService tokenService, int port, IHostApplicationLifetime lifetime)
    {
        _tokenService = tokenService;
        _port = port;
        _lifetime = lifetime;
    }

    public void Start()
    {
        _thread = new Thread(RunMessageLoop)
        {
            IsBackground = true,
            Name = "ControlComTray"
        };
        _thread.SetApartmentState(ApartmentState.STA);
        _thread.Start();
    }

    public void Dispose()
    {
        if (_disposed)
        {
            return;
        }

        _disposed = true;
        if (_thread is { IsAlive: true })
        {
            try
            {
                Application.ExitThread();
            }
            catch
            {
                // Tray thread may already be exiting.
            }
        }
    }

    private void RunMessageLoop()
    {
        Application.EnableVisualStyles();
        Application.SetCompatibleTextRenderingDefault(false);
        Application.SetHighDpiMode(HighDpiMode.SystemAware);

        using var context = new ApplicationContext();
        using var notifyIcon = CreateNotifyIcon();
        notifyIcon.Visible = true;

        if (!_tokenService.HasToken)
        {
            notifyIcon.ShowBalloonTip(
                4000,
                "ControlCom Agent",
                "작업 표시줄 숨겨진 아이콘에서 페어링 QR을 열 수 있습니다.",
                ToolTipIcon.Info);
        }

        Application.Run(context);
    }

    private NotifyIcon CreateNotifyIcon()
    {
        var statusItem = new ToolStripMenuItem { Enabled = false };
        var openPairingItem = new ToolStripMenuItem("페어링 QR 열기");
        openPairingItem.Click += (_, _) => OpenPairingPage();

        var exitItem = new ToolStripMenuItem("종료");
        exitItem.Click += (_, _) =>
        {
            _lifetime.StopApplication();
            Application.ExitThread();
        };

        var menu = new ContextMenuStrip();
        menu.Items.Add(statusItem);
        menu.Items.Add(new ToolStripSeparator());
        menu.Items.Add(openPairingItem);
        menu.Items.Add(new ToolStripSeparator());
        menu.Items.Add(exitItem);
        menu.Opening += (_, _) => UpdateStatusText(statusItem);

        var notifyIcon = new NotifyIcon
        {
            Text = "ControlCom Agent",
            Icon = LoadTrayIcon(),
            ContextMenuStrip = menu
        };

        notifyIcon.DoubleClick += (_, _) => OpenPairingPage();
        UpdateStatusText(statusItem);
        return notifyIcon;
    }

    private void UpdateStatusText(ToolStripMenuItem statusItem)
    {
        statusItem.Text = _tokenService.HasToken
            ? "상태: 페어링됨 — 폰에서 연결 가능"
            : "상태: 페어링 대기 — QR을 열어 연결하세요";
    }

    private void OpenPairingPage()
    {
        try
        {
            Process.Start(new ProcessStartInfo
            {
                FileName = $"http://localhost:{_port}/pair",
                UseShellExecute = true
            });
        }
        catch
        {
            // Browser launch is best-effort.
        }
    }

    private static Icon LoadTrayIcon()
    {
        try
        {
            var exePath = Environment.ProcessPath;
            if (!string.IsNullOrWhiteSpace(exePath))
            {
                var extracted = Icon.ExtractAssociatedIcon(exePath);
                if (extracted is not null)
                {
                    return extracted;
                }
            }
        }
        catch
        {
            // Fall back to default icon.
        }

        return SystemIcons.Application;
    }
}
