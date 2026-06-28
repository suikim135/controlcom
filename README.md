# ControlCom

누워서 PC를 조작하는 개인 전용 앱입니다.

- Android 클라이언트 (`android/`)
- Windows PC 에이전트 (`pc-agent/`)

## 문서

| 가이드 | 설명 |
|--------|------|
| [사용자 가이드](docs/USER_GUIDE.md) | 설치, 페어링, 사용법, 문제 해결 |
| [상시 실행 가이드](docs/GUIDE_ALWAYS_ON.md) | PC 자동 시작·재부팅 후 연결 |
| [Play Store 가이드](docs/GUIDE_PLAY_STORE.md) | 스토어 출시·사용자 접근성 |
| [보안 가이드](docs/SECURITY.md) | LAN 필터, 페어링 보호 |
| [문서 목록](docs/README.md) | 전체 문서 인덱스 |

## 기능

- 절전 모드 (Sleep)
- 음소거 토글
- 주 모니터만 사용 / 듀얼 복구
- 최선 저장 후 종료

## 빠른 시작

1. PC: `cd pc-agent` → `dotnet run` (또는 Setup.exe 설치)
2. 폰: `dist/ControlCom-1.0.0-release.apk` 설치 ([Snapdrop](https://snapdrop.net) 권장)
3. 앱 **시작하기** → PC 페어링 QR 스캔

자세한 내용 → [사용자 가이드](docs/USER_GUIDE.md)

## 사전 준비 (요약)

### PC

1. [.NET 8 SDK](https://dotnet.microsoft.com/download) 설치
2. 에이전트 실행:

```powershell
cd pc-agent
dotnet run
```

3. (관리자) 방화벽 허용:

```powershell
.\scripts\add-firewall-rule.ps1
```

4. (선택) 로그인 시 자동 실행:

```powershell
.\scripts\install-startup-task.ps1
```

콘솔에 표시되는 **6자리 페어링 코드**를 Android 앱에 입력합니다.

### Android

1. Android Studio에서 `android/` 폴더 열기
2. S24+ 실기기 또는 에뮬레이터에서 빌드/실행

**또는 APK 직접 설치**

빌드된 APK: [`dist/ControlCom-1.0.0-debug.apk`](dist/ControlCom-1.0.0-debug.apk)

폰으로 옮긴 뒤 설치 (출처 불명 앱 허용 필요):

```powershell
# USB 디버깅 연결 시
adb install dist/ControlCom-1.0.0-debug.apk
```

3. 설정에서 PC IP (예: `192.168.0.15`), 포트 `7847` 입력
4. 연결 테스트 → 페어링
5. 메인 화면에서 버튼 사용

## API 계약

[docs/SSOT_API.md](docs/SSOT_API.md)

## 배포·스토어 출시 기획

[docs/PRODUCT_DISTRIBUTION_PLAN.md](docs/PRODUCT_DISTRIBUTION_PLAN.md)

## Phase B 빌드

### Android (Play Store 제출용)

```powershell
.\scripts\build-android-release.ps1
```

산출물: `dist/ControlCom-1.0.0.aab`, `dist/ControlCom-1.0.0-release.apk`

최초 1회 keystore 생성: `.\scripts\create-release-keystore.ps1`

### PC 에이전트 설치 파일

```powershell
cd pc-agent
.\scripts\publish.ps1
.\scripts\build-installer.ps1   # Inno Setup 6 필요
```

페어링 페이지: `http://localhost:7847/pair`

### 개인정보 처리방침

[docs/privacy-policy.html](docs/privacy-policy.html) — Play Console URL로 호스팅 필요

## 주의

- 종료+저장은 모든 앱을 보장하지 않습니다.
- Sleep/종료는 실제 PC 상태를 변경합니다. 테스트 시 주의하세요.
