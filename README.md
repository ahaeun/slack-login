# Social Login Service (Slack · Google · Naver)

Spring Security OAuth2로 여러 소셜 공급자 로그인을 통합하고, 로그인 이후에는 자체 발급한 **JWT(Access/Refresh)** 로 무상태(stateless) 세션을 유지하는 Spring Boot 웹 애플리케이션입니다.

외부 인증(OAuth2)과 내부 세션 관리(JWT)를 분리·결합하는 인증 아키텍처 설계에 초점을 둔 프로젝트입니다.

## ✨ 주요 기능

- **소셜 로그인 통합**: Google·Slack(OIDC), Naver(OAuth2)
- **자체 JWT 발급**: 로그인 성공 시 Access/Refresh 토큰을 발급해 **HttpOnly 쿠키**로 전달
- **무상태 인증**: 매 요청마다 쿠키의 JWT를 검증해 인증 (세션 미사용)
- **토큰 자동 재발급**: Access 만료 시 Refresh로 재발급(rotation)
- **설정 외부화**: 시크릿을 `.env`로 분리, CI/CD에서 환경별 주입

## 🛠 기술 스택

| 구분 | 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0, Spring Security, Spring Web MVC |
| View | Thymeleaf (SSR) |
| Auth | Spring Security OAuth2 Client, JWT (jjwt 0.12) |
| Build | Gradle |
| CI/CD | GitHub Actions → SSH/SCP 배포 |

## 🏗 인증 아키텍처

```
[로그인 버튼] → /oauth2/authorization/{google|slack|naver}
     │  Spring Security 가 공급자 인가 페이지로 리다이렉트
     ▼
공급자 로그인 → /login/oauth2/code/{registrationId}  (콜백, Security 가 토큰교환·사용자조회)
     │
     ▼
OAuth2LoginSuccessHandler
     │  공급자 사용자정보 → LoginUser 매핑 → JWT(Access/Refresh) 발급 → HttpOnly 쿠키
     ▼
/home  ──(이후 모든 요청)──▶ JwtAuthenticationFilter 가 쿠키 JWT 검증 → SecurityContext 인증
     │
     └─ Access 만료 → (entry point) /oauth/refresh → Refresh 검증 후 재발급 → /home
```

**역할 분담**

| 컴포넌트 | 역할 |
|---|---|
| `SecurityConfig` | 인가 규칙, oauth2Login 등록, 필터 체인 구성 |
| `OAuth2LoginSuccessHandler` | OAuth 성공 → 공급자별 사용자정보 매핑 → JWT 쿠키 발급 |
| `JwtAuthenticationFilter` | 매 요청 JWT 검증 → `SecurityContext`에 인증 주입 |
| `JwtTokenProvider` | JWT 생성·검증 (서명·만료·타입) |
| `AuthController` | 토큰 재발급(`/oauth/refresh`), 로그아웃(`/logout`) |
| `AuthCookieFactory` | HttpOnly·SameSite 쿠키 생성 |

> 공급자마다 사용자 응답 구조가 다릅니다. Google·Slack은 OIDC 표준 클레임(`sub`/`name`/`email`/`picture`), Naver는 `response` 객체로 중첩(`id`/`name`/`email`/`profile_image`)되어, `registrationId` 기준으로 분기 매핑합니다.

## 📂 패키지 구조

```
com.example.slack
├── SlackApplication.java          # 진입점
├── LoginController.java           # 로그인 화면(/, /login)
├── HomeController.java            # 홈 화면(/home)
├── config
│   └── SecurityConfig.java        # Spring Security 설정
└── auth
    ├── AuthController.java        # /oauth/refresh, /logout
    ├── jwt/JwtTokenProvider.java  # JWT 발급·검증
    ├── model/LoginUser.java       # 로그인 사용자(record)
    └── web
        ├── AuthCookieFactory.java
        ├── JwtAuthenticationFilter.java
        └── OAuth2LoginSuccessHandler.java
```

## 🚀 실행 방법

### 1. 사전 준비
각 공급자 콘솔에서 OAuth 앱을 등록하고 **Redirect/Callback URL**을 추가합니다.

| 공급자 | Redirect URI |
|---|---|
| Google | `http://localhost:8080/login/oauth2/code/google` |
| Slack | `http://localhost:8080/login/oauth2/code/slack` |
| Naver | `http://localhost:8080/login/oauth2/code/naver` |

### 2. 환경 변수(`.env`)
프로젝트 루트에 `.env`를 만들고 값을 채웁니다. (`.env.example` 참고)

```dotenv
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
SLACK_CLIENT_ID=...
SLACK_CLIENT_SECRET=...
NAVER_CLIENT_ID=...
NAVER_CLIENT_SECRET=...
JWT_SECRET=...   # HS256: 32바이트 이상
```

> `.env`는 `spring.config.import=optional:file:.env[.properties]`로 로드됩니다. OS 환경변수가 있으면 그쪽이 우선합니다.

### 3. 실행

```bash
./gradlew bootRun
# http://localhost:8080
```

## 🔑 주요 엔드포인트

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/`, `/login` | 로그인 화면 |
| GET | `/oauth2/authorization/{provider}` | 소셜 로그인 시작 (Spring Security) |
| GET | `/login/oauth2/code/{provider}` | OAuth 콜백 (Spring Security) |
| GET | `/home` | 홈(인증 필요) |
| GET | `/oauth/refresh` | Access 토큰 재발급 |
| GET | `/logout` | 로그아웃(쿠키 만료) |

## 🔒 보안 설계

- **HttpOnly 쿠키**: JS 접근 차단으로 XSS 토큰 탈취 방지
- **SameSite=Lax**: CSRF 완화
- **stateless**: 인증을 세션에 저장하지 않음(`NullSecurityContextRepository`)
- **시크릿 분리**: 클라이언트/JWT 시크릿을 `.env`로 외부화, 빌드 산출물(jar)과 분리

> Bearer(헤더) 방식과 HttpOnly 쿠키 방식은 각각 XSS·CSRF에 대한 트레이드오프가 있습니다. SSR 구조에 맞춰 쿠키 방식을 채택했으며, 운영에서는 CSRF 토큰 적용·HTTPS(`Secure` 쿠키)가 권장됩니다.

## ⚙️ CI/CD

`main` 브랜치 push 시 GitHub Actions가 다음을 수행합니다.
1. Gradle 빌드(`ROOT.jar`)
2. 서버로 jar 전송(SCP)
3. GitHub Secrets로 서버 `.env` 생성·전송
4. 무중단성 재기동 스크립트 실행

## 📌 향후 개선

- Refresh 토큰 서버 저장(DB/Redis)으로 강제 로그아웃·재사용 탐지
- CSRF 토큰 적용 및 운영 HTTPS `Secure` 쿠키 적용
- 사용자 영속화(가입/프로필) 및 권한(Role) 확장
