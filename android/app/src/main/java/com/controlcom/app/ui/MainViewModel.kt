package com.controlcom.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.controlcom.app.data.AppSettings
import com.controlcom.app.data.ControlComApi
import com.controlcom.app.data.DisplayModeRequest
import com.controlcom.app.data.PairRequest
import com.controlcom.app.data.PairingQrParser
import com.controlcom.app.data.PairingQrParseResult
import com.controlcom.app.data.LanNetworkValidator
import com.controlcom.app.data.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ConnectionStatus {
    Unknown,
    Connected,
    Disconnected
}

data class MainUiState(
    val settings: AppSettings = AppSettings("", 7847, ""),
    val connectionStatus: ConnectionStatus = ConnectionStatus.Unknown,
    val isLoading: Boolean = false,
    val muted: Boolean? = null,
    val displayMode: String? = null,
    val message: String? = null,
    val error: String? = null,
    val showShutdownDialog: Boolean = false,
    val pairingCode: String = "",
    val needsPairing: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)
    private var api: ControlComApi? = null
    private var healthMonitorJob: Job? = null

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val settingsFlow = repository.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings("", 7847, "", false)
    )

    init {
        viewModelScope.launch {
            repository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
                api = if (settings.isConfigured) repository.createApi(settings) else null
                if (settings.isConfigured) {
                    refreshStatus()
                    startHealthMonitor()
                } else {
                    stopHealthMonitor()
                    _uiState.update { it.copy(connectionStatus = ConnectionStatus.Unknown) }
                }
            }
        }
    }

    fun saveSettings(pcIp: String, pcPort: Int) {
        viewModelScope.launch {
            repository.saveSettings(pcIp, pcPort)
            _uiState.update { it.copy(message = "설정이 저장되었습니다.") }
        }
    }

    fun updatePairingCode(code: String) {
        _uiState.update { it.copy(pairingCode = code) }
    }

    fun pair(pcIp: String? = null, pcPort: Int? = null) {
        val ip = (pcIp ?: _uiState.value.settings.pcIp).trim()
        val port = pcPort ?: _uiState.value.settings.pcPort
        val code = _uiState.value.pairingCode.trim()

        if (ip.isBlank()) {
            _uiState.update { it.copy(error = "PC IP 주소를 입력하세요.") }
            return
        }
        if (!LanNetworkValidator.isPrivateIpv4(ip)) {
            _uiState.update { it.copy(error = "사설 LAN IP(예: 192.168.x.x)만 입력할 수 있습니다.") }
            return
        }
        if (!LanNetworkValidator.isValidPort(port)) {
            _uiState.update { it.copy(error = "포트 번호가 올바르지 않습니다.") }
            return
        }
        if (!LanNetworkValidator.isPairingCode(code)) {
            _uiState.update { it.copy(error = "페어링 코드는 6자리 숫자여야 합니다.") }
            return
        }

        viewModelScope.launch {
            repository.saveSettings(ip, port)
            runAction {
                val pairApi = repository.createApi(AppSettings(ip, port, "", false))
                val response = pairApi.pair(PairRequest(code))
                repository.saveToken(response.token)
                repository.setOnboardingCompleted(true)
                _uiState.update {
                    it.copy(
                        needsPairing = false,
                        pairingCode = "",
                        message = "페어링이 완료되었습니다."
                    )
                }
            }
        }
    }

    fun pairFromQr(raw: String) {
        when (val result = PairingQrParser.parseResult(raw)) {
            is PairingQrParseResult.Error -> {
                _uiState.update { it.copy(error = result.message) }
                return
            }
            is PairingQrParseResult.Success -> {
                val data = result.data
                viewModelScope.launch {
                    repository.saveSettings(data.pcIp, data.pcPort)
                    _uiState.update { it.copy(pairingCode = data.code) }
                    runAction {
                        val pairApi = repository.createApi(
                            AppSettings(data.pcIp, data.pcPort, "", false)
                        )
                        val response = pairApi.pair(PairRequest(data.code))
                        repository.saveToken(response.token)
                        repository.setOnboardingCompleted(true)
                        _uiState.update {
                            it.copy(
                                needsPairing = false,
                                pairingCode = "",
                                message = "QR 페어링이 완료되었습니다."
                            )
                        }
                    }
                }
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.setOnboardingCompleted(true)
            _uiState.update { it.copy(message = "시작하기를 완료했습니다.") }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            runAction(showLoading = true) {
                val health = requireApi().health()
                _uiState.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.Connected,
                        message = "연결 성공: ${health.service} ${health.version}"
                    )
                }
            }
        }
    }

    fun sleep() {
        viewModelScope.launch {
            runAction {
                requireApi().sleep()
                _uiState.update { it.copy(message = "절전 명령을 전송했습니다.") }
            }
        }
    }

    fun toggleMute() {
        viewModelScope.launch {
            runAction {
                val response = requireApi().toggleMute()
                _uiState.update {
                    it.copy(
                        muted = response.muted,
                        message = if (response.muted) "음소거 켜짐" else "음소거 꺼짐"
                    )
                }
            }
        }
    }

    fun toggleDisplayMode() {
        viewModelScope.launch {
            runAction {
                val current = _uiState.value.displayMode ?: requireApi().getDisplayMode().mode
                val next = if (current == "single") "dual" else "single"
                val response = requireApi().setDisplayMode(DisplayModeRequest(next))
                _uiState.update {
                    it.copy(
                        displayMode = response.mode,
                        message = if (response.mode == "single") "주 모니터만 사용" else "듀얼 모니터 복구"
                    )
                }
            }
        }
    }

    fun showShutdownDialog() {
        _uiState.update { it.copy(showShutdownDialog = true) }
    }

    fun dismissShutdownDialog() {
        _uiState.update { it.copy(showShutdownDialog = false) }
    }

    fun shutdown() {
        viewModelScope.launch {
            runAction {
                val response = requireApi().shutdown()
                _uiState.update {
                    it.copy(
                        showShutdownDialog = false,
                        message = "종료 예약됨 (${response.windowsNotified}개 창 저장 시도, ${response.delaySeconds}초 후 종료)"
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    private suspend fun refreshStatus() {
        try {
            val currentApi = api ?: return
            val health = currentApi.health()
            if (!health.ok) {
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.Disconnected) }
                return
            }

            val muted = try {
                currentApi.getMute().muted
            } catch (_: Exception) {
                _uiState.update { it.copy(needsPairing = true, connectionStatus = ConnectionStatus.Connected) }
                return
            }

            val displayMode = currentApi.getDisplayMode().mode
            _uiState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.Connected,
                    muted = muted,
                    displayMode = displayMode,
                    needsPairing = false
                )
            }
        } catch (_: Exception) {
            _uiState.update { it.copy(connectionStatus = ConnectionStatus.Disconnected) }
        }
    }

    private fun startHealthMonitor() {
        healthMonitorJob?.cancel()
        healthMonitorJob = viewModelScope.launch {
            while (isActive) {
                refreshStatus()
                delay(5_000)
            }
        }
    }

    private fun stopHealthMonitor() {
        healthMonitorJob?.cancel()
        healthMonitorJob = null
    }

    private suspend fun runAction(showLoading: Boolean = true, block: suspend () -> Unit) {
        if (showLoading) {
            _uiState.update { it.copy(isLoading = true, error = null) }
        }
        try {
            block()
        } catch (ex: Exception) {
            val message = when {
                ex.message?.contains("403", ignoreCase = true) == true ->
                    "LAN이 아닌 주소에서의 접근은 차단됩니다. 같은 Wi-Fi인지 확인하세요."
                ex.message?.contains("429", ignoreCase = true) == true ->
                    "페어링 시도가 너무 많습니다. 잠시 후 다시 시도하세요."
                ex.message?.contains("401", ignoreCase = true) == true ||
                    ex.message?.contains("pairing", ignoreCase = true) == true -> {
                    _uiState.update { it.copy(needsPairing = true) }
                    "페어링이 필요합니다. PC 페어링 코드를 확인하세요."
                }
                else -> ex.message ?: "요청에 실패했습니다."
            }
            _uiState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.Disconnected,
                    error = message
                )
            }
        } finally {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun requireApi(): ControlComApi {
        return api ?: throw IllegalStateException("PC IP와 포트를 먼저 설정하세요.")
    }

    override fun onCleared() {
        stopHealthMonitor()
        super.onCleared()
    }
}
