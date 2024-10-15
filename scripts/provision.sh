#!/bin/bash
set -eux
apt-get update
apt-get install -y --no-install-recommends ca-certificates curl git unzip

# Download, verify and install JDK-21
curl -o /tmp/jdk.tar.gz https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.tar.gz
sha256sum -c <<< "9c2f7c39e0d5b296ce50e563740694b2ebfe4a620415d1b2b848ba47bebceb47 /tmp/jdk.tar.gz"
tar -C /build -xzf /tmp/jdk.tar.gz
rm -f /tmp/jdk.tar.gz
export PATH="${PATH}:${JAVA_HOME}/bin"

# Download, verify and install Android SDK cli tools
curl -o /tmp/cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
sha256sum -c <<< "2d2d50857e4eb553af5a6dc3ad507a17adf43d115264b1afc116f95c92e5e258 /tmp/cmdline-tools.zip"
mkdir -p "${ANDROID_HOME}"
unzip -q -d "${ANDROID_HOME}" /tmp/cmdline-tools.zip
rm -f /tmp/cmdline-tools.zip
export PATH="${PATH}:${ANDROID_HOME}/cmdline-tools/bin"

# Download and install remaining Android SDK tools
(yes || true) | sdkmanager --sdk_root="${ANDROID_HOME}" --licenses >/dev/null
sdkmanager --sdk_root="${ANDROID_HOME}" "build-tools;34.0.0"
sdkmanager --sdk_root="${ANDROID_HOME}" "ndk;27.1.12297006"
sdkmanager --sdk_root="${ANDROID_HOME}" "platforms;android-34"
sdkmanager --sdk_root="${ANDROID_HOME}" "platform-tools"

# Download, verify and install Go
curl -o /tmp/go.tar.gz https://dl.google.com/go/go1.23.1.linux-amd64.tar.gz
sha256sum -c <<< "49bbb517cfa9eee677e1e7897f7cf9cfdbcf49e05f61984a2789136de359f9bd /tmp/go.tar.gz"
tar -C /build -xzf /tmp/go.tar.gz
rm -f /tmp/go.tar.gz
export PATH="${PATH}:${GOBIN}"

go install golang.org/x/mobile/cmd/gomobile@c6794c95c70b02ddb1a79d92fec29f8b8ddd8836
go install golang.org/x/mobile/cmd/gobind@c6794c95c70b02ddb1a79d92fec29f8b8ddd8836
