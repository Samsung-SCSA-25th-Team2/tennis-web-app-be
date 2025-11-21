#!/bin/bash

set -e  # 에러 발생 시 즉시 중단

# 애플리케이션 시작
echo "Starting application..."

# 애플리케이션 디렉토리
APP_DIR=/home/ec2-user/app
cd $APP_DIR

echo "Current directory: $(pwd)"
echo "Directory contents:"
ls -la

# 환경변수 파일 로드
ENV_FILE=$APP_DIR/.env.prod
if [ -f "$ENV_FILE" ]; then
    echo "Loading environment variables from $ENV_FILE"
    set -a  # 모든 변수를 자동으로 export
    source $ENV_FILE
    set +a
    echo "Environment variables loaded successfully"
else
    echo "Warning: Environment file not found at $ENV_FILE"
    echo "Application will use default values from application.yml"
fi

# Java 버전 확인
echo "Java version:"
java -version 2>&1 | head -1

# JAR 파일 찾기 (여러 경로 시도)
JAR_FILE=""

# 1. build/libs/ 디렉토리에서 찾기
if [ -d "$APP_DIR/build/libs" ]; then
    JAR_FILE=$(ls -t $APP_DIR/build/libs/*.jar 2>/dev/null | grep -v plain | head -1)
fi

# 2. 루트 디렉토리에서 찾기
if [ -z "$JAR_FILE" ]; then
    JAR_FILE=$(ls -t $APP_DIR/*.jar 2>/dev/null | grep -v plain | head -1)
fi

# 3. find로 전체 검색
if [ -z "$JAR_FILE" ]; then
    JAR_FILE=$(find $APP_DIR -name "*.jar" -type f ! -name "*plain*" | head -1)
fi

if [ -z "$JAR_FILE" ]; then
    echo "Error: JAR file not found in $APP_DIR"
    echo "Directory contents:"
    ls -la $APP_DIR
    exit 1
fi

echo "Found JAR file: $JAR_FILE"

# 로그 디렉토리 생성
LOG_DIR=/var/log/tennis-web-app
sudo mkdir -p $LOG_DIR
sudo chown ec2-user:ec2-user $LOG_DIR

echo "Log directory: $LOG_DIR"

# 이전 포트 사용 확인
if lsof -i :8080 > /dev/null 2>&1; then
    echo "Warning: Port 8080 is already in use"
    lsof -i :8080
fi

# 애플리케이션 실행
echo "Executing: java -jar $JAR_FILE"
nohup java -jar $JAR_FILE > $LOG_DIR/application.log 2>&1 &

# PID 저장
PID=$!
echo $PID > $APP_DIR/application.pid

echo "Application started with PID: $PID"
echo "Log file: $LOG_DIR/application.log"
echo "Waiting for application to start..."

# 프로세스가 실제로 실행 중인지 확인
sleep 2
if ! ps -p $PID > /dev/null; then
    echo "ERROR: Application process died immediately after start"
    echo "Last 50 lines of log:"
    tail -50 $LOG_DIR/application.log
    exit 1
fi

# 애플리케이션 시작 대기 (최대 60초)
for i in {1..60}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "Application started successfully!"
        exit 0
    fi

    # 프로세스가 살아있는지 중간 확인
    if ! ps -p $PID > /dev/null; then
        echo "ERROR: Application process terminated unexpectedly"
        echo "Last 50 lines of log:"
        tail -50 $LOG_DIR/application.log
        exit 1
    fi

    echo "Waiting for application to be ready... ($i/60)"
    sleep 1
done

echo "WARNING: Application health check timeout"
echo "Process is still running but not responding"
echo "Last 50 lines of log:"
tail -50 $LOG_DIR/application.log
exit 0