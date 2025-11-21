#!/bin/bash

# EC2 Launch Template User Data 스크립트
# 이 스크립트는 EC2 인스턴스가 처음 시작될 때 자동으로 실행됩니다.
# Launch Template → Advanced details → User data에 이 내용을 붙여넣으세요.

set -e

# 로그 파일 설정
LOG_FILE="/var/log/user-data.log"
exec > >(tee -a ${LOG_FILE}) 2>&1

echo "=========================================="
echo "EC2 User Data 스크립트 시작"
echo "Time: $(date)"
echo "=========================================="

# 1. 시스템 패키지 업데이트
echo "[1/5] 시스템 패키지 업데이트..."
yum update -y

# 2. 필수 패키지 설치
echo "[2/5] 필수 패키지 설치..."
yum install -y \
    java-17-amazon-corretto \
    ruby \
    wget \
    git

# 3. CodeDeploy Agent 설치
echo "[3/5] CodeDeploy Agent 설치..."
cd /home/ec2-user
if [ ! -f "/opt/codedeploy-agent/bin/codedeploy-agent" ]; then
    wget https://aws-codedeploy-ap-northeast-2.s3.ap-northeast-2.amazonaws.com/latest/install
    chmod +x ./install
    ./install auto
    rm -f ./install
    echo "CodeDeploy Agent 설치 완료"
else
    echo "CodeDeploy Agent 이미 설치됨"
fi

# CodeDeploy Agent 시작 및 자동 시작 설정
systemctl start codedeploy-agent
systemctl enable codedeploy-agent
systemctl status codedeploy-agent

# 4. 애플리케이션 디렉토리 생성
echo "[4/5] 애플리케이션 디렉토리 생성..."
mkdir -p /home/ec2-user/app
chown -R ec2-user:ec2-user /home/ec2-user/app

# 로그 디렉토리 생성
mkdir -p /var/log/tennis-web-app
chown -R ec2-user:ec2-user /var/log/tennis-web-app

# 5. CloudWatch Logs Agent 설치 (선택사항)
echo "[5/5] CloudWatch Logs Agent 설치..."
if [ ! -f "/opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl" ]; then
    wget https://s3.amazonaws.com/amazoncloudwatch-agent/amazon_linux/amd64/latest/amazon-cloudwatch-agent.rpm
    rpm -U ./amazon-cloudwatch-agent.rpm
    rm -f ./amazon-cloudwatch-agent.rpm
    echo "CloudWatch Agent 설치 완료"
else
    echo "CloudWatch Agent 이미 설치됨"
fi

echo "=========================================="
echo "EC2 User Data 스크립트 완료"
echo "Time: $(date)"
echo "=========================================="

# 시스템 정보 출력
echo ""
echo "시스템 정보:"
echo "- Java Version: $(java -version 2>&1 | head -n 1)"
echo "- AWS CLI Version: $(aws --version)"
echo "- CodeDeploy Agent: $(systemctl is-active codedeploy-agent)"
echo ""
echo "준비 완료! CodeDeploy 배포를 기다리는 중..."
