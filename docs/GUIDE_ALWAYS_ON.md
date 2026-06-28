# ControlCom 상시 실행 가이드

PC를 켜 두는 동안 ControlCom을 **항상 쓸 수 있게** 설정하는 방법입니다.

---

## 동작 원리

| 상황 | 에이전트 | 폰 앱 |
|------|----------|--------|
| PC 켜짐 + 에이전트 실행 | ✅ | 연결됨 |
| PC Sleep | ❌ 응답 없음 | 끊김 → 깨우면 재연결 |
| PC 종료/재부팅 | ❌ | 끊김 |
| 재부팅 후 자동 실행 설정됨 | ✅ 자동 | 몇 초 후 재연결 |

> **PC가 완전히 꺼지면** 에이전트도 꺼집니다. 이건 피할 수 없습니다.

---

## 1회 설정: 로그인 시 자동 실행 (권장)

PowerShell:

```powershell
cd C:\Users\sui12\controlcom\pc-agent
powershell -ExecutionPolicy Bypass -File .\scripts\install-startup-task.ps1
```

또는 `pc-agent` 폴더에서 **`install-startup-task.bat`** 더블클릭.

### 이 스크립트가 하는 일

1. Release 빌드 → `pc-agent\publish\ControlCom.Agent.exe`
2. Windows **작업 스케줄러** 등록
3. **로그인할 때마다** 에이전트 자동 시작

### 확인

1. PC 재부팅
2. 로그인 후 작업 관리자 → `ControlCom.Agent.exe` 실행 중인지 확인
3. 브라우저: `http://localhost:7847/api/health` → `{"ok":true,...}`
4. 폰 앱 → 상단 **연결됨**

---

## 방화벽 (최초 1회)

관리자 PowerShell:

```powershell
cd C:\Users\sui12\controlcom\pc-agent
.\scripts\add-firewall-rule.ps1
```

또는:

```powershell
New-NetFirewallRule -DisplayName "ControlCom Agent TCP 7847" `
  -Direction Inbound -Action Allow -Protocol TCP -LocalPort 7847 `
  -Profile Private,Public
```

---

## `dotnet run` vs 자동 실행

| 방식 | 특징 |
|------|------|
| `dotnet run` | 개발용. PowerShell 창 닫으면 종료 |
| 작업 스케줄러 / Setup.exe | 일상 사용용. 로그인 시 백그라운드 실행 |

일상 사용은 **작업 스케줄러** 또는 **Setup.exe 설치**를 쓰세요.

---

## 재부팅 후 폰 앱

- **재페어링 불필요** (토큰 저장됨)
- PC IP가 바뀌면 설정에서 IP만 수정
- 앱이 5초마다 상태 확인 → **연결됨** / **끊김** 표시

### PC IP 확인

```powershell
ipconfig
```

Wi-Fi 또는 이더넷의 **IPv4 주소** (예: `192.168.55.113`)

---

## 페어링 초기화 (PC 바꿀 때)

```powershell
Remove-Item -Recurse -Force "$env:LOCALAPPDATA\ControlCom\Agent"
```

에이전트 재시작 → 새 QR로 폰에서 다시 페어링

---

## 문제 해결

| 증상 | 조치 |
|------|------|
| 재부팅 후 연결 안 됨 | 작업 스케줄러에 `ControlComAgent` 있는지 확인 |
| health는 되는데 앱만 안 됨 | IP/포트 확인, 앱 재실행 |
| Sleep 후 끊김 | 정상. PC 깨운 뒤 자동 재연결 대기 |

---

## 관련 문서

- [사용자 가이드](USER_GUIDE.md)
- [Play Store 출시 가이드](GUIDE_PLAY_STORE.md)
