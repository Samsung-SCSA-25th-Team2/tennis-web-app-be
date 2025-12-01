# Tennis Match Web Application (Backend)

테니스 매치 매칭 플랫폼의 백엔드 서버

## 프로젝트 소개

테니스를 즐기는 사람들이 원하는 시간, 장소, 조건에 맞는 상대방을 찾아 매치를 주선하는 플랫폼입니다.
사용자는 카카오 소셜 로그인을 통해 간편하게 가입하고, 원하는 조건의 매치를 등록하거나 참여할 수 있으며,
실시간 채팅을 통해 매치 참가자들과 소통할 수 있습니다.

## 주요 기능

### 1. 인증 및 사용자 관리
- **카카오 OAuth2 소셜 로그인**: 간편한 회원가입 및 로그인
- **JWT 기반 인증**: Access Token + Refresh Token을 활용한 무상태 인증
- **Redis 세션 관리**: Refresh Token을 Redis에 저장하여 빠른 검증 및 관리
- **프로필 관리**: 사용자 정보 수정, 프로필 이미지 업로드 (AWS S3)

### 2. 매치 관리
- **매치 생성 및 조회**:
  - 경기 타입 (단식, 복식 등), 시간, 장소, 참가비 설정
  - 모집 조건 설정 (나이, 성별, 경력 등)
- **매치 검색 및 필터링**:
  - 지역, 시간, 경기 타입, 성별 등 다양한 조건으로 검색
  - 페이징 처리로 효율적인 데이터 조회
- **매치 참가 및 취소**:
  - 중복 참가 방지 로직
  - 호스트는 참가 취소 불가 정책
- **자동 상태 업데이트**:
  - Spring Batch를 이용한 스케줄링
  - ShedLock으로 분산 환경에서 중복 실행 방지
  - 매치 시작 시간 지난 경기 자동 완료 처리

### 3. 실시간 채팅
- **WebSocket (STOMP)**: 실시간 양방향 통신
- **RabbitMQ Broker Relay**:
  - 다중 서버 환경에서 메시지 동기화
  - Auto-scaling 대응 가능한 분산 메시지 브로커
- **채팅방 관리**:
  - 1:1 채팅방 자동 생성
  - 중복 채팅방 방지
  - 채팅 히스토리 조회 (커서 기반 페이징)
- **JWT 인증 통합**: WebSocket 연결 시 JWT 검증

### 4. 코트 관리
- **테니스 코트 정보 관리**: 위치, 좌표 정보 포함
- **지도 기반 검색**: 위도/경도 정보를 활용한 위치 기반 검색 지원

## 기술 스택

### Backend Framework
- **Java 17**
- **Spring Boot 3.5.7**
- **Spring Data JPA**: 엔티티 관리 및 DB 접근
- **Spring Security**: 인증/인가 처리
- **Spring Batch**: 배치 작업 스케줄링

### Database & Cache
- **MySQL**: 메인 데이터베이스
- **Redis**: 세션 관리 및 캐싱

### Message Broker & WebSocket
- **RabbitMQ**: 분산 메시지 브로커
- **STOMP over WebSocket**: 실시간 채팅

### Authentication & Security
- **Spring OAuth2 Client**: 카카오 소셜 로그인
- **JWT (jjwt 0.12.3)**: Access/Refresh Token 기반 인증

### Cloud & Storage
- **AWS S3**: 이미지 파일 저장
- **Presigned URL**: 클라이언트 직접 업로드 방식

### CI/CD & Deployment
- **AWS CodePipeline**: CI/CD 파이프라인
- **AWS CodeBuild**: 빌드 자동화
- **AWS CodeDeploy**: 무중단 배포
- **AWS EC2**: 애플리케이션 서버 호스팅

### Etc
- **Lombok**: 보일러플레이트 코드 감소
- **SpringDoc OpenAPI (Swagger)**: API 문서화
- **ShedLock**: 분산 환경 스케줄러 중복 실행 방지

## 프로젝트 구조

