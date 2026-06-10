#!/bin/bash
set -eux
apt-get update
apt-get install -y --no-install-recommends ca-certificates wget git unzip

# Download, verify and install JDK-21
# https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.tar.gz.sha256
wget -qO /tmp/jdk.tar.gz https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.tar.gz
sha256sum -c <<< "e1c25a83f9e2e374c93e0c29cc3d98a947621ae0fefa4a8d932951eb160c47c3  /tmp/jdk.tar.gz"
tar -C /build -xzf /tmp/jdk.tar.gz
rm -f /tmp/jdk.tar.gz
export PATH="${PATH}:${JAVA_HOME}/bin"

# Download, verify and install Android SDK cli tools
wget -qO /tmp/cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
sha256sum -c <<< "2d2d50857e4eb553af5a6dc3ad507a17adf43d115264b1afc116f95c92e5e258 /tmp/cmdline-tools.zip"
mkdir -p "${ANDROID_HOME}"
unzip -q -d "${ANDROID_HOME}" /tmp/cmdline-tools.zip
rm -f /tmp/cmdline-tools.zip
export PATH="${PATH}:${ANDROID_HOME}/cmdline-tools/bin"

# Download and install remaining Android SDK tools
(yes || true) | sdkmanager --sdk_root="${ANDROID_HOME}" --licenses 1>/dev/null
sdkmanager --sdk_root="${ANDROID_HOME}" "build-tools;34.0.0"
sdkmanager --sdk_root="${ANDROID_HOME}" "ndk;28.2.13676358"
sdkmanager --sdk_root="${ANDROID_HOME}" "platforms;android-36"
sdkmanager --sdk_root="${ANDROID_HOME}" "platform-tools"
