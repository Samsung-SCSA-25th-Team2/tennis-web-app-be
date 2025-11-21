#!/bin/bash

# .env.prod 파일을 S3에 암호화하여 업로드하는 스크립트
# 사용법: ./scripts/upload-to-s3.sh

set -e

REGION="ap-northeast-2"
S3_BUCKET="tennis-app-config"
S3_KEY="prod/.env.prod"
LOCAL_FILE=".env.prod"

echo "=========================================="
echo "S3에 환경변수 파일 업로드"
echo "=========================================="
echo "Region: $REGION"
echo "Bucket: s3://${S3_BUCKET}"
echo "Key: $S3_KEY"
echo "Local file: $LOCAL_FILE"
echo ""

# .env.prod 파일 존재 확인
if [ ! -f "$LOCAL_FILE" ]; then
    echo "✗ ERROR: $LOCAL_FILE 파일을 찾을 수 없습니다."
    echo ""
    echo "다음 단계를 먼저 수행하세요:"
    echo "1. cp .env.prod.template .env.prod"
    echo "2. .env.prod 파일을 편집하여 실제 운영 값 입력"
    exit 1
fi

# .env.prod 파일이 비어있지 않은지 확인
if [ ! -s "$LOCAL_FILE" ]; then
    echo "✗ ERROR: $LOCAL_FILE 파일이 비어있습니다."
    exit 1
fi

# 파일 내용 간단히 검증
ENV_COUNT=$(grep -c "=" "$LOCAL_FILE" || true)
echo "환경변수 개수: $ENV_COUNT"

if [ $ENV_COUNT -eq 0 ]; then
    echo "✗ ERROR: $LOCAL_FILE에 유효한 환경변수가 없습니다."
    exit 1
fi

echo ""
echo "S3 버킷 확인 중..."

# S3 버킷 존재 확인
if ! aws s3 ls "s3://${S3_BUCKET}" --region $REGION > /dev/null 2>&1; then
    echo "⚠ S3 버킷이 존재하지 않습니다. 생성하시겠습니까? (y/N)"
    read -r response
    if [[ "$response" =~ ^[Yy]$ ]]; then
        echo "S3 버킷 생성 중..."
        aws s3 mb "s3://${S3_BUCKET}" --region $REGION

        # 버킷 퍼블릭 액세스 차단 (보안)
        aws s3api put-public-access-block \
            --bucket $S3_BUCKET \
            --public-access-block-configuration \
                "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true" \
            --region $REGION

        # 버킷 암호화 활성화 (AES256)
        aws s3api put-bucket-encryption \
            --bucket $S3_BUCKET \
            --server-side-encryption-configuration '{
                "Rules": [{
                    "ApplyServerSideEncryptionByDefault": {
                        "SSEAlgorithm": "AES256"
                    },
                    "BucketKeyEnabled": true
                }]
            }' \
            --region $REGION

        # 버저닝 활성화 (변경 이력 관리)
        aws s3api put-bucket-versioning \
            --bucket $S3_BUCKET \
            --versioning-configuration Status=Enabled \
            --region $REGION

        echo "✓ S3 버킷 생성 완료 (암호화 및 버저닝 활성화)"
    else
        echo "작업을 취소합니다."
        exit 1
    fi
else
    echo "✓ S3 버킷 확인 완료"
fi

echo ""
echo "파일 업로드 중..."

# S3에 업로드 (서버측 암호화)
if aws s3 cp "$LOCAL_FILE" "s3://${S3_BUCKET}/${S3_KEY}" \
    --region $REGION \
    --server-side-encryption AES256 \
    --no-progress; then
    echo ""
    echo "=========================================="
    echo "✓ 업로드 성공!"
    echo "=========================================="
    echo "S3 URI: s3://${S3_BUCKET}/${S3_KEY}"
    echo ""
    echo "파일 확인:"
    echo "aws s3 ls s3://${S3_BUCKET}/${S3_KEY} --region $REGION"
    echo ""
    echo "다음 단계:"
    echo "1. EC2 IAM Role에 S3 읽기 권한 추가"
    echo "2. Git push로 배포 시작"
else
    echo ""
    echo "✗ 업로드 실패"
    echo "AWS 자격증명과 권한을 확인하세요."
    exit 1
fi
