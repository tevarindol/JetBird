#!/bin/bash
set -eux
export PATH="${PATH}:${ANDROID_HOME}/build-tools/34.0.0"
export PATH="${PATH}:${JAVA_HOME}/bin"

[ -z "${APK_KS}" ] && {
    echo "Missing keystore file (KS)"
    exit 1
}
[ -z "${APK_KS_ALIAS}" ] && {
    echo "Missing keystore alias (KS_ALIAS)"
    exit 1
}
[ -z "${APK_KS_PASS}" ] && {
    echo "Missing keystore password (KS_PASS)"
    exit 1
}

echo -n "${APK_KS}" | base64 -di >/tmp/keystore.keystore

for file in ./app/build/outputs/apk/release/*.apk; do
    if [ -f "$file" ]; then
        arch=$(basename "${file}" | sed s/"app-"//g | sed s/"-unsigned"//g)
        echo "Signing ${arch}..."
        apksigner sign --ks /tmp/keystore.keystore --ks-key-alias "${APK_KS_ALIAS}" --ks-pass pass:"${APK_KS_PASS}" --out "JetBird-${arch}" "$file"
    fi
done
