#!/bin/bash

# 애플리케이션 시작
echo "Starting application..."

# 애플리케이션 디렉토리
APP_DIR=/home/ec2-user/app
cd $APP_DIR

# 환경변수 파일 로드
ENV_FILE=$APP_DIR/.env.prod
if [ -f "$ENV_FILE" ]; then
    echo "Loading environment variables from $ENV_FILE"
    export $(grep -v '^#' $ENV_FILE | xargs)
else
    echo "Warning: Environment file not found at $ENV_FILE"
    echo "Application will use default values from application.yml"
fi

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

# 애플리케이션 실행
nohup java -jar $JAR_FILE > $LOG_DIR/application.log 2>&1 &

# PID 저장
echo $! > $APP_DIR/application.pid

echo "Application started with PID: $(cat $APP_DIR/application.pid)"
echo "Waiting for application to start..."

# 애플리케이션 시작 대기 (최대 60초)
for i in {1..60}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "Application started successfully!"
        exit 0
    fi
    echo "Waiting for application to be ready... ($i/60)"
    sleep 1
done

echo "Warning: Application may not have started properly. Check logs at $LOG_DIR/application.log"
exit 0