FROM openjdk:8-slim

RUN apt-get update && apt-get install -y python3 python3-pip curl

COPY ./build/libs/kafka-gitops-all.jar /usr/local/bin/kafka-gitops-all.jar
COPY ./kafka-gitops /usr/local/bin/kafka-gitops

