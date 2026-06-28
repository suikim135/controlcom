package com.controlcom.app.data

object LanNetworkValidator {
    fun isPrivateIpv4(ip: String): Boolean {
        val parts = ip.trim().split('.')
        if (parts.size != 4) {
            return false
        }

        val octets = parts.mapNotNull { it.toIntOrNull() }
        if (octets.size != 4 || octets.any { it !in 0..255 }) {
            return false
        }

        return when (octets[0]) {
            10 -> true
            127 -> true
            192 -> octets[1] == 168
            172 -> octets[1] in 16..31
            else -> false
        }
    }

    fun isValidPort(port: Int): Boolean = port in 1..65535

    fun isPairingCode(code: String): Boolean =
        code.length == 6 && code.all { it.isDigit() }
}
