FROM alpine:latest AS download

WORKDIR /temp
RUN apk --no-cache add wget && \
    wget -O kafka_2.13-2.6.0.tgz http://mirror.nbtelecom.com.br/apache/kafka/2.6.0/kafka_2.13-2.6.0.tgz && \
    tar xzf kafka_2.13-2.6.0.tgz


FROM alpine:latest

RUN apk --no-cache --update add openjdk8-jre bash

RUN mkdir -p /app/kafka /app/data
WORKDIR /app
COPY executar.sh .
COPY --from=download /temp/kafka_2.13-2.6.0/ kafka/
RUN sed -i 's/dataDir=\/tmp\/zookeeper/dataDir=\/app\/data\/zookeeper/g' kafka/config/zookeeper.properties && \
    sed -i 's/log\.dirs=\/tmp\/kafka-logs/log\.dirs=\/app\/data\/kafka-logs/g' kafka/config/server.properties && \
    sed -i 's/#listeners=PLAINTEXT:\/\/:9092/listeners=PLAINTEXT:\/\/0\.0\.0\.0:9092/g' kafka/config/server.properties && \
    sed -i 's/#advertised\.listeners=PLAINTEXT:\/\/your\.host\.name:9092/advertised\.listeners=PLAINTEXT:\/\/localhost:9092/g' kafka/config/server.properties && \
    sed -i 's/#listener\.security\.protocol\.map/listener\.security\.protocol\.map/g' kafka/config/server.properties 

VOLUME [ "/app/data" ]
EXPOSE 2181 9092

ENTRYPOINT /app/executar.sh
