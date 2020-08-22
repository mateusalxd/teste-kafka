FROM alpine:latest AS download

WORKDIR /temp
RUN apk --no-cache add wget && \
    wget -O kafka_2.13-2.6.0.tgz http://mirror.nbtelecom.com.br/apache/kafka/2.6.0/kafka_2.13-2.6.0.tgz && \
    tar xzf kafka_2.13-2.6.0.tgz


FROM alpine:latest

RUN apk --no-cache --update add openjdk8-jre bash

RUN mkdir -p /app/kafka /app/data/zookeeper /app/data/kafka-logs
WORKDIR /app
COPY executar.sh .
RUN chmod +x executar.sh
COPY --from=download /temp/kafka_2.13-2.6.0/ kafka/
RUN sed -i 's/dataDir=\/tmp\/zookeeper/dataDir=\/app\/data\/zookeeper/g' kafka/config/zookeeper.properties
RUN sed -i 's/log\.dirs=\/tmp\/kafka-logs/log\.dirs=\/app\/data\/kafka-logs/g' kafka/config/zookeeper.properties

VOLUME [ "/app/data" ]
EXPOSE 2181
EXPOSE 9092

ENTRYPOINT /app/executar.sh
