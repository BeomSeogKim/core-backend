---
tags: [computer-science, network, rest, api]
status: completed
created: 2026-04-06
---

# REST API

## 핵심 개념

**Representational State Transfer** — 웹의 장점을 최대한 활용하는 네트워크 아키텍처 스타일. Roy Fielding이 2000년 박사 논문에서 제안했다.

- **자원(Resource)** → URI로 표현: `/users/1`, `/orders/42`
- **행위(Verb)** → HTTP Method로 표현: `GET / POST / PUT / PATCH / DELETE`
- **표현(Representation)** → JSON, XML 등으로 데이터 전달

## 동작 원리

### 6가지 제약 조건

| 제약 조건 | 핵심 |
|---|---|
| **Stateless** | 서버는 클라이언트의 이전 요청을 저장하지 않음. 매 요청에 필요한 정보를 모두 포함 |
| **Client-Server** | UI와 데이터 저장 분리. 각자 독립적으로 발전 가능 |
| **Cacheable** | 응답에 캐시 가능 여부 명시 (`Cache-Control`) |
| **Uniform Interface** | 자원은 URI, 행위는 Method로 일관된 인터페이스 |
| **Layered System** | 클라이언트는 중간 서버(프록시, 게이트웨이) 존재를 알 필요 없음 |
| **Code on Demand** (선택) | 서버가 실행 가능한 코드 전달 (JavaScript 등) |

### Stateless 원칙

```
Stateful (세션 기반):
  요청1: 로그인 → 서버가 세션 저장
  요청2: /orders → 서버가 세션 조회해서 사용자 식별

Stateless (REST):
  요청1: POST /login → JWT 토큰 발급
  요청2: GET /orders + Authorization: Bearer {token} → 토큰에서 직접 사용자 식별
```

서버가 상태를 저장하지 않으므로 **수평 확장(Scale-out)** 이 용이하다. 어떤 서버가 요청을 받아도 토큰만 있으면 처리 가능하다.

### HTTP Method와 의미

| Method | 의미 | 멱등성 | 안전 |
|--------|------|-------|------|
| **GET** | 조회 | O | O |
| **POST** | 생성 | X | X |
| **PUT** | 전체 수정 | O | X |
| **PATCH** | 부분 수정 | X | X |
| **DELETE** | 삭제 | O | X |

> [!note]
> **멱등성** — 같은 요청을 여러 번 보내도 결과가 동일한 성질. PUT은 멱등(같은 데이터로 덮어쓰기), POST는 비멱등(호출마다 새 리소스 생성).

### URI 설계 원칙

```
좋은 예:
  GET    /users          → 사용자 목록 조회
  GET    /users/1        → 특정 사용자 조회
  POST   /users          → 사용자 생성
  PUT    /users/1        → 사용자 전체 수정
  DELETE /users/1        → 사용자 삭제
  GET    /users/1/orders → 특정 사용자의 주문 목록

나쁜 예:
  GET    /getUsers       → 동사 사용 금지
  POST   /users/delete   → Method로 표현
  GET    /user/1         → 복수형 일관성 유지
```

## 코드 예시

```java
@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserCreateRequest request) {
        UserResponse response = userService.create(request);
        URI location = URI.create("/users/" + response.getId());
        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

## 관련 문서

- [[2-Areas/backend/06-computer-science/network/HTTP]]
- [[2-Areas/backend/06-computer-science/network/CORS]]
- [[2-Areas/backend/02-frameworks/Spring/web/Spring-MVC]]
