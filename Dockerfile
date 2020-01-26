FROM openjdk:8-slim

RUN apt-get update && apt-get install -y python3 python3-pip curl

COPY ./build/libs/kafka-dsf-all.jar /usr/local/bin/kafka-dsf-all.jar
COPY ./kafka-dsf /usr/local/bin/kafka-dsf