```
src/main/java/com/example/scsa/
├── batch/                    # Spring Batch 설정 및 스케줄러
│   ├── MatchStatusBatchConfig.java
│   └── MatchStatusScheduler.java
├── config/                   # 애플리케이션 설정
│   ├── SecurityConfig.java
│   ├── WebSocketConfig.java
│   ├── RedisConfig.java
│   ├── S3Config.java
│   ├── SwaggerConfig.java
│   └── filter/
│       ├── JwtAuthenticationFilter.java
│       └── BotBlockingFilter.java
├── controller/               # REST API 엔드포인트
│   ├── MatchController.java
│   ├── ChatWsController.java
│   ├── CourtController.java
│   └── UserProfileController.java
├── domain/
│   ├── entity/              # JPA 엔티티
│   │   ├── Match.java
│   │   ├── MatchGuest.java
│   │   ├── Court.java
│   │   ├── User.java
│   │   ├── Chat.java
│   │   └── ChatRoom.java
│   └── vo/                  # Value Objects (Enum)
│       ├── MatchStatus.java
│       ├── GameType.java
│       ├── Gender.java
│       └── Period.java
├── dto/                     # 데이터 전송 객체
│   ├── auth/
│   ├── match/
│   ├── chat/
│   └── court/
├── exception/               # 커스텀 예외 및 핸들러
│   ├── GlobalExceptionHandler.java
│   └── ErrorCode.java
├── handler/                 # OAuth2 핸들러
│   └── auth/
├── repository/              # JPA Repository
│   ├── MatchRepository.java
│   ├── UserRepository.java
│   ├── ChatRepository.java
│   └── CourtRepository.java
├── service/                 # 비즈니스 로직
│   ├── match/
│   ├── chat/
│   ├── court/
│   └── auth/
└── util/                    # 유틸리티 클래스
    ├── JwtUtil.java
    └── CookieUtils.java
```

## 핵심 아키텍처 설계

### 1. 인증 플로우
```
1. 사용자 → 카카오 로그인 요청
2. OAuth2 인증 성공 → JWT 생성 (Access + Refresh Token)
3. Refresh Token → Redis 저장 (TTL 설정)
4. 모든 API 요청 시 JwtAuthenticationFilter에서 Access Token 검증
5. Access Token 만료 시 Refresh Token으로 재발급
```

### 2. 실시간 채팅 아키텍처
```
클라이언트 A                      클라이언트 B
    │                                 │
    │ WebSocket Connect               │ WebSocket Connect
    └─────────────┐                  ┌────────────┘
                  │                  │
              [Load Balancer]
                  │
     ┌────────────┼────────────┐
     │            │            │
 [Server 1]   [Server 2]   [Server 3]
     │            │            │
     └────────────┼────────────┘
                  │
            [RabbitMQ Broker]
                  │
         메시지 브로드캐스트
```

### 3. 배치 스케줄링
- **ShedLock**: 다중 서버 환경에서 1개 인스턴스만 배치 실행 보장
- **Spring Batch**: 만료된 매치 자동 완료 처리 (매일 자정 실행)

### 4. CI/CD 파이프라인
```
GitHub Push
    ↓
AWS CodePipeline 트리거
    ↓
AWS CodeBuild (Gradle Build)
    ↓
S3에 아티팩트 업로드
    ↓
AWS CodeDeploy (Blue/Green 배포)
    ↓
EC2 인스턴스 자동 배포
```

## API 문서

애플리케이션 실행 후 Swagger UI를 통해 모든 API 명세를 확인할 수 있습니다.

```
http://localhost:8080/swagger-ui.html
```

### 주요 엔드포인트

#### 매치 API
- `GET /api/v1/matches` - 매치 리스트 조회 (필터링, 페이징)
- `GET /api/v1/matches/{match_id}` - 매치 상세 조회
- `POST /api/v1/my/matches` - 매치 생성 (인증 필요)

#### 채팅 API (WebSocket)
- `CONNECT /ws-stomp` - WebSocket 연결
- `/app/chat.send` - 채팅 메시지 전송
- `/topic/chatroom.{chatRoomId}` - 채팅방 구독

