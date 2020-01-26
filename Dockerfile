FROM openjdk:8-alpine

RUN apk update && apk add bash && apk add python3

COPY ./build/libs/kafka-dsf-all.jar /usr/local/bin/kafka-dsf-all.jar
COPY ./kafka-dsf /usr/local/bin/kafka-dsf

