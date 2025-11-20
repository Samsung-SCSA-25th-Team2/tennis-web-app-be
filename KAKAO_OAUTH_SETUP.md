# 카카오 소셜 로그인 설정 가이드

## 1. 카카오 개발자 센터 설정

### 1.1 카카오 개발자 계정 생성 및 애플리케이션 등록
1. [카카오 개발자 센터](https://developers.kakao.com/) 접속
2. 로그인 후 "내 애플리케이션" 메뉴 선택
3. "애플리케이션 추가하기" 클릭
4. 앱 이름, 사업자명 등 기본 정보 입력

### 1.2 앱 키 확인
1. 생성한 애플리케이션 선택
2. "앱 설정" > "앱 키" 메뉴에서 다음 키 확인:
   - **REST API 키**: `client-id`로 사용
   - **Client Secret**: 보안 강화를 위해 활성화 필요

### 1.3 Client Secret 활성화
1. "제품 설정" > "카카오 로그인" > "보안" 메뉴 선택
2. "Client Secret" 항목에서 "코드 생성" 클릭
3. 생성된 코드를 복사 (이 값이 `client-secret`)
4. 상태를 "사용함"으로 변경

### 1.4 Redirect URI 설정
1. "제품 설정" > "카카오 로그인" 메뉴 선택
2. "활성화 설정"을 ON으로 변경
3. "Redirect URI" 항목에서 "Redirect URI 등록" 클릭
4. 다음 URI들을 등록:
   ```
   http://localhost:8080/login/oauth2/code/kakao
   https://your-domain.com/login/oauth2/code/kakao  (배포 시)
   ```

### 1.5 동의 항목 설정
1. "제품 설정" > "카카오 로그인" > "동의항목" 메뉴 선택
2. 다음 항목들을 설정:
   - **프로필 정보 (닉네임/프로필 사진)**: 필수 동의 또는 선택 동의
   - **카카오계정 (이메일)**: 선택 동의 (필요시)

## 2. 애플리케이션 설정

### 2.1 환경 변수 설정

#### 방법 1: 환경 변수로 설정 (권장)
```bash
# Linux/Mac
export KAKAO_CLIENT_ID=your-rest-api-key
export KAKAO_CLIENT_SECRET=your-client-secret

# Windows (PowerShell)
$env:KAKAO_CLIENT_ID="your-rest-api-key"
$env:KAKAO_CLIENT_SECRET="your-client-secret"

# Windows (CMD)
set KAKAO_CLIENT_ID=your-rest-api-key
set KAKAO_CLIENT_SECRET=your-client-secret
```

#### 방법 2: application.yml 직접 수정
`src/main/resources/application.yml` 파일에서 다음 부분을 수정:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          kakao:
            client-id: your-rest-api-key  # 여기에 REST API 키 입력
            client-secret: your-client-secret  # 여기에 Client Secret 입력
```

⚠️ **주의**: 방법 2를 사용할 경우, 보안을 위해 해당 파일을 Git에 커밋하지 마세요!

#### 방법 3: .env 파일 사용 (선택사항)
프로젝트 루트에 `.env` 파일 생성:

```
KAKAO_CLIENT_ID=your-rest-api-key
KAKAO_CLIENT_SECRET=your-client-secret
```

## 3. 애플리케이션 실행

### 3.1 의존성 설치 및 빌드
```bash
./gradlew clean build
```

### 3.2 애플리케이션 실행
```bash
./gradlew bootRun
```

## 4. 테스트

### 4.1 카카오 로그인 테스트
1. 브라우저에서 `http://localhost:8080` 접속
2. 카카오 로그인 버튼 클릭
3. 카카오 계정으로 로그인
4. 동의 항목 확인 후 "동의하고 계속하기" 클릭
5. 로그인 성공 시 홈 페이지로 리다이렉트

### 4.2 로그 확인
애플리케이션 콘솔에서 다음과 같은 로그 확인:
```
OAuth2 Login - Provider: kakao
OAuth2 User Info - Provider: kakao, ProviderId: 123456789, Name: 홍길동
OAuth2 로그인 성공 - UserId: 1, Provider: kakao, ProviderId: 123456789
```

## 5. 구현된 기능

### 5.1 현재 구현된 기능
- ✅ 카카오 OAuth2 로그인
- ✅ 사용자 정보 자동 저장 (닉네임, 프로필 이미지)
- ✅ 중복 닉네임 자동 처리
- ✅ 기존 사용자 정보 업데이트

### 5.2 추가 구현이 필요한 기능
- ⏳ JWT 토큰 발급 및 인증
- ⏳ 추가 정보 입력 페이지 (성별, 나이대, 테니스 경력)
- ⏳ 프론트엔드 연동
- ⏳ 로그아웃 처리

## 6. API 엔드포인트

| 엔드포인트 | 메서드 | 설명 |
|----------|--------|------|
| `/oauth2/authorization/kakao` | GET | 카카오 로그인 페이지로 리다이렉트 |
| `/login/oauth2/code/kakao` | GET | 카카오 로그인 콜백 (자동 처리) |
| `/logout` | POST | 로그아웃 |

## 7. 주요 클래스 구조

```
src/main/java/com/example/scsa/
├── auth/
│   ├── dto/
│   │   ├── OAuth2UserInfo.java          # OAuth2 사용자 정보 인터페이스
│   │   ├── KakaoOAuth2UserInfo.java     # 카카오 사용자 정보 구현체
│   │   └── CustomOAuth2User.java        # 커스텀 OAuth2 사용자
│   ├── service/
│   │   └── CustomOAuth2UserService.java # OAuth2 사용자 서비스
│   └── handler/
│       ├── OAuth2LoginSuccessHandler.java  # 로그인 성공 핸들러
│       └── OAuth2LoginFailureHandler.java  # 로그인 실패 핸들러
├── config/
│   └── SecurityConfig.java              # Spring Security 설정
└── domain/
    └── entity/
        └── User.java                    # 사용자 엔티티
```

## 8. 문제 해결

### 8.1 "Client authentication failed" 오류
- Client Secret이 올바르게 설정되었는지 확인
- 카카오 개발자 센터에서 Client Secret 상태가 "사용함"인지 확인

### 8.2 "Redirect URI mismatch" 오류
- 카카오 개발자 센터에 등록한 Redirect URI와 application.yml의 redirect-uri가 일치하는지 확인
- 포트 번호까지 정확히 일치해야 함

### 8.3 "Invalid scope" 오류
- 카카오 개발자 센터의 동의항목에서 요청한 scope가 활성화되어 있는지 확인

## 9. 보안 주의사항

⚠️ **절대 Git에 커밋하지 말아야 할 정보**:
- `KAKAO_CLIENT_ID`
- `KAKAO_CLIENT_SECRET`
- 데이터베이스 비밀번호

💡 `.gitignore`에 다음 항목 추가 권장:
```
.env
application-secret.yml
```

## 10. 참고 자료

- [카카오 로그인 공식 문서](https://developers.kakao.com/docs/latest/ko/kakaologin/common)
- [Spring Security OAuth2 공식 문서](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html)