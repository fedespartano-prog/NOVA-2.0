
@echo off
setlocal

set "GRADLE_VERSION=8.9"

if not defined GRADLE_USER_HOME (
    set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
)

set "GRADLE_DIR=%GRADLE_USER_HOME%\manual-gradle"
set "GRADLE_HOME=%GRADLE_DIR%\gradle-%GRADLE_VERSION%"

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
    if not exist "%GRADLE_DIR%" mkdir "%GRADLE_DIR%"

    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $zip=Join-Path $env:TEMP 'gradle-8.9-bin.zip'; Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-8.9-bin.zip' -OutFile $zip; Expand-Archive -Path $zip -DestinationPath '%GRADLE_DIR%' -Force; Remove-Item $zip"

    if errorlevel 1 exit /b 1
)

call "%GRADLE_HOME%\bin\gradle.bat" -p "%~dp0" %*

exit /b %ERRORLEVEL%
