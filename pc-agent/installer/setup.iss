#define MyAppName "ControlCom Agent"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "ControlCom"
#define MyAppExeName "ControlCom.Agent.exe"

[Setup]
AppId={{A1B2C3D4-E5F6-7890-ABCD-EF1234567890}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={autopf}\ControlCom Agent
DefaultGroupName=ControlCom
DisableProgramGroupPage=yes
OutputDir=..\installer
OutputBaseFilename=ControlComAgent-Setup
Compression=lzma2
SolidCompression=yes
ArchitecturesInstallIn64BitMode=x64compatible
PrivilegesRequired=admin

[Languages]
Name: "korean"; MessagesFile: "compiler:Languages\Korean.isl"

[Tasks]
Name: "autostart"; Description: "Windows 로그인 시 자동 실행"; GroupDescription: "추가 옵션:"; Flags: checkedonce
Name: "firewall"; Description: "Private 네트워크 방화벽 허용 (포트 7847)"; GroupDescription: "추가 옵션:"; Flags: checkedonce

[Files]
Source: "..\publish\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{group}\페어링 페이지 열기"; Filename: "http://localhost:7847/pair"
Name: "{autostart}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: autostart

[Run]
Filename: "powershell.exe"; Parameters: "-ExecutionPolicy Bypass -File ""{app}\scripts\add-firewall-rule.ps1"""; Tasks: firewall; Flags: runhidden
Filename: "{app}\{#MyAppExeName}"; Description: "ControlCom Agent 실행"; Flags: nowait postinstall skipifsilent
Filename: "http://localhost:7847/pair"; Description: "페어링 페이지 열기"; Flags: postinstall shellexec skipifsilent

[UninstallDelete]
Type: filesandordirs; Name: "{localappdata}\ControlCom"
