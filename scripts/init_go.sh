#!/bin/bash
set -eux

# Download, verify and install Go
# https://go.dev/dl
wget -qO /tmp/go.tar.gz https://go.dev/dl/go1.24.6.linux-amd64.tar.gz
sha256sum -c <<< "bbca37cc395c974ffa4893ee35819ad23ebb27426df87af92e93a9ec66ef8712 /tmp/go.tar.gz"
tar -C /build -xzf /tmp/go.tar.gz
rm -f /tmp/go.tar.gz
export PATH="${PATH}:${GOBIN}"

go install golang.org/x/mobile/cmd/gomobile@395d808d53cdcc50f14b84fcb8049ba2c6427a69
go install golang.org/x/mobile/cmd/gobind@395d808d53cdcc50f14b84fcb8049ba2c6427a69
