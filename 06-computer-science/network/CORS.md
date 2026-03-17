---
tags: [computer-science, network, cors]
status: completed
created: 2026-02-20
---

# CORS (Cross-Origin Resource Sharing)

## 핵심 개념

브라우저가 **다른 Origin으로의 HTTP 요청을 제한**하는 보안 메커니즘. HTTP 헤더 기반으로 동작하며, 서버가 허용할 Origin을 명시한다.

> [!note]
> **CORS는 브라우저 정책**이다. 서버가 차단하는 것이 아니라, 서버는 응답을 정상적으로 보내지만 **브라우저가 응답을 버린다**. 서버 간 통신에는 CORS가 적용되지 않는다.

**Origin**이란 `프로토콜 + 도메인 + 포트` 조합이다.
- `http://localhost:3000` != `http://localhost:8080` (포트가 다름)
- `http://example.com` != `https://example.com` (프로토콜 다름)
- `https://api.example.com` != `https://example.com` (도메인 다름)

## 동작 원리

### 기본 흐름

```
1. 브라우저가 다른 Origin으로 요청 전송
           ↓
2. 서버가 응답 + Access-Control-Allow-Origin 헤더 반환
           ↓
3. 브라우저가 헤더 검사
   ├── 허용된 Origin → 응답 사용
   └── 허용되지 않음 → 응답 폐기 + CORS 에러
```

### Preflight 요청

GET, 단순 POST 외의 요청은 **사전 검증(OPTIONS)** 이 발생한다. 브라우저가 먼저 "이 요청 보내도 되는가?"를 확인하고, 서버가 허용하면 실제 요청을 전송한다.

> [!warning]
> `Content-Type: application/json`을 사용하는 POST 요청도 단순 요청이 아니므로 **Preflight**가 발생한다. 이는 프론트엔드-백엔드 분리 환경에서 흔히 겪는 이슈이다.

### 주요 HTTP 헤더

| 헤더 | 역할 |
|------|------|
| `Access-Control-Allow-Origin` | 허용할 Origin 지정 (`*` 혹은 특정 도메인) |
| `Access-Control-Allow-Methods` | 허용할 HTTP 메서드 |
| `Access-Control-Allow-Headers` | 허용할 요청 헤더 |
| `Access-Control-Allow-Credentials` | 쿠키/인증 정보 포함 허용 여부 |

> [!tip]
> `Access-Control-Allow-Origin: *`과 `Access-Control-Allow-Credentials: true`는 동시에 사용할 수 없다. 인증 정보를 포함하려면 반드시 특정 Origin을 명시해야 한다.

## 코드 예시

```java
// Spring Boot에서 CORS 설정
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowCredentials(true);
    }
}
```

## 관련 문서
- [[2-Areas/backend/02-frameworks/Next.js/Route|Route]]
- [[SFTP]]
- [[2-Areas/backend/06-computer-science/network/CORS|CORS]]
