# CORS (Cross-Origin Resource Sharing)

정의
> 브라우저가 **다른 Origin으로의 HTTP 요청을 제한**하는 보안 메커니즘
>
> HTTP 헤더 기반으로 동작하며, 서버가 허용할 Origin을 명시

Origin이란?

- `프로토콜 + 도메인 + 포트` 조합
- 예시
  - http://localhost:3000 != http://localhost:8080 (포트가 다름)
  - http://example.com != https://example.com (프로토콜 다름)
  - https://api.example.com != https://example.com (도메인 다름)

핵심 포인트

- **CORS는 브라우저 정책**이다 (서버가 차단하는 것이 아님)
- 서버는 응답을 정상적으로 보내지만, **브라우저가 응답을 버림**
- 서버 -> 서버 통신에는 **CORS가 적용되지 않음** (브라우저가 아니므로)

동작 방식

1. 브라우저가 다른 Origin으로 요청 전송
2. 서버가 응답 + Access-Control-Allow-Origin 헤더 반환
3. 브라우저가 헤더 검사
  - 허용된 Origin이면 -> 응답 사용
  - 아니면 -> 응답 폐기 + CORS 에러

Preflight 요청

- GET, 단순 POST 외의 요청은 **사전 검증(OPTIONS)** 발생
- 브라우저가 먼저 "이 요청 보내도 돼?" 확인
- 서버가 허용하면 실제 요청 전송

주요 Http 헤더

| 헤더                               | 역할                          |
|----------------------------------|-----------------------------|
| Access-Control-Allow-Origin      | 허용할 Origin 지정 (* 혹은 특정 도메인) |
| Access-Control-Allow-Methods     | 허용할 HTTP 메서드                |
| Access-Control-Allow-Headers     | 허용할 요청 헤더                   |
| Access-Control-Allow-Credentials | 쿠키/인증 정보 포함 허용 여부           |

