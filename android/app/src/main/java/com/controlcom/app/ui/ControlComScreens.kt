package com.controlcom.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7C9CFF),
    onPrimary = Color(0xFF0B1B3A),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF1B1B1F),
    onSurface = Color(0xFFE4E1E9),
    error = Color(0xFFFFB4AB)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlComApp(viewModel: MainViewModel) {
    MaterialTheme(colorScheme = DarkColors) {
        val navController = rememberNavController()
        val uiState by viewModel.uiState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
            result.contents?.let { viewModel.pairFromQr(it) }
        }
        val launchQrScan = {
            scanLauncher.launch(
                ScanOptions().apply {
                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    setPrompt("PC 화면의 QR 코드를 스캔하세요")
                    setBeepEnabled(false)
                    setOrientationLocked(false)
                }
            )
        }

        LaunchedEffect(uiState.message, uiState.error) {
            val text = uiState.error ?: uiState.message
            if (!text.isNullOrBlank()) {
                snackbarHostState.showSnackbar(text)
                viewModel.clearMessage()
            }
        }

        LaunchedEffect(uiState.settings.isReady, uiState.settings.onboardingCompleted) {
            if (uiState.settings.isReady && uiState.settings.onboardingCompleted) {
                navController.navigate("main") {
                    popUpTo("onboarding") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }

        val startDestination = if (uiState.settings.onboardingCompleted && uiState.settings.isReady) {
            "main"
        } else {
            "onboarding"
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("ControlCom") },
                    actions = {
                        ConnectionBadge(uiState.connectionStatus)
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("onboarding") {
                        OnboardingScreen(
                            uiState = uiState,
                            onPair = viewModel::pair,
                            onPairFromQr = viewModel::pairFromQr,
                            onPairingCodeChange = viewModel::updatePairingCode,
                            onOpenSettings = { navController.navigate("settings") },
                            onSkipIfReady = {
                                viewModel.completeOnboarding()
                                navController.navigate("main") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("main") {
                        MainScreen(
                            uiState = uiState,
                            onSleep = viewModel::sleep,
                            onToggleMute = viewModel::toggleMute,
                            onToggleDisplay = viewModel::toggleDisplayMode,
                            onShutdown = viewModel::showShutdownDialog,
                            onOpenSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            uiState = uiState,
                            onSave = viewModel::saveSettings,
                            onTestConnection = viewModel::testConnection,
                            onPairingCodeChange = viewModel::updatePairingCode,
                            onPair = viewModel::pair,
                            onScanQr = launchQrScan,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        if (uiState.showShutdownDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissShutdownDialog,
                title = { Text("저장 후 종료") },
                text = {
                    Text("열린 프로그램에 저장을 시도한 뒤 PC를 종료합니다. 모든 앱이 저장되지는 않을 수 있습니다.")
                },
                confirmButton = {
                    TextButton(onClick = viewModel::shutdown) {
                        Text("종료")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissShutdownDialog) {
                        Text("취소")
                    }
                }
            )
        }
    }
}

@Composable
private fun ConnectionBadge(status: ConnectionStatus) {
    val (label, color) = when (status) {
        ConnectionStatus.Connected -> "연결됨" to Color(0xFF6DD58C)
        ConnectionStatus.Disconnected -> "끊김" to Color(0xFFFFB4AB)
        ConnectionStatus.Unknown -> "미연결" to Color(0xFFCAC4D0)
    }
    Row(
        modifier = Modifier.padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "●",
            color = color,
            fontSize = 12.sp,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(text = label, fontSize = 14.sp)
    }
}

@Composable
private fun MainScreen(
    uiState: MainUiState,
    onSleep: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleDisplay: () -> Unit,
    onShutdown: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!uiState.settings.isConfigured) {
            Text(
                text = "설정에서 PC IP를 먼저 입력하세요.",
                color = MaterialTheme.colorScheme.error
            )
        }

        if (uiState.needsPairing) {
            Text(
                text = "페어링이 필요합니다. 설정 또는 시작하기 화면에서 연결하세요.",
                color = MaterialTheme.colorScheme.error
            )
        }

        ActionButton(text = "절전 모드", onClick = onSleep)
        ActionButton(
            text = if (uiState.muted == true) "음소거 해제" else "음소거",
            onClick = onToggleMute
        )
        ActionButton(
            text = if (uiState.displayMode == "single") "듀얼 모니터 복구" else "모니터 1개만",
            onClick = onToggleDisplay
        )
        ActionButton(
            text = "저장 후 종료",
            onClick = onShutdown,
            containerColor = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("설정")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    uiState: MainUiState,
    onSave: (String, Int) -> Unit,
    onTestConnection: () -> Unit,
    onPairingCodeChange: (String) -> Unit,
    onPair: () -> Unit,
    onScanQr: () -> Unit,
    onBack: () -> Unit
) {
    var pcIp by remember(uiState.settings.pcIp) { mutableStateOf(uiState.settings.pcIp) }
    var pcPort by remember(uiState.settings.pcPort) { mutableStateOf(uiState.settings.pcPort.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) { Text("← 뒤로") }

        OutlinedTextField(
            value = pcIp,
            onValueChange = { pcIp = it },
            label = { Text("PC IP 주소") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = pcPort,
            onValueChange = { pcPort = it.filter { ch -> ch.isDigit() } },
            label = { Text("포트") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                val port = pcPort.toIntOrNull() ?: 7847
                onSave(pcIp, port)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("저장")
        }

        Button(
            onClick = onTestConnection,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("연결 테스트")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("페어링", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("PC Agent 페어링 화면의 QR을 스캔하거나 6자리 코드를 입력하세요.")

        Button(
            onClick = onScanQr,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("QR 코드 스캔")
        }

        OutlinedTextField(
            value = uiState.pairingCode,
            onValueChange = onPairingCodeChange,
            label = { Text("페어링 코드") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = onPair,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("페어링")
        }

        if (uiState.settings.token.isNotBlank()) {
            Text("토큰이 저장되어 있습니다.")
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.primary
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor)
    ) {
        Text(text = text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}
