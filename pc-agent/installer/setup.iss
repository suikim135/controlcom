#define MyAppName "ControlCom Agent"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "ControlCom"
#define MyAppExeName "ControlCom.Agent.exe"
#define MyAppURL "https://suikim135.github.io/controlcom/"

[Setup]
AppId={{A1B2C3D4-E5F6-7890-ABCD-EF1234567890}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={autopf}\ControlCom Agent
DefaultGroupName=ControlCom
DisableProgramGroupPage=yes
OutputDir=Output
OutputBaseFilename=ControlComAgent-Setup
Compression=lzma2
SolidCompression=yes
ArchitecturesInstallIn64BitMode=x64compatible
PrivilegesRequired=admin
InfoBeforeFile=infobefore.txt
InfoAfterFile=infoafter.txt
WizardStyle=modern
UninstallDisplayIcon={app}\{#MyAppExeName}

[Languages]
Name: "korean"; MessagesFile: "compiler:Languages\Korean.isl"

[Tasks]
Name: "autostart"; Description: "Windows 로그인 시 ControlCom 자동 실행"; GroupDescription: "추가 옵션 (권장):"; Flags: checkedonce
Name: "firewall"; Description: "같은 Wi-Fi(사설 네트워크)에서 폰 연결 허용 — 포트 7847"; GroupDescription: "추가 옵션 (권장):"; Flags: checkedonce

[Files]
Source: "..\publish\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{group}\페어링 QR 열기"; Filename: "http://localhost:7847/pair"
Name: "{autostart}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: autostart

[Run]
; Windows 방화벽 (PowerShell 대신 netsh — 설치 프로그램 표준 방식)
Filename: "netsh"; Parameters: "advfirewall firewall add rule name=""ControlCom Agent TCP 7847"" dir=in action=allow protocol=TCP localport=7847 profile=private"; Tasks: firewall; Flags: runhidden waituntilterminated; StatusMsg: "방화벽 규칙을 추가하는 중..."
Filename: "{app}\{#MyAppExeName}"; Description: "ControlCom Agent 실행"; Flags: nowait postinstall skipifsilent
Filename: "http://localhost:7847/pair"; Description: "페어링 QR 화면 열기"; Flags: postinstall shellexec skipifsilent

[UninstallRun]
Filename: "netsh"; Parameters: "advfirewall firewall delete rule name=""ControlCom Agent TCP 7847"""; Flags: runhidden waituntilterminated

[UninstallDelete]
Type: filesandordirs; Name: "{localappdata}\ControlCom"

[Code]
var
  OptionsInfoPage: TWizardPage;
  OptionsInfoLabel: TNewStaticText;

procedure InitializeWizard;
begin
  OptionsInfoPage := CreateCustomPage(
    wpSelectTasks,
    '연결 설정 안내',
    '폰과 PC가 같은 Wi-Fi에서 통신하려면 아래 옵션을 권장합니다.');

  OptionsInfoLabel := TNewStaticText.Create(OptionsInfoPage);
  OptionsInfoLabel.Parent := OptionsInfoPage.Surface;
  OptionsInfoLabel.Left := 0;
  OptionsInfoLabel.Top := 0;
  OptionsInfoLabel.Width := OptionsInfoPage.SurfaceWidth;
  OptionsInfoLabel.Height := ScaleY(130);
  OptionsInfoLabel.AutoSize := False;
  OptionsInfoLabel.WordWrap := True;
  OptionsInfoLabel.Caption :=
    '자동 실행: PC를 켜 두면 폰에서 바로 연결할 수 있습니다.' + #13#10 +
    '방화벽 허용: 같은 Wi-Fi(사설 네트워크)에서만 폰의 연결을 받습니다.' + #13#10 +
    '인터넷 원격 접속은 지원하지 않으며, 포트포워딩은 필요하지 않습니다.';
end;
