---
tags: [computer-science, network, http]
status: completed
created: 2026-04-21
---

# Forward vs Redirect

## 핵심 개념

**Forward** — 서버 내부에서 요청을 다른 컴포넌트로 위임. 브라우저는 인지하지 못한다.

**Redirect** — 서버가 브라우저에게 새 URL로 재요청하도록 지시. 브라우저가 두 번째 요청을 보낸다.

## 동작 원리

### Forward

```
브라우저                   서버
   |                        |
   |--- GET /old-page ----→ |
   |                        |--- (서버 내부 위임) ---→ /new-page 처리
   |                        |
   |←--- 200 OK ----------- |   (브라우저는 /old-page로 알고 있음)
   |
  주소창: /old-page (변화 없음)
```

- HTTP 요청 1회
- URL 주소창 변화 없음
- 서버 내부 리소스(Servlet, JSP, Controller)만 접근 가능
- 요청/응답 객체(Request, Response)를 그대로 공유

### Redirect

```
브라우저                   서버
   |                        |
   |--- GET /old-page ----→ |
   |←--- 301/302 + Location: /new-page --- |
   |                        |
   |--- GET /new-page ----→ |   (두 번째 요청 자동 발송)
   |←--- 200 OK ----------- |
   |
  주소창: /new-page (변경됨)
```

- HTTP 요청 2회
- URL 주소창이 새 URL로 변경
- 외부 URL로도 이동 가능
- 새 요청이므로 Request 객체는 초기화됨

### 301 vs 302

| | 301 Moved Permanently | 302 Found (Moved Temporarily) |
|---|---|---|
| 의미 | URL 영구 변경 | 임시 이동 |
| 브라우저 캐싱 | **캐싱 O** (다음 요청 서버 미경유) | 캐싱 X (매번 서버 요청) |
| 사용 사례 | 도메인 변경, URL 구조 개편 | 로그인 후 리다이렉트, A/B 테스트 |

## PRG 패턴 (Post-Redirect-Get)

POST 요청에 Forward를 쓰면 뒤로 가기/새로고침 시 폼이 재제출된다. 이를 방지하기 위한 패턴.

```
브라우저           서버
   |                |
   |-- POST /order → |   (주문 처리)
   |← 302 /order/complete |
   |                |
   |-- GET /order/complete → |   (결과 조회)
   |← 200 OK -------|
   |
  새로고침해도 GET 요청만 재실행 → 중복 주문 없음
```

## Forward vs Redirect 비교

| | Forward | Redirect |
|---|---|---|
| 처리 위치 | 서버 내부 | 브라우저 |
| URL 주소창 | 변화 없음 | 새 URL로 변경 |
| HTTP 요청 횟수 | 1회 | 2회 |
| Request 객체 | 공유 | 초기화 |
| 외부 URL 이동 | 불가 | 가능 |
| POST 폼 재제출 위험 | **있음** | 없음 (PRG 패턴) |

## 코드 예시

```java
// Spring MVC — Redirect
@PostMapping("/order")
public String order(OrderRequest request) {
    orderService.place(request);
    return "redirect:/order/complete";   // 302 Redirect
}

// Spring MVC — Forward
@GetMapping("/internal")
public String forward() {
    return "forward:/other-page";        // 서버 내부 Forward
}
```

```java
// HttpServletResponse 직접 제어
response.sendRedirect("/new-url");                      // 302 Redirect
request.getRequestDispatcher("/new-url").forward(request, response);  // Forward
```

## 관련 문서

- [[dev/06-computer-science/network/HTTP]]
- [[dev/06-computer-science/network/TCP-UDP]]
- [[dev/02-frameworks/spring/web/DispatcherServlet]]
