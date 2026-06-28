# ControlCom GitHub Pages 설정 가이드

공식 웹(다운로드·개인정보 처리방침)과 Play Store 제출용 URL을 맞추는 절차입니다.

**저장소:** [suikim135/controlcom](https://github.com/suikim135/controlcom)  
**공개 URL:** `https://suikim135.github.io/controlcom/`

---

## 1. GitHub 접근 (Cursor 에이전트)

에이전트는 **로컬 폴더만** 수정할 수 있습니다. push는 **본인 PC에서 git 인증**이 되어 있어야 합니다.

```powershell
winget install GitHub.cli
gh auth login
```

---

## 2. 로컬 → GitHub push

원격에 LICENSE만 있는 경우, 아래처럼 **한 번 pull 후 push**합니다.

```powershell
cd C:\Users\sui12\controlcom
git init
git remote add origin https://github.com/suikim135/controlcom.git
git fetch origin
git checkout -b main
git pull origin main --allow-unrelated-histories
git add .
git commit -m "Add ControlCom Android app, PC agent, and docs"
git push -u origin main
```

이미 `git init` 했다면 `remote add`는 생략하고 `pull` → `add` → `commit` → `push`만 하면 됩니다.

---

## 3. GitHub Pages 켜기

1. [suikim135/controlcom → Settings → Pages](https://github.com/suikim135/controlcom/settings/pages)
2. **Source:** Deploy from a branch
3. **Branch:** `main` / **`/docs`**
4. **Save**
5. 1~3분 후 확인:
   - https://suikim135.github.io/controlcom/
   - https://suikim135.github.io/controlcom/download.html
   - https://suikim135.github.io/controlcom/privacy-policy.html

### `docs/` 구성

| 파일 | 용도 |
|------|------|
| `index.html` | 공식 홈 |
| `download.html` | PC Setup.exe |
| `privacy-policy.html` | 개인정보 처리방침 |
| `.nojekyll` | Jekyll 비활성화 |

---

## 4. Releases (PC 설치 파일)

```powershell
cd C:\Users\sui12\controlcom\pc-agent
.\scripts\build-installer.ps1
```

GitHub → **Releases** → **Draft a new release**

| 항목 | 값 |
|------|-----|
| Tag | `v1.0.0` |
| 파일 | `pc-agent/installer/Output/ControlComAgent-Setup.exe` |

---

## 5. Play Console / 앱 URL

| 용도 | URL |
|------|-----|
| 개발자 웹사이트 | `https://suikim135.github.io/controlcom/` |
| PC 다운로드 | `https://suikim135.github.io/controlcom/download.html` |
| 개인정보 처리방침 | `https://suikim135.github.io/controlcom/privacy-policy.html` |
| Releases | `https://github.com/suikim135/controlcom/releases/latest` |

`AppConfig.kt`의 `GITHUB_OWNER` / `GITHUB_REPO`와 동일합니다.

---

## 6. 나중에 Organization으로 옮길 때

1. Organization 생성 후 repo 이전(Transfer) 또는 mirror
2. `AppConfig.kt`의 `GITHUB_OWNER` 변경
3. `docs/*.html`의 GitHub 링크 수정
4. Pages URL이 바뀌므로 Play Console URL도 갱신

---

## 체크리스트

- [x] Repo: https://github.com/suikim135/controlcom
- [ ] 로컬 코드 push
- [ ] Pages: `main` / `docs`
- [ ] Release + Setup.exe
- [ ] Android 앱 재빌드

---

## 관련 문서

- [Play Store 가이드](GUIDE_PLAY_STORE.md)
- [보안 가이드](SECURITY.md)
