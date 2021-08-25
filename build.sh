#!/usr/bin/env bash
set -e

mkdir -p build/output

cat stub.sh ./build/libs/kafka-gitops.jar > build/output/kafka-gitops
chmod +x build/output/kafka-gitops
