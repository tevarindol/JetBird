#!/bin/bash
[ -x "$(command -v gomobile)" ] || { echo "gomobile not in path"; exit 1; }

while getopts 'v' f; do
    case "${f}" in
        v) ((_v++));;
        *) :;;
    esac
done

if [[ $_v -ge 2 ]]
then
  _vverbose=true;
elif [[ $_v -eq 1 ]]
then
  _verbose=true;
fi

[[ $_verbose == true ]] && set -x

mkdir -p lib/
cd ./netbird || { echo "No netbird folder"; exit 1; }
# shellcheck disable=SC2012
[[ $(($(ls -1q . | wc -l))) -eq 0 ]] && git submodule update --init --recursive
git reset --hard HEAD
git apply ../patches/* ${_vverbose:+-v} && echo "Applied patches" || { echo "Applied patches"; exit 1; }

gomobile init

CGO_ENABLED=0 gomobile bind -o "$(pwd)"/../lib/netbird.aar \
    -javapkg=io.netbird \
    -ldflags="-s -w -X golang.zx2c4.com/wireguard/ipc.socketDirectory=/data/data/dev.bg.jetbird/cache/wireguard -X github.com/netbirdio/netbird/version.version=$(git rev-parse --short HEAD) -buildid=" \
    -androidapi 24 \
    -trimpath \
    ${_vverbose:+-v} \
    "$(pwd)"/client/android && echo "Built netbird" || { echo "Failed to build netbird"; exit 1; }
