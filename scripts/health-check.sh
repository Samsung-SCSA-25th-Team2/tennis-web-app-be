#!/bin/bash

# 애플리케이션 헬스 체크
echo "Checking application health..."

# 최대 30초 동안 헬스 체크 시도
for i in {1..30}; do
    # Internal health endpoint 체크 (JWT/예외 처리와 완전히 분리됨)
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/internal/health)

    if [ "$HTTP_CODE" -eq 200 ]; then
        echo "Health check passed! (HTTP $HTTP_CODE)"
        exit 0
    fi

    echo "Health check attempt $i/30 failed (HTTP $HTTP_CODE). Retrying..."
    sleep 1
done

echo "Health check failed after 30 attempts"
exit 1