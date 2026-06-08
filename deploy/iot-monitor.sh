#!/bin/bash

APP_NAME="iot-monitor"
APP_JAR="iot-api-1.0.0.jar"
APP_HOME="/opt/iot-monitor"
LOG_DIR="/var/log/iot-monitor"
PID_FILE="/var/run/${APP_NAME}.pid"
PROFILE="prod"

JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

start() {
    echo "Starting $APP_NAME..."
    
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            echo "$APP_NAME is already running (PID: $PID)"
            return 1
        else
            rm -f "$PID_FILE"
        fi
    fi

    mkdir -p "$LOG_DIR"
    
    nohup java $JAVA_OPTS -jar "$APP_HOME/$APP_JAR" \
        --spring.profiles.active=$PROFILE \
        >> "$LOG_DIR/${APP_NAME}.log" 2>&1 &
    
    echo $! > "$PID_FILE"
    echo "$APP_NAME started (PID: $(cat $PID_FILE))"
}

stop() {
    echo "Stopping $APP_NAME..."
    
    if [ ! -f "$PID_FILE" ]; then
        echo "$APP_NAME is not running"
        return 1
    fi
    
    PID=$(cat "$PID_FILE")
    if ps -p "$PID" > /dev/null 2>&1; then
        kill "$PID"
        sleep 5
        
        if ps -p "$PID" > /dev/null 2>&1; then
            echo "Force stopping $APP_NAME..."
            kill -9 "$PID"
        fi
        
        rm -f "$PID_FILE"
        echo "$APP_NAME stopped"
    else
        rm -f "$PID_FILE"
        echo "$APP_NAME is not running"
    fi
}

restart() {
    stop
    sleep 2
    start
}

status() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            echo "$APP_NAME is running (PID: $PID)"
            return 0
        fi
    fi
    echo "$APP_NAME is not running"
    return 1
}

case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac

exit $?
