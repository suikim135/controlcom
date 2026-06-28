# ControlCom 보안 가이드

개인용 LAN 앱 기준 보안 설계와 검수 결과입니다.

---

## 1. 보안 모델

| 원칙 | 구현 |
|------|------|
| LAN only | `LanOnlyMiddleware` — 사설 IP만 허용 |
| 인증 | Bearer 토큰 (1회 페어링) |
| 외부 서버 없음 | 데이터 수집·전송 없음 |
| 최소 권한 | Android: INTERNET, CAMERA(QR) |

---

## 2. 구현된 보안 기능 (패치)

### LAN IP 필터 (`LanOnlyMiddleware`)

허용: `10.x`, `192.168.x`, `172.16~31.x`, `127.0.0.1`  
거부: 그 외 IP → `403 forbidden`

설정: `appsettings.json` → `Agent:AllowLanOnly` (기본 `true`)

> **주의:** 공유기 포트포워딩으로 외부에 포트를 열면 LAN 필터만으로는 부족합니다. **포트포워딩 하지 마세요.**

### 페어링 보호 (`TokenService`)

| 기능 | 설정 | 기본값 |
|------|------|--------|
| 실패 시도 제한 | `MaxPairingAttempts` | 5회 |
| 잠금 시간 | `PairingLockoutMinutes` | 15분 |
| 코드 만료 | `PairingCodeTtlMinutes` | 30분 |

5회 틀리면 `429 pairing_locked` — 15분 후 재시도.

### Bearer 인증

- 페어링 후 모든 API에 토큰 필요
- 공개: `/api/health`, `/api/pairing/info`, `/pair`, `/api/auth/pair`

### 토큰 저장

- PC: `%LOCALAPPDATA%\ControlCom\Agent\token.json`
- Android: DataStore (기기 내부)

---

## 3. 알려진 한계

| 항목 | 위험 | 완화 |
|------|------|------|
| HTTP (비암호화) | LAN 스니핑 이론상 가능 | 같은 가정 Wi-Fi 신뢰 전제; HTTPS는 2차 |
| 토큰 1개 | 기기 분실 시 LAN에서 재사용 가능 | 재페어링으로 토큰 교체 |
| Wi-Fi 침해 | 토큰 탈취 가능 | 강한 Wi-Fi 비밀번호, 게스트 Wi-Fi 분리 |
| 저장 후 종료 | 데이터 손실 | UI에 한계 명시 |

---

## 4. 사용자 권장 사항

1. **공유기 포트포워딩 7847 하지 않기**
2. 게스트 Wi-Fi에서 사용 자제
3. PC 분실·공유 시 페어링 초기화
4. 신뢰할 수 없는 네트워크에서 에이전트 실행 자제

---

## 5. 페어링 초기화

PC:

```powershell
Remove-Item -Recurse -Force "$env:LOCALAPPDATA\ControlCom\Agent"
```

Android: 앱 데이터 삭제 또는 설정에서 재페어링

---

## 6. 출시 전 보안 체크리스트

- [x] LAN IP 필터
- [x] 페어링 시도 제한
- [x] 페어링 코드 만료
- [x] Bearer 토큰
- [ ] Release keystore 비밀번호 변경 (기본값 사용 중)
- [ ] PC Setup.exe 코드 서명 (SmartScreen)
- [ ] privacy-policy.html 호스팅
- [ ] `AllowLanOnly: true` 유지 확인

---

## 7. 관련 설정 (`appsettings.json`)

```json
{
  "Agent": {
    "Port": 7847,
    "AllowLanOnly": true,
    "MaxPairingAttempts": 5,
    "PairingLockoutMinutes": 15,
    "PairingCodeTtlMinutes": 30
  }
}
```

---

## 8. 관련 문서

- [SSOT API](SSOT_API.md)
- [Play Store 가이드](GUIDE_PLAY_STORE.md)
