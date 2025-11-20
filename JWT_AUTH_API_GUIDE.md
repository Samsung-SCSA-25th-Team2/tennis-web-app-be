# JWT 인증 API 가이드

## 목차
- [개요](#개요)
- [인증 방식](#인증-방식)
- [API 엔드포인트](#api-엔드포인트)
- [요청/응답 예시](#요청응답-예시)
- [에러 처리](#에러-처리)
- [보안 고려사항](#보안-고려사항)

---

## 개요

이 애플리케이션은 **JWT (JSON Web Token) 기반 인증**을 사용합니다.
- OAuth2를 통한 카카오 소셜 로그인 지원
- JWT 토큰은 `localStorage`에 저장
- 모든 보호된 API 요청 시 `Authorization: Bearer <token>` 헤더 필요

---

## 인증 방식

### 1. 로그인 플로우

```
1. 카카오 로그인 버튼 클릭
   ↓
2. GET /oauth2/authorization/kakao
   → 카카오 인증 페이지로 리다이렉트
   ↓
3. 사용자 인증 완료
   ↓
4. 콜백: GET /login/oauth2/code/kakao?code=...
   → 서버에서 사용자 정보 가져오기 및 JWT 생성
   ↓
5. 리다이렉트: /index.html?token=<JWT>
   ↓
6. 프론트엔드: URL에서 토큰 추출 → localStorage 저장
   localStorage.setItem('jwt_token', token)
```

### 2. 인증된 API 호출

모든 보호된 API 요청에 JWT 토큰을 헤더에 포함:

```javascript
fetch('/api/auth/me', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('jwt_token')}`,
    'Content-Type': 'application/json'
  }
})
```

### 3. 로그아웃 플로우

```
1. POST /api/auth/logout
   ↓
2. localStorage에서 토큰 삭제
   localStorage.removeItem('jwt_token')
   ↓
3. 로그인 페이지로 이동
```

---

## API 엔드포인트

### 1. 인증 상태 조회

현재 사용자의 인증 상태를 확인합니다.

**Endpoint:** `GET /api/auth/status`

**인증 필요:** ❌ (public)

**Headers:**
```
Authorization: Bearer <JWT> (선택사항)
```

**Response (인증된 경우):**
```json
{
  "authenticated": true,
  "userId": 1,
  "provider": "kakao",
  "providerId": "123456789",
  "name": "홍길동",
  "imageUrl": "https://k.kakaocdn.net/...",
  "message": "로그인 성공!",
  "loginUrl": null
}
```

**Response (미인증된 경우):**
```json
{
  "authenticated": false,
  "userId": null,
  "provider": null,
  "providerId": null,
  "name": null,
  "imageUrl": null,
  "message": "로그인이 필요합니다.",
  "loginUrl": "/oauth2/authorization/kakao"
}
```

---

### 2. 사용자 정보 조회

현재 로그인한 사용자의 상세 정보를 조회합니다.

**Endpoint:** `GET /api/auth/me`

**인증 필요:** ✅ (JWT 필수)

**Headers:**
```
Authorization: Bearer <JWT>
Content-Type: application/json
```

**Response (성공):** HTTP 200
```json
{
  "userId": 1,
  "provider": "kakao",
  "providerId": "123456789",
  "name": "홍길동",
  "imageUrl": "https://k.kakaocdn.net/...",
  "nickname": "테니스왕"
}
```

**Response (인증 실패):** HTTP 401
```json
{
  "error": "인증되지 않은 사용자입니다.",
  "errorCode": "UNAUTHORIZED"
}
```

**Response (사용자 없음):** HTTP 404
```json
{
  "error": "사용자를 찾을 수 없습니다.",
  "errorCode": "USER_NOT_FOUND"
}
```

---

### 3. 로그아웃

현재 사용자를 로그아웃합니다.

**Endpoint:** `POST /api/auth/logout`

**인증 필요:** ❌ (public, 하지만 토큰이 있으면 포함 권장)

**Headers:**
```
Authorization: Bearer <JWT> (선택사항)
Content-Type: application/json
```

**Response (성공):** HTTP 200
```json
{
  "success": true,
  "message": "로그아웃되었습니다. 클라이언트에서 토큰을 삭제하세요."
}
```

**Response (실패):** HTTP 500
```json
{
  "success": false,
  "message": "로그아웃 처리 중 오류가 발생했습니다: ..."
}
```

**중요:** 서버 로그아웃 후 **반드시 클라이언트에서 토큰 삭제** 필요:
```javascript
localStorage.removeItem('jwt_token');
```

---

## 요청/응답 예시

### JavaScript (Fetch API)

#### 1. 인증 상태 확인
```javascript
async function checkAuthStatus() {
  const token = localStorage.getItem('jwt_token');

  const response = await fetch('/api/auth/status', {
    method: 'GET',
    headers: {
      'Authorization': token ? `Bearer ${token}` : '',
      'Content-Type': 'application/json'
    }
  });

  const data = await response.json();
  console.log('인증 상태:', data);
  return data;
}
```

#### 2. 사용자 정보 조회
```javascript
async function getUserInfo() {
  const token = localStorage.getItem('jwt_token');

  if (!token) {
    throw new Error('토큰이 없습니다. 로그인이 필요합니다.');
  }

  const response = await fetch('/api/auth/me', {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error);
  }

  const data = await response.json();
  console.log('사용자 정보:', data);
  return data;
}
```

#### 3. 로그아웃
```javascript
async function logout() {
  const token = localStorage.getItem('jwt_token');

  try {
    const response = await fetch('/api/auth/logout', {
      method: 'POST',
      headers: {
        'Authorization': token ? `Bearer ${token}` : '',
        'Content-Type': 'application/json'
      }
    });

    const data = await response.json();
    console.log('로그아웃 결과:', data);
  } finally {
    // 서버 요청 성공 여부와 관계없이 토큰 삭제
    localStorage.removeItem('jwt_token');
    window.location.href = '/';
  }
}
```

### cURL

#### 1. 인증 상태 확인
```bash
curl -X GET http://localhost:8080/api/auth/status \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### 2. 사용자 정보 조회
```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json"
```

#### 3. 로그아웃
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json"
```

---

## 에러 처리

### HTTP 상태 코드

| 상태 코드 | 설명 | 발생 상황 |
|---------|------|---------|
| 200 | OK | 요청 성공 |
| 401 | Unauthorized | 인증 실패 (토큰 없음, 만료, 유효하지 않음) |
| 404 | Not Found | 사용자를 찾을 수 없음 |
| 500 | Internal Server Error | 서버 내부 오류 |

### 에러 응답 형식

```json
{
  "error": "에러 메시지",
  "errorCode": "ERROR_CODE"
}
```

### 에러 코드

| 코드 | 설명 |
|-----|------|
| `UNAUTHORIZED` | 인증되지 않은 사용자 |
| `USER_NOT_FOUND` | 사용자를 찾을 수 없음 |

### 에러 처리 예시

```javascript
async function handleApiCall() {
  try {
    const response = await fetch('/api/auth/me', {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('jwt_token')}`
      }
    });

    if (!response.ok) {
      const error = await response.json();

      switch (response.status) {
        case 401:
          // 인증 실패 - 로그인 페이지로 리다이렉트
          localStorage.removeItem('jwt_token');
          window.location.href = '/';
          break;
        case 404:
          // 사용자 없음
          console.error('사용자를 찾을 수 없습니다.');
          break;
        default:
          console.error('에러:', error.error);
      }
      return;
    }

    const data = await response.json();
    console.log('성공:', data);
  } catch (error) {
    console.error('네트워크 에러:', error);
  }
}
```

---

## 보안 고려사항

### 1. XSS (Cross-Site Scripting) 방어

**문제점:**
- JWT를 localStorage에 저장하면 JavaScript로 접근 가능
- XSS 공격으로 토큰 탈취 위험

**대응 방안:**
```javascript
// ✅ 사용자 입력 sanitization
function sanitizeInput(input) {
  const div = document.createElement('div');
  div.textContent = input;
  return div.innerHTML;
}

// ✅ CSP (Content Security Policy) 설정
// application.yml 또는 SecurityConfig에 추가
```

### 2. CSRF (Cross-Site Request Forgery) 방어

**장점:**
- Authorization 헤더 방식은 자동으로 CSRF 방어
- 쿠키와 달리 자동으로 전송되지 않음

### 3. 토큰 만료 처리

```javascript
// JWT 만료 확인
function isTokenExpired(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    const expiresAt = new Date(payload.exp * 1000);
    return expiresAt < new Date();
  } catch (e) {
    return true;
  }
}

// 만료된 토큰 자동 삭제
function checkTokenExpiration() {
  const token = localStorage.getItem('jwt_token');
  if (token && isTokenExpired(token)) {
    localStorage.removeItem('jwt_token');
    window.location.href = '/';
  }
}
```

### 4. HTTPS 사용 (운영 환경)

운영 환경에서는 **반드시 HTTPS** 사용:
- JWT 토큰이 평문으로 전송되므로 암호화 필수
- 중간자 공격(MITM) 방지

### 5. 토큰 저장 위치 비교

| 저장 위치 | XSS 취약 | CSRF 취약 | 장점 | 단점 |
|---------|---------|----------|------|------|
| localStorage | ✅ 높음 | ❌ 없음 | 간편한 사용 | XSS 공격에 취약 |
| HTTP-only Cookie | ❌ 없음 | ✅ 있음 | XSS 방어 | CSRF 대응 필요 |
| sessionStorage | ✅ 높음 | ❌ 없음 | 탭 닫으면 삭제 | XSS 공격에 취약 |

**현재 구현:** localStorage (편의성 우선)

**권장사항:**
- XSS 방어를 위한 CSP 설정
- 신뢰할 수 있는 라이브러리만 사용
- 정기적인 보안 업데이트

---

## JWT 토큰 구조

JWT는 3부분으로 구성됩니다:

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwicm9sZSI6IlJPTEVfVVNFUiIsImlhdCI6MTcwMDAwMDAwMCwiZXhwIjoxNzAwMDg2NDAwfQ.signature
│                                    │                                                                                              │
Header (알고리즘)                      Payload (데이터)                                                                               Signature (서명)
```

### Payload 예시

```json
{
  "sub": "1",              // 사용자 ID
  "role": "ROLE_USER",     // 권한
  "iat": 1700000000,       // 발급 시간
  "exp": 1700086400        // 만료 시간
}
```

### 토큰 디코딩 (클라이언트)

```javascript
function decodeJWT(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return {
      userId: payload.sub,
      role: payload.role,
      issuedAt: new Date(payload.iat * 1000),
      expiresAt: new Date(payload.exp * 1000)
    };
  } catch (e) {
    console.error('JWT 디코딩 실패:', e);
    return null;
  }
}
```

---

## 트러블슈팅

### 1. 401 Unauthorized 에러

**원인:**
- 토큰이 없음
- 토큰이 만료됨
- 토큰이 유효하지 않음

**해결 방법:**
```javascript
// 토큰 확인
const token = localStorage.getItem('jwt_token');
console.log('토큰:', token);

// 토큰 디코딩하여 만료 확인
if (token) {
  const decoded = decodeJWT(token);
  console.log('만료 시간:', decoded.expiresAt);
}

// 재로그인
localStorage.removeItem('jwt_token');
window.location.href = '/oauth2/authorization/kakao';
```

### 2. CORS 에러

**원인:**
- 다른 도메인에서 API 호출

**해결 방법:**
```java
// SecurityConfig.java 또는 CorsConfig.java
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

### 3. 네트워크 에러

```javascript
// 재시도 로직
async function fetchWithRetry(url, options, retries = 3) {
  for (let i = 0; i < retries; i++) {
    try {
      return await fetch(url, options);
    } catch (error) {
      if (i === retries - 1) throw error;
      await new Promise(resolve => setTimeout(resolve, 1000 * (i + 1)));
    }
  }
}
```

---

## 참고 자료

- [JWT 공식 사이트](https://jwt.io/)
- [OAuth 2.0 명세](https://oauth.net/2/)
- [Spring Security 문서](https://docs.spring.io/spring-security/reference/)
- [Kakao Login API](https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api)

---

**문서 버전:** 1.0
**최종 수정일:** 2025-01-17
**작성자:** Tennis Match Platform Team