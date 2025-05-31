#!/bin/bash
export PATH="${PATH}:${GOBIN}"
export PATH="${PATH}:${JAVA_HOME}/bin"

[ -x "$(command -v gomobile)" ] || {
    echo "gomobile not in path"
    exit 1
}

while getopts 'vd' f; do
    case "${f}" in
        v) ((_v++));;
        d) _d=1;;
        *) :;;
    esac
done

if [[ $_v -ge 2 ]]; then
    _vverbose=true
elif [[ $_v -eq 1 ]]; then
    _verbose=true
fi

if [[ $_d -eq 1 ]]; then
    _pkg="dev.bg.jetbird.debug"
else
    _pkg="dev.bg.jetbird"
fi

[[ $_verbose == true ]] && set -x

mkdir -p lib/
cd ./netbird || {
    echo "No netbird folder"
    exit 1
}

go clean

# shellcheck disable=SC2012
[[ $(($(ls -1q . | wc -l))) -eq 0 ]] && git submodule update --init --recursive
git reset --hard HEAD
git apply ../patches/* ${_vverbose:+-v} && echo "Applied patches" || {
    echo "Applied patches"
    exit 1
}

gomobile init

# TODO update IPC dir to reflect current package name
CGO_ENABLED=0 gomobile bind -o "$(pwd)"/../lib/netbird.aar \
    -target=android \
    -androidapi 24 \
    -javapkg=io.netbird \
    -ldflags="all=-s -w -X golang.zx2c4.com/wireguard/ipc.socketDirectory=/data/data/${_pkg}/cache/wireguard -X github.com/netbirdio/netbird/version.version=$(git rev-parse --short=8 HEAD) -buildid=" \
    -trimpath \
    ${_vverbose:+-v} \
    ${_vverbose:+-x} \
    "$(pwd)"/client/android && echo "Built netbird" || {
        echo "Failed to build netbird"
        exit 1
    }
