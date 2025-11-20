#!/bin/bash

# 기존 애플리케이션 프로세스 중지
echo "Stopping existing application..."

# PID 파일 위치
PID_FILE=/home/ec2-user/app/application.pid

# PID 파일이 존재하면 프로세스 종료
if [ -f $PID_FILE ]; then
    PID=$(cat $PID_FILE)
    echo "Found PID: $PID"

    if ps -p $PID > /dev/null; then
        echo "Killing process $PID..."
        kill -15 $PID

        # 프로세스가 종료될 때까지 최대 30초 대기
        for i in {1..30}; do
            if ! ps -p $PID > /dev/null; then
                echo "Process stopped successfully"
                break
            fi
            echo "Waiting for process to stop... ($i/30)"
            sleep 1
        done

        # 여전히 실행 중이면 강제 종료
        if ps -p $PID > /dev/null; then
            echo "Force killing process $PID..."
            kill -9 $PID
        fi
    else
        echo "Process $PID not found (already stopped)"
    fi

    rm -f $PID_FILE
else
    echo "No PID file found - application may not be running"
fi

echo "Stop script completed"
