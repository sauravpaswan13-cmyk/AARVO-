@echo off
setlocal
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)
echo Gradle wrapper launcher is intended for CI; please install Gradle 9.7.1 or use the GitHub Actions workflow.
exit /b 1
