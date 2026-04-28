---
tags: [spring, filter, interceptor, servlet]
status: completed
created: 2026-04-02
---

# Filter vs Interceptor

## 핵심 개념

**Filter**는 서블릿 스펙의 컴포넌트로 Spring 컨텍스트 밖(Tomcat)에서 동작하고, **Interceptor**는 Spring MVC의 컴포넌트로 Spring 컨텍스트 안에서 동작한다. 요청 처리 파이프라인에서의 위치와 접근 가능한 정보가 다르다.

## 동작 원리

### 요청 처리 흐름

```
HTTP 요청
    ↓
┌── Tomcat (서블릿 컨테이너) ──┐
│  [Filter 1] → [Filter 2]    │
└──────────────────────────────┘
    ↓
┌── Spring MVC ────────────────────────────────────┐
│  DispatcherServlet                                │
│      ↓                                           │
│  [Interceptor.preHandle]                          │
│      ↓                                           │
│  [Controller 실행]                                │
│      ↓                                           │
│  [Interceptor.postHandle]                         │
│      ↓                                           │
│  [View 렌더링]                                    │
│      ↓                                           │
│  [Interceptor.afterCompletion]                    │
└──────────────────────────────────────────────────┘
    ↓
HTTP 응답 (Filter를 역순으로 통과)
```

### 비교

| | Filter | Interceptor |
|---|---|---|
| 스펙 | **서블릿** (javax.servlet) | **Spring MVC** (HandlerInterceptor) |
| 동작 위치 | DispatcherServlet **밖** | DispatcherServlet **안** |
| Spring Bean 주입 | Spring Boot에서는 가능 | **가능** |
| Controller 정보 | 없음 | **HandlerMethod** 접근 가능 |
| 적용 범위 | 모든 요청 (정적 리소스 포함) | Spring MVC 요청만 |

### Interceptor 메서드

| 메서드 | 시점 | 용도 |
|---|---|---|
| `preHandle` | Controller 실행 **전** | 인증/인가, 요청 검증 (false 반환 시 요청 차단) |
| `postHandle` | Controller 실행 **후**, View 렌더링 전 | 모델 데이터 가공 |
| `afterCompletion` | View 렌더링 **후** | 리소스 정리, 로깅 |

### 용도별 선택 가이드

| 용도 | Filter | Interceptor |
|---|---|---|
| 인코딩 (CharacterEncoding) | **O** | |
| XSS / CORS 필터링 | **O** | |
| Spring Security (인증/인가) | **O** (Filter 체인 기반) | |
| 비즈니스 로깅 | | **O** |
| 권한 체크 (Controller 정보 필요) | | **O** |
| API 응답 시간 측정 | | **O** |

## 코드 예시

```java
// Filter 구현
@Component
public class LogFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        log.info("요청 시작");
        chain.doFilter(req, res);  // 다음 필터 또는 서블릿으로 전달
        log.info("응답 완료");
    }
}

// Interceptor 구현
@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod hm = (HandlerMethod) handler;
            // Controller 메서드 정보 접근 가능
        }
        return true;  // false 반환 시 요청 차단
    }
}
```

## 관련 문서

- [[AOP]] — Interceptor와 AOP의 관계
- [[Spring-Boot-동작원리]] — DispatcherServlet 등록
