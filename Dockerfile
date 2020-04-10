FROM openjdk:8-slim

RUN apt-get update && apt-get install -y python3 python3-pip curl

COPY ./build/output/kafka-gitops /usr/local/bin/kafka-gitops

