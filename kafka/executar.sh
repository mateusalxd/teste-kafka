#!/bin/bash

/app/kafka/bin/zookeeper-server-start.sh /app/kafka/config/zookeeper.properties &
sleep 2

finalizar() {
    SIGNAL=${SIGNAL:-TERM}
    PIDS=$(netstat -antlp | tr -s ' ' | cut -d ' ' -f 7 | grep java | sort -nr | uniq | cut -d '/' -f 1)
    for PID in $PIDS; do
        kill -s $SIGNAL $PID
        wait $PID
    done

    sleep 2
}

trap 'finalizar' SIGTERM
/app/kafka/bin/kafka-server-start.sh /app/kafka/config/server.properties &
wait $!
