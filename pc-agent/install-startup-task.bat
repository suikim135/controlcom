@echo off
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%scripts\install-startup-task.ps1"
if errorlevel 1 (
  echo.
  echo 실패했습니다. 아래 명령을 PowerShell에 직접 붙여넣어 보세요:
  echo powershell -ExecutionPolicy Bypass -File "%SCRIPT_DIR%scripts\install-startup-task.ps1"
)
echo.
pause
