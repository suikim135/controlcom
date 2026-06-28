package com.controlcom.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlcom.app.AppConfig
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
fun OnboardingScreen(
    uiState: MainUiState,
    onPair: (String, Int) -> Unit,
    onPairFromQr: (String) -> Unit,
    onPairingCodeChange: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onSkipIfReady: () -> Unit
) {
    val context = LocalContext.current
    var pcIp by remember(uiState.settings.pcIp) { mutableStateOf(uiState.settings.pcIp) }
    var pcPort by remember(uiState.settings.pcPort) { mutableStateOf(uiState.settings.pcPort.toString()) }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let(onPairFromQr)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("ControlCom 시작하기", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(
            "폰으로 PC를 조종하려면 Windows에 ControlCom 동반 프로그램이 필요합니다. " +
                "Spotify·Discord PC 앱처럼 한 번만 설치하면 됩니다.",
            lineHeight = 24.sp
        )

        Text(
            "안전하게 쓰는 방법",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
        Text(
            "• 같은 Wi-Fi에서만 연결됩니다\n" +
                "• 외부 서버로 데이터를 보내지 않습니다\n" +
                "• 페어링한 폰만 PC를 제어할 수 있습니다",
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        )

        Text("1단계: PC에 설치", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text(
            "PC에서 아래 버튼을 눌러 설치한 뒤, 작업 표시줄 숨겨진 아이콘(^)에서 " +
                "ControlCom → 페어링 QR 열기를 선택하세요.",
            lineHeight = 22.sp
        )
        Button(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.PC_DOWNLOAD_URL)))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("PC에 ControlCom 설치하기")
        }

        Text("2단계: 폰과 연결", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Button(
            onClick = {
                scanLauncher.launch(
                    ScanOptions().apply {
                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        setPrompt("PC 화면의 QR 코드를 스캔하세요")
                        setBeepEnabled(false)
                        setOrientationLocked(false)
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("QR 코드로 페어링")
        }

        Text("또는 PC IP를 직접 입력", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

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

        OutlinedTextField(
            value = uiState.pairingCode,
            onValueChange = onPairingCodeChange,
            label = { Text("페어링 코드 (6자리)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                val port = pcPort.toIntOrNull() ?: AppConfig.DEFAULT_PORT
                onPair(pcIp.trim(), port)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("연결하기")
        }

        if (uiState.settings.isReady) {
            OutlinedButton(
                onClick = onSkipIfReady,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("메인 화면으로")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onOpenSettings) {
            Text("고급 설정")
        }

        TextButton(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.PRIVACY_POLICY_URL)))
            }
        ) {
            Text("개인정보 처리방침")
        }
    }
}