#### 사용자 API
- `GET /api/v1/auth/status` - 로그인 상태 확인
- `PUT /api/v1/profile` - 프로필 수정 (인증 필요)
- `GET /api/v1/presigned-url` - S3 업로드용 Presigned URL 발급

## 환경 변수 설정

`.env.local` 또는 `.env.prod` 파일을 생성하여 다음 환경 변수를 설정합니다.

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/tennis_db
SPRING_DATASOURCE_USERNAME=your_username
SPRING_DATASOURCE_PASSWORD=your_password

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# RabbitMQ
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_STOMP_PORT=61613
SPRING_RABBITMQ_USERNAME=guest
SPRING_RABBITMQ_PASSWORD=guest
SPRING_RABBITMQ_VIRTUAL_HOST=/

# JWT
JWT_SECRET=your_jwt_secret_key_here
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# OAuth2 (Kakao)
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_CLIENT_ID=your_kakao_client_id
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_CLIENT_SECRET=your_kakao_client_secret
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_REDIRECT_URI=http://localhost:8080/login/oauth2/code/kakao

# AWS S3
AWS_S3_BUCKET_NAME=your_bucket_name
AWS_S3_REGION=ap-northeast-2
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

## 로컬 실행 방법

### 사전 요구사항
- Java 17 이상
- MySQL 8.0 이상
- Redis 7.0 이상
- RabbitMQ 3.12 이상 (STOMP 플러그인 활성화 필요)

### 1. 데이터베이스 설정
```sql
CREATE DATABASE tennis_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. RabbitMQ STOMP 플러그인 활성화
```bash
rabbitmq-plugins enable rabbitmq_stomp
rabbitmq-plugins enable rabbitmq_web_stomp
```

### 3. 애플리케이션 실행
```bash
# 의존성 설치 및 빌드
./gradlew clean build

# 애플리케이션 실행 (로컬 환경)
./gradlew bootRun

# 또는 JAR 파일 직접 실행
java -jar build/libs/scsa-0.0.1-SNAPSHOT.jar
```

서버는 기본적으로 `http://localhost:8080`에서 실행됩니다.

## 배포

AWS EC2 환경에서의 자동 배포를 지원합니다.

### 배포 스크립트
- `scripts/setup-env.sh`: 환경 변수 설정
- `scripts/start.sh`: 애플리케이션 시작 (Blue/Green 배포)
- `scripts/stop.sh`: 기존 애플리케이션 중지
- `scripts/health-check.sh`: 헬스 체크

### 배포 프로세스
1. GitHub에 코드 푸시
2. CodePipeline 자동 트리거
3. CodeBuild에서 Gradle 빌드 실행
4. 빌드 산출물 S3 업로드
5. CodeDeploy가 EC2에 자동 배포
6. 헬스 체크 통과 시 배포 완료

## 트러블슈팅

### WebSocket 연결 실패
- RabbitMQ STOMP 플러그인이 활성화되어 있는지 확인
- 방화벽에서 61613 포트가 열려있는지 확인

### JWT 토큰 검증 실패
- Redis 서버가 실행 중인지 확인
- JWT_SECRET 환경 변수가 올바르게 설정되어 있는지 확인

### 배치 중복 실행 문제
- ShedLock 테이블이 생성되어 있는지 확인
- 데이터베이스 연결 풀 설정 확인

## 향후 개선 계획

- [ ] 매치 리뷰 및 평점 시스템
- [ ] 푸시 알림 (FCM)
- [ ] 결제 시스템 연동 (토스페이먼츠)
- [ ] 매치 통계 및 분석 대시보드
- [ ] 테스트 커버리지 80% 이상 달성

## 라이센스

이 프로젝트는 교육 목적으로 작성되었습니다.

## 팀원

삼성 SDS SCSA 25기 Team 2
- FE: 김동훈 프로
- BE: 나정원, 박예영 프로
- DevOps: 심현우 프로
- QA/QC: 이미르 프로 

---
