using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Text;
using ControlCom.Agent.Models;

namespace ControlCom.Agent.Handlers;

public sealed class PowerHandler : IPowerHandler
{
    private const int WmSave = 0x0006;

    private static readonly HashSet<string> SkippedClassNames = new(StringComparer.OrdinalIgnoreCase)
    {
        "Shell_TrayWnd",
        "Shell_SecondaryTrayWnd",
        "Progman",
        "WorkerW",
        "Windows.UI.Core.CoreWindow"
    };

    public void Sleep()
    {
        SetSuspendState(hibernate: false, forceCritical: false, disableWakeEvent: false);
    }

    public async Task<ShutdownResponse> ShutdownWithSaveAsync(
        int saveDelaySeconds,
        int shutdownGraceSeconds,
        CancellationToken cancellationToken)
    {
        var windowsNotified = NotifyVisibleWindowsToSave();
        await Task.Delay(TimeSpan.FromSeconds(saveDelaySeconds), cancellationToken);

        var psi = new ProcessStartInfo
        {
            FileName = "shutdown",
            Arguments = $"/s /t {shutdownGraceSeconds}",
            CreateNoWindow = true,
            UseShellExecute = false
        };
        Process.Start(psi);

        return new ShutdownResponse
        {
            Ok = true,
            Message = "shutdown_scheduled",
            WindowsNotified = windowsNotified,
            DelaySeconds = shutdownGraceSeconds
        };
    }

    private static int NotifyVisibleWindowsToSave()
    {
        var count = 0;
        EnumWindows((hwnd, _) =>
        {
            if (!IsWindowVisible(hwnd))
            {
                return true;
            }

            if (GetWindow(hwnd, GetWindowCmd.Owner) != IntPtr.Zero)
            {
                return true;
            }

            var className = GetClassName(hwnd);
            if (SkippedClassNames.Contains(className))
            {
                return true;
            }

            var title = GetWindowTitle(hwnd);
            if (string.IsNullOrWhiteSpace(title))
            {
                return true;
            }

            PostMessage(hwnd, WmSave, IntPtr.Zero, IntPtr.Zero);
            SendCtrlS(hwnd);
            count++;
            return true;
        }, IntPtr.Zero);

        return count;
    }

    private static void SendCtrlS(IntPtr hwnd)
    {
        if (!SetForegroundWindow(hwnd))
        {
            return;
        }

        Thread.Sleep(50);

        var inputs = new INPUT[4];
        inputs[0] = CreateKeyInput(0x11, keyUp: false); // Ctrl down
        inputs[1] = CreateKeyInput(0x53, keyUp: false); // S down
        inputs[2] = CreateKeyInput(0x53, keyUp: true);  // S up
        inputs[3] = CreateKeyInput(0x11, keyUp: true);  // Ctrl up
        SendInput((uint)inputs.Length, inputs, Marshal.SizeOf<INPUT>());
    }

    private static INPUT CreateKeyInput(ushort virtualKey, bool keyUp)
    {
        return new INPUT
        {
            type = 1,
            U = new InputUnion
            {
                ki = new KEYBDINPUT
                {
                    wVk = virtualKey,
                    dwFlags = keyUp ? 0x0002u : 0u
                }
            }
        };
    }

    private static string GetClassName(IntPtr hwnd)
    {
        var builder = new StringBuilder(256);
        GetClassName(hwnd, builder, builder.Capacity);
        return builder.ToString();
    }

    private static string GetWindowTitle(IntPtr hwnd)
    {
        var length = GetWindowTextLength(hwnd);
        if (length == 0)
        {
            return string.Empty;
        }

        var builder = new StringBuilder(length + 1);
        GetWindowText(hwnd, builder, builder.Capacity);
        return builder.ToString();
    }

    private delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);

    private enum GetWindowCmd : uint
    {
        Owner = 4
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct INPUT
    {
        public uint type;
        public InputUnion U;
    }

    [StructLayout(LayoutKind.Explicit)]
    private struct InputUnion
    {
        [FieldOffset(0)] public KEYBDINPUT ki;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct KEYBDINPUT
    {
        public ushort wVk;
        public ushort wScan;
        public uint dwFlags;
        public uint time;
        public IntPtr dwExtraInfo;
    }

    [DllImport("user32.dll")]
    private static extern bool EnumWindows(EnumWindowsProc lpEnumFunc, IntPtr lParam);

    [DllImport("user32.dll")]
    private static extern bool IsWindowVisible(IntPtr hWnd);

    [DllImport("user32.dll")]
    private static extern IntPtr GetWindow(IntPtr hWnd, GetWindowCmd uCmd);

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    private static extern int GetClassName(IntPtr hWnd, StringBuilder lpClassName, int nMaxCount);

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    private static extern int GetWindowText(IntPtr hWnd, StringBuilder lpString, int nMaxCount);

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    private static extern int GetWindowTextLength(IntPtr hWnd);

    [DllImport("user32.dll")]
    private static extern bool PostMessage(IntPtr hWnd, uint msg, IntPtr wParam, IntPtr lParam);

    [DllImport("user32.dll")]
    private static extern bool SetForegroundWindow(IntPtr hWnd);

    [DllImport("user32.dll")]
    private static extern uint SendInput(uint nInputs, INPUT[] pInputs, int cbSize);

    [DllImport("Powrprof.dll", CharSet = CharSet.Auto, ExactSpelling = true)]
    private static extern bool SetSuspendState(bool hibernate, bool forceCritical, bool disableWakeEvent);
}
