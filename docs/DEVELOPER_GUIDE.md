# ControlCom 개발자 가이드

소스 빌드, 배포, Play Store 준비를 위한 기술 문서입니다.

---

## 1. 프로젝트 구조

```
controlcom/
├── android/           # Kotlin + Jetpack Compose 앱
├── pc-agent/          # C# .NET 8 Windows 에이전트
├── docs/              # 문서 (API, 가이드, 정책)
├── dist/              # 빌드 산출물 (APK, AAB, 설치 파일)
└── scripts/           # 빌드·배포 스크립트
```

### SSOT 책임 분리

| 레이어 | 책임 |
|--------|------|
| `android/` | UI, HTTP 요청, 설정 저장 |
| `pc-agent/` | Sleep, 음소거, 모니터, 종료 실행 |
| `docs/SSOT_API.md` | API 요청/응답 스키마 |

---

## 2. 사전 요구사항

| 도구 | 용도 |
|------|------|
| [.NET 8 SDK](https://dotnet.microsoft.com/download) | PC 에이전트 빌드 |
| JDK 17 | Android 빌드 |
| Android SDK (API 34) | Android 빌드 |
| Inno Setup 6 (선택) | PC 설치 파일 생성 |

---

## 3. PC 에이전트

### 개발 실행

```powershell
cd pc-agent
dotnet run
```

- 포트: `7847` (기본)
- 페어링 페이지: `http://localhost:7847/pair`
- 설정: `appsettings.json`

### Release 빌드 (self-contained)

```powershell
cd pc-agent
.\scripts\publish.ps1
```

산출물: `pc-agent/publish/ControlCom.Agent.exe`

### 설치 파일 생성

[Inno Setup 6](https://jrsoftware.org/isinfo.php) 설치 후:

```powershell
cd pc-agent
.\scripts\build-installer.ps1
```

산출물: `pc-agent/installer/Output/ControlComAgent-Setup.exe`

### Handler 구조

```
pc-agent/Handlers/
├── IPowerHandler.cs    → Sleep, 저장 후 종료
├── IAudioHandler.cs    → 음소거 (NAudio)
└── IDisplayHandler.cs  → 모니터 전환 (DisplaySwitch.exe)
```

### 페어링 데이터 위치

```
%LOCALAPPDATA%\ControlCom\Agent\
├── token.json
└── pairing-code.txt
```

---

## 4. Android 앱

### 개발 빌드

```powershell
cd android
.\gradlew.bat assembleDebug
```

산출물: `android/app/build/outputs/apk/debug/app-debug.apk`

### Release 빌드 (Play Store용)

```powershell
.\scripts\build-android-release.ps1
```

산출물:
- `dist/ControlCom-1.0.0.aab` — Play Store 제출
- `dist/ControlCom-1.0.0-release.apk` — 직접 설치용

### Release Keystore (최초 1회)

```powershell
.\scripts\create-release-keystore.ps1
```

생성 파일:
- `android/keystore/controlcom-release.jks`
- `android/keystore.properties`

> **주의:** keystore와 비밀번호는 분실 시 Play Store 업데이트 불가. 백업 필수.  
> 출시 전 기본 비밀번호(`controlcom123`)를 반드시 변경하세요.

### 주요 패키지

```
com.controlcom.app/
├── ui/           MainViewModel, OnboardingScreen, ControlComScreens
├── data/         ApiModels, SettingsRepository, PairingQrParser
└── AppConfig.kt  PC_DOWNLOAD_URL, PRIVACY_POLICY_URL
```

### QR 페어링 형식

```
controlcom://pair?ip={ip}&port={port}&code={code}
```

PC: `PairingQrService.cs`  
Android: `PairingQrParser.kt`

---

## 5. API

전체 계약: [SSOT_API.md](SSOT_API.md)

### 공개 엔드포인트 (인증 불필요)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/health` | 연결 확인 |
| GET | `/api/pairing/info` | 페어링 정보 JSON |
| GET | `/pair` | QR HTML 페이지 |
| POST | `/api/auth/pair` | 페어링 (미페어링 상태) |

### 인증

```
Authorization: Bearer {token}
```

---

## 6. 배포 체크리스트

### Play Store (Phase C)

- [ ] Google Play 개발자 계정 ($25)
- [ ] `dist/ControlCom-1.0.0.aab` 업로드
- [ ] `docs/privacy-policy.html` 웹 호스팅 → URL 등록
- [ ] 스크린샷, 앱 설명, 아이콘 512px
- [ ] `AppConfig.PC_DOWNLOAD_URL` 실제 Releases URL로 변경
- [ ] 데이터 안전성: 수집 없음

### PC 에이전트

- [ ] `ControlComAgent-Setup.exe` 배포
- [ ] GitHub Releases 또는 웹 다운로드 페이지
- [ ] (선택) 코드 서명 인증서 — SmartScreen 경고 완화
- [ ] (선택) Microsoft Store / winget

---

## 7. 유용한 스크립트

| 스크립트 | 설명 |
|----------|------|
| `scripts/build-android-release.ps1` | AAB + release APK |
| `scripts/create-release-keystore.ps1` | Android keystore 생성 |
| `scripts/install-to-phone.ps1` | Wi-Fi APK 전송 서버 |
| `pc-agent/scripts/publish.ps1` | PC 에이전트 publish |
| `pc-agent/scripts/build-installer.ps1` | Setup.exe 빌드 |
| `pc-agent/scripts/add-firewall-rule.ps1` | 방화벽 규칙 추가 |

---

## 8. 로드맵

| 단계 | 상태 | 내용 |
|------|------|------|
| Phase A | 완료 | MVP 기능 |
| Phase B | 완료 | keystore, AAB, QR 페어링, Setup, 온보딩 |
| Phase C | 예정 | Play Store 출시 |
| Phase D | 예정 | mDNS 자동 검색, MS Store |

상세: [PRODUCT_DISTRIBUTION_PLAN.md](PRODUCT_DISTRIBUTION_PLAN.md)

---

## 9. 보안

[SECURITY.md](SECURITY.md)

---

## 10. 관련 문서

- [사용자 가이드](USER_GUIDE.md)
- [API 계약](SSOT_API.md)
- [배포 기획](PRODUCT_DISTRIBUTION_PLAN.md)
