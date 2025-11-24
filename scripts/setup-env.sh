#!/bin/bash

set -e

echo "Setting up environment configuration from S3..."

APP_DIR=/home/ec2-user/app
ENV_FILE=$APP_DIR/.env.prod
REGION="ap-northeast-2"
S3_BUCKET="tennis-app-config"
S3_KEY="prod/.env.prod"

# 로그 디렉토리 생성
LOG_DIR="/var/log/tennis-web-app"
sudo mkdir -p $LOG_DIR
sudo chown ec2-user:ec2-user $LOG_DIR

echo "Downloading environment file from S3..."

# S3에서 .env.prod 파일 다운로드 (서버측 암호화된 파일)
if aws s3 cp "s3://${S3_BUCKET}/${S3_KEY}" "$ENV_FILE" --region $REGION; then
    echo "✓ Successfully downloaded .env.prod from S3"
else
    echo "✗ ERROR: Failed to download .env.prod from S3"
    echo "  Bucket: s3://${S3_BUCKET}/${S3_KEY}"
    echo "  Please check:"
    echo "  1. S3 bucket exists and file is uploaded"
    echo "  2. EC2 IAM role has s3:GetObject permission"
    exit 1
fi

# 파일 권한 설정
chmod 600 $ENV_FILE

# 파일 검증
if [ ! -s "$ENV_FILE" ]; then
    echo "✗ ERROR: Downloaded .env.prod file is empty"
    exit 1
fi

ENV_COUNT=$(grep -c "=" "$ENV_FILE" || true)
echo "✓ Environment file loaded with $ENV_COUNT variables"

# 필수 환경변수 검증
REQUIRED_VARS=("DB_PASSWORD" "JWT_SECRET" "KAKAO_CLIENT_SECRET")

for var in "${REQUIRED_VARS[@]}"; do
    if ! grep -q "^${var}=" "$ENV_FILE"; then
        echo "✗ ERROR: Required variable $var is missing"
        exit 1
    fi
done

echo "✓ All required variables validated successfully"

# ENV_FILE 환경변수를 .bashrc에 설정 (애플리케이션이 참조할 수 있도록)
echo "export ENV_FILE=.env.prod" >> /home/ec2-user/.bashrc
