#!/bin/bash
set -eux

# Download, verify and install Go
curl -o /tmp/go.tar.gz https://dl.google.com/go/go1.23.1.linux-amd64.tar.gz
sha256sum -c <<< "49bbb517cfa9eee677e1e7897f7cf9cfdbcf49e05f61984a2789136de359f9bd /tmp/go.tar.gz"
tar -C /build -xzf /tmp/go.tar.gz
rm -f /tmp/go.tar.gz
export PATH="${PATH}:${GOBIN}"

go install golang.org/x/mobile/cmd/gomobile@c6794c95c70b02ddb1a79d92fec29f8b8ddd8836
go install golang.org/x/mobile/cmd/gobind@c6794c95c70b02ddb1a79d92fec29f8b8ddd8836
