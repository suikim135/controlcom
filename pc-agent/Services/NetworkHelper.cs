using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;

namespace ControlCom.Agent.Services;

public static class NetworkHelper
{
    public static string? GetLocalIPv4()
    {
        foreach (var networkInterface in NetworkInterface.GetAllNetworkInterfaces())
        {
            if (networkInterface.OperationalStatus != OperationalStatus.Up)
            {
                continue;
            }

            if (networkInterface.NetworkInterfaceType is NetworkInterfaceType.Loopback or NetworkInterfaceType.Tunnel)
            {
                continue;
            }

            foreach (var address in networkInterface.GetIPProperties().UnicastAddresses)
            {
                if (address.Address.AddressFamily != AddressFamily.InterNetwork)
                {
                    continue;
                }

                var ip = address.Address.ToString();
                if (IPAddress.IsLoopback(address.Address) || ip.StartsWith("169.254.", StringComparison.Ordinal))
                {
                    continue;
                }

                return ip;
            }
        }

        return null;
    }
}
