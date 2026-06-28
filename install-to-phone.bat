@echo off
cd /d "%~dp0"
powershell -ExecutionPolicy Bypass -File "%~dp0install-to-phone.ps1"
pause
