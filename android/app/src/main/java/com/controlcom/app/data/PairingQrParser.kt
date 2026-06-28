package com.controlcom.app.data

import android.net.Uri
import com.controlcom.app.AppConfig

data class PairingQrData(
    val pcIp: String,
    val pcPort: Int,
    val code: String
)

object PairingQrParser {
    fun parse(raw: String): PairingQrData? = when (val result = parseResult(raw)) {
        is PairingQrParseResult.Success -> result.data
        is PairingQrParseResult.Error -> null
    }

    fun parseResult(raw: String): PairingQrParseResult {
        val uri = runCatching { Uri.parse(raw.trim()) }.getOrNull()
            ?: return PairingQrParseResult.Error("QR 코드 형식이 올바르지 않습니다.")

        if (uri.scheme != "controlcom" || uri.host != "pair") {
            return PairingQrParseResult.Error("ControlCom 페어링 QR이 아닙니다.")
        }

        val ip = uri.getQueryParameter("ip")?.trim().orEmpty()
        val port = uri.getQueryParameter("port")?.toIntOrNull() ?: AppConfig.DEFAULT_PORT
        val code = uri.getQueryParameter("code")?.trim().orEmpty()

        if (ip.isBlank()) {
            return PairingQrParseResult.Error("QR에 PC IP가 없습니다.")
        }
        if (!LanNetworkValidator.isPrivateIpv4(ip)) {
            return PairingQrParseResult.Error("사설 LAN IP(예: 192.168.x.x)만 허용됩니다.")
        }
        if (!LanNetworkValidator.isValidPort(port)) {
            return PairingQrParseResult.Error("포트 번호가 올바르지 않습니다.")
        }
        if (!LanNetworkValidator.isPairingCode(code)) {
            return PairingQrParseResult.Error("페어링 코드는 6자리 숫자여야 합니다.")
        }

        return PairingQrParseResult.Success(PairingQrData(pcIp = ip, pcPort = port, code = code))
    }
}

sealed class PairingQrParseResult {
    data class Success(val data: PairingQrData) : PairingQrParseResult()
    data class Error(val message: String) : PairingQrParseResult()
}
