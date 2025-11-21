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

echo "Searching for JAR file..."

# 1. build/libs/ 디렉토리에서 찾기 (gradle-wrapper 제외)
if [ -d "$APP_DIR/build/libs" ]; then
    echo "Checking build/libs/ directory..."
    JAR_FILE=$(ls -t $APP_DIR/build/libs/*.jar 2>/dev/null | grep -v plain | grep -v wrapper | head -1)
    if [ -n "$JAR_FILE" ]; then
        echo "Found in build/libs/: $JAR_FILE"
    fi
fi

# 2. 루트 디렉토리에서 찾기 (gradle-wrapper 제외)
if [ -z "$JAR_FILE" ]; then
    echo "Checking root directory..."
    JAR_FILE=$(ls -t $APP_DIR/*.jar 2>/dev/null | grep -v plain | grep -v wrapper | head -1)
    if [ -n "$JAR_FILE" ]; then
        echo "Found in root: $JAR_FILE"
    fi
fi

# 3. find로 scsa 이름의 JAR 찾기 (가장 확실한 방법)
if [ -z "$JAR_FILE" ]; then
    echo "Searching for scsa*.jar..."
    JAR_FILE=$(find $APP_DIR -name "scsa*.jar" -type f ! -name "*plain*" | head -1)
    if [ -n "$JAR_FILE" ]; then
        echo "Found scsa JAR: $JAR_FILE"
    fi
fi

# 4. gradle-wrapper가 아닌 모든 JAR 검색
if [ -z "$JAR_FILE" ]; then
    echo "Searching for any non-wrapper JAR..."
    JAR_FILE=$(find $APP_DIR -name "*.jar" -type f ! -name "*plain*" ! -name "*wrapper*" ! -path "*/gradle/*" | head -1)
    if [ -n "$JAR_FILE" ]; then
        echo "Found JAR: $JAR_FILE"
    fi
fi

if [ -z "$JAR_FILE" ]; then
    echo "ERROR: JAR file not found in $APP_DIR"
    echo "Directory structure:"
    find $APP_DIR -name "*.jar" -type f
    echo ""
    echo "Directory contents:"
    ls -laR $APP_DIR
    exit 1
fi

echo "Selected JAR file: $JAR_FILE"

# JAR 파일 검증
if [ ! -f "$JAR_FILE" ]; then
    echo "ERROR: JAR file does not exist: $JAR_FILE"
    exit 1
fi

# 실행 가능한 JAR인지 확인
if ! jar tf "$JAR_FILE" | grep -q "BOOT-INF"; then
    echo "WARNING: JAR file may not be a Spring Boot executable JAR"
    echo "Attempting to run anyway..."
fi

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