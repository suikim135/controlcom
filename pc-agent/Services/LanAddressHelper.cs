namespace ControlCom.Agent.Services;

public static class LanAddressHelper
{
    public static bool IsPrivateLan(System.Net.IPAddress? address)
    {
        if (address is null)
        {
            return false;
        }

        if (System.Net.IPAddress.IsLoopback(address))
        {
            return true;
        }

        if (address.IsIPv4MappedToIPv6)
        {
            address = address.MapToIPv4();
        }

        if (address.AddressFamily != System.Net.Sockets.AddressFamily.InterNetwork)
        {
            return false;
        }

        var bytes = address.GetAddressBytes();
        return bytes[0] switch
        {
            10 => true,
            127 => true,
            192 when bytes[1] == 168 => true,
            172 when bytes[1] is >= 16 and <= 31 => true,
            _ => false
        };
    }
}
