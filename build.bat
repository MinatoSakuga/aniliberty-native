@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
title AniLiberty - Сборка APK
echo ============================================
echo   AniLiberty - автосборка APK с нуля
echo   Скачает JDK 17, Android SDK и соберёт APK
echo ============================================
echo.

set "ROOT=%~dp0"
set "TOOLS=%ROOT%.toolchain"

REM ---------- 1. JDK 17 ----------
if not exist "%TOOLS%\jdk17\bin\java.exe" (
  echo [1/4] Скачиваю JDK 17 ^(Temurin, ~180 МБ^)...
  if not exist "%TOOLS%" mkdir "%TOOLS%"
  curl -L --progress-bar -o "%TOOLS%\jdk17.zip" "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse" || goto :fail
  tar -xf "%TOOLS%\jdk17.zip" -C "%TOOLS%" || goto :fail
  for /d %%D in ("%TOOLS%\jdk-17*") do ren "%%~D" jdk17
  del "%TOOLS%\jdk17.zip"
) else (
  echo [1/4] JDK 17 уже установлен.
)
set "JAVA_HOME=%TOOLS%\jdk17"

REM ---------- 2. Android SDK ----------
if not exist "%TOOLS%\sdk\cmdline-tools\latest\bin\sdkmanager.bat" (
  echo [2/4] Скачиваю Android SDK ^(cmdline-tools, ~150 МБ^)...
  curl -L --progress-bar -o "%TOOLS%\cmdtools.zip" "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip" || goto :fail
  mkdir "%TOOLS%\sdk\cmdline-tools" 2>nul
  tar -xf "%TOOLS%\cmdtools.zip" -C "%TOOLS%\sdk\cmdline-tools" || goto :fail
  ren "%TOOLS%\sdk\cmdline-tools\cmdline-tools" latest
  del "%TOOLS%\cmdtools.zip"
) else (
  echo [2/4] Android SDK уже скачан.
)
set "ANDROID_HOME=%TOOLS%\sdk"
set "ANDROID_USER_HOME=%TOOLS%\android-user"

echo [3/4] Устанавливаю пакеты SDK: platform-tools, android-35, build-tools 35.0.0...
echo y| "%TOOLS%\sdk\cmdline-tools\latest\bin\sdkmanager.bat" --sdk_root="%TOOLS%\sdk" --licenses >nul 2>&1
"%TOOLS%\sdk\cmdline-tools\latest\bin\sdkmanager.bat" --sdk_root="%TOOLS%\sdk" "platform-tools" "platforms;android-35" "build-tools;35.0.0" >nul
if errorlevel 1 goto :fail

REM ---------- 3. local.properties ----------
> "%ROOT%local.properties" echo sdk.dir=%TOOLS:\=/%/sdk

REM ---------- 4. Сборка ----------
echo [4/4] Собираю APK ^(Gradle сам скачает зависимости, первый раз долго^)...
set "PATH=%TOOLS%\jdk17\bin;%PATH%"
call "%ROOT%gradlew.bat" --no-daemon assembleDebug
if errorlevel 1 goto :fail

copy /Y "%ROOT%app\build\outputs\apk\debug\app-debug.apk" "%ROOT%AniLiberty.apk" >nul
echo.
echo ============================================
echo   Готово! APK собран: %ROOT%AniLiberty.apk
echo   Подпись: отладочный ключ из репозитория.
echo ============================================
pause
exit /b 0

:fail
echo.
echo =================== ОШИБКА ===================
echo  Сборка не удалась. Проверьте интернет и
echo  сообщения выше. Нужно ~8 ГБ ОЗУ и ~6 ГБ диска.
echo ===============================================
pause
exit /b 1
