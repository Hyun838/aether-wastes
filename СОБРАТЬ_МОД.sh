#!/usr/bin/env bash
# Эфирные Пустоши — сборка мода (macOS / Linux)
set -e
cd "$(dirname "$0")"
echo "=== Эфирные Пустоши: сборка мода ==="

if [ -x ".jdk/bin/java" ] || [ -x ".jdk/Contents/Home/bin/java" ]; then
  echo "[1/3] Java 21 уже есть."
elif command -v java >/dev/null 2>&1 && java -version 2>&1 | grep -q 'version "21'; then
  echo "[1/3] Используется системная Java 21."
else
  echo "[1/3] Скачиваю Java 21 (один раз)..."
  OS=$(uname -s | tr '[:upper:]' '[:lower:]'); [ "$OS" = "darwin" ] && OS=mac
  ARCH=$(uname -m); case "$ARCH" in x86_64|amd64) ARCH=x64;; arm64|aarch64) ARCH=aarch64;; esac
  curl -L -o jdk.tar.gz "https://api.adoptium.net/v3/binary/latest/21/ga/$OS/$ARCH/jdk/hotspot/normal/eclipse?project=jdk"
  mkdir -p .jdk-tmp && tar -xzf jdk.tar.gz -C .jdk-tmp && mv .jdk-tmp/* .jdk && rm -rf .jdk-tmp jdk.tar.gz
fi
if [ -x ".jdk/Contents/Home/bin/java" ]; then export JAVA_HOME="$PWD/.jdk/Contents/Home"; elif [ -x ".jdk/bin/java" ]; then export JAVA_HOME="$PWD/.jdk"; fi
[ -n "$JAVA_HOME" ] && export PATH="$JAVA_HOME/bin:$PATH"

echo "[2/3] Собираю мод (первый раз 5-15 минут)..."
chmod +x gradlew
if ! ./gradlew build --no-daemon --console=plain > build-log.txt 2>&1; then
  echo "[X] Сборка не удалась. Последние строки журнала:"; tail -40 build-log.txt
  echo "Пришлите файл build-log.txt — по нему ошибку легко исправить."; exit 1
fi
JAR=$(ls build/libs/*.jar | grep -v sources | head -1)
cp "$JAR" AetherWastes.jar
echo "[3/3] Готово: $PWD/AetherWastes.jar"
echo "Положите его в папку mods профиля NeoForge 1.21.1 (https://neoforged.net)."
