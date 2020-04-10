#!/usr/bin/env bash

mkdir -p build/output

cat stub.sh ./build/libs/kafka-gitops-all.jar > build/output/kafka-gitops
chmod +x build/output/kafka-gitops