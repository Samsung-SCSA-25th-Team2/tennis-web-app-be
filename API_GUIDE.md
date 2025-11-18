# AuthController API 가이드라인 (JWT 기반)

이 문서는 프론트엔드 개발자를 위한 `AuthController`의 API 사용 가이드라인을 제공합니다.
우리 서비스는 **JWT(JSON Web Token) 기반의 토큰 인증 방식**을 사용합니다.

## 1. 인증 흐름 (Authentication Flow)

1.  **로그인 요청**: 사용자가 프론트엔드의 '카카오 로그인' 버튼을 클릭하면, 백엔드의 `/oauth2/authorization/kakao` 경로로 이동합니다.
2.  **소셜 로그인**: 사용자는 카카오 계정으로 로그인을 완료합니다.
3.  **JWT 발급 및 리디렉션**: 백엔드 서버는 로그인이 성공하면 JWT를 생성하여, 프론트엔드의 콜백 URL로 리디렉션시킵니다. 이때 JWT는 쿼리 파라미터에 담겨 전달됩니다.
    -   **리디렉션 예시**: `http://localhost:3000/oauth/callback?token=eyJhbGciOiJIUzI1NiJ9...`
4.  **토큰 저장**: 프론트엔드 애플리케이션은 URL에서 `token` 값을 추출하여 **로컬 스토리지**나 **쿠키**에 안전하게 저장합니다.
5.  **인증된 API 요청**: 이후 모든 API 요청 시, HTTP `Authorization` 헤더에 `Bearer` 스킴과 함께 저장된 JWT를 담아 전송해야 합니다.
    -   **헤더 예시**: `Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...`

---

## 2. API 엔드포인트

### 2.1. 카카오 로그인 시작

-   **URL**: `GET /oauth2/authorization/kakao`
-   **Description**: 카카오 로그인 프로세스를 시작합니다. 프론트엔드에서는 이 URL로 사용자를 이동시키기만 하면 됩니다.
-   **Authentication**: 필요 없음

#### 사용 방법
```html
<!-- 로그인 버튼 예시 -->
<a href="http://localhost:8080/oauth2/authorization/kakao">카카오로 로그인하기</a>
```
-   `http://localhost:8080` 부분은 실제 백엔드 서버 주소로 변경해야 합니다.

---

### 2.2. 로그인 콜백 (프론트엔드 담당)

-   **URL**: `http://localhost:3000/oauth/callback`
-   **Description**: 소셜 로그인 성공 후 백엔드가 리디렉션할 프론트엔드 페이지입니다. 이 페이지에서 URL의 `token` 쿼리 파라미터를 파싱하여 JWT를 저장해야 합니다.
-   **백엔드 설정**: 현재 백엔드는 `http://localhost:3000/oauth/callback`으로 리디렉션하도록 설정되어 있습니다. 프론트엔드 개발 환경에 맞게 이 주소는 변경될 수 있습니다.

#### 구현 예시 (React)
```javascript
import { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

const OAuthCallback = () => {
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    const searchParams = new URLSearchParams(location.search);
    const token = searchParams.get('token');

    if (token) {
      localStorage.setItem('jwt', token); // 로컬 스토리지에 토큰 저장
      // 로그인 성공 후 이동할 페이지로 리디렉션
      navigate('/main'); 
    } else {
      // 토큰이 없는 경우 에러 처리
      navigate('/login-failed');
    }
  }, [location, navigate]);

  return <div>로그인 처리 중...</div>;
};
```

---

### 2.3. 현재 사용자 정보 조회

-   **Endpoint**: `GET /api/user/me`
-   **Description**: 현재 로그인된 사용자의 상세 정보를 조회합니다.
-   **Authentication**: **필수 (JWT Bearer Token)**

#### 요청 (Request)
-   **Headers**:
    ```
    Authorization: Bearer <your_jwt_token>
    ```

#### 응답 (Response)

-   **Status Code**: `200 OK`
-   **Body**:
    ```json
    {
        "userId": 123,
        "provider": "kakao",
        "providerId": "kakao_user_id",
        "name": "홍길동",
        "imageUrl": "http://path.to/profile/image.jpg"
    }
    ```

#### 에러 응답 (Error Response)

-   **Status Code**: `401 Unauthorized` (토큰이 없거나 유효하지 않은 경우)
-   **Body**: (백엔드 설정에 따라 다를 수 있음)

#### 사용 방법
-   API 요청 시 반드시 `Authorization` 헤더에 JWT를 포함해야 합니다.
-   `401` 에러를 수신하면, 저장된 토큰이 만료되었거나 유효하지 않다는 의미이므로 사용자를 다시 로그인 페이지로 안내해야 합니다.

---

### 2.4. 홈 (`/`) 및 로그인 (`/login`) 페이지

-   `GET /` 와 `GET /login` 엔드포인트는 JWT 인증 방식에서는 직접적인 용도가 줄어듭니다.
-   초기 진입점 확인이나 로그인 상태 표시는 프론트엔드에서 로컬 스토리지의 JWT 존재 여부로 1차 판단하고, 정확한 상태는 `/api/user/me` 호출을 통해 확인하는 것을 권장합니다.
