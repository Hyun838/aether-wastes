@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion
cd /d "%~dp0"
title Aether Wastes - build

echo ==================================================
echo    Эфирные Пустоши — сборка мода в один файл
echo ==================================================
echo.

echo "%CD%" | findstr /c:" " >nul && (
  echo [ВНИМАНИЕ] В пути к папке есть пробелы. Если сборка упадёт,
  echo     перенесите папку, например, в C:\aetherwastes
  echo.
)

rem --- 1. Java 21: скачиваем переносную, если её ещё нет ---
if not exist ".jdk\bin\java.exe" (
  echo [1/3] Скачиваю Java 21 ^(один раз, около 200 МБ^)...
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ErrorActionPreference='Stop'; $ProgressPreference='SilentlyContinue';" ^
    "Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk' -OutFile 'jdk.zip';" ^
    "Expand-Archive -Path 'jdk.zip' -DestinationPath '.jdk-tmp' -Force;" ^
    "$d = Get-ChildItem '.jdk-tmp' | Select-Object -First 1; Move-Item $d.FullName '.jdk';" ^
    "Remove-Item '.jdk-tmp' -Recurse -Force; Remove-Item 'jdk.zip' -Force"
  if errorlevel 1 (
    echo.
    echo [X] Не удалось скачать Java. Проверьте интернет или установите Java 21 с https://adoptium.net
    pause
    exit /b 1
  )
) else (
  echo [1/3] Java 21 уже есть.
)
set "JAVA_HOME=%CD%\.jdk"
set "PATH=%JAVA_HOME%\bin;%PATH%"

rem --- 2. Сборка ---
echo [2/3] Собираю мод. Первый раз это занимает 5-15 минут: скачиваются Minecraft и NeoForge.
echo       Не закрывайте окно. Подробный журнал пишется в build-log.txt
call gradlew.bat build --no-daemon --console=plain > build-log.txt 2>&1
if errorlevel 1 (
  echo.
  echo [X] Сборка не удалась. Последние строки журнала:
  echo --------------------------------------------------
  powershell -NoProfile -Command "Get-Content build-log.txt -Tail 40"
  echo --------------------------------------------------
  echo Пришлите файл build-log.txt — по нему ошибку легко исправить.
  pause
  exit /b 1
)

rem --- 3. Готовый файл ---
set "JAR="
for %%f in (build\libs\*.jar) do (
  echo %%~nf | findstr /i "sources" >nul || set "JAR=%%f"
)
if not defined JAR (
  echo [X] Сборка прошла, но jar не найден в build\libs
  pause
  exit /b 1
)
copy /y "!JAR!" "AetherWastes.jar" >nul
echo [3/3] Готово. Мод: %CD%\AetherWastes.jar
echo.

set "MODS=%APPDATA%\.minecraft\mods"
choice /c YN /m "Скопировать мод в %MODS% ?"
if errorlevel 2 goto done
if not exist "%MODS%" mkdir "%MODS%"
copy /y "AetherWastes.jar" "%MODS%\AetherWastes.jar" >nul
echo Скопировано. Запускайте Minecraft с профилем NeoForge 1.21.1.

:done
echo.
echo Нужен установленный NeoForge для Minecraft 1.21.1: https://neoforged.net
explorer /select,"%CD%\AetherWastes.jar"
pause
