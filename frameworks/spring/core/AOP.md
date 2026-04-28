---
tags: [spring, aop, proxy, cglib]
status: completed
created: 2026-04-02
---

# AOP (Aspect-Oriented Programming)

## 핵심 개념

**AOP(관점 지향 프로그래밍)**는 로깅, 트랜잭션, 보안 등 **횡단 관심사(Cross-cutting Concern)**를 비즈니스 로직에서 분리하는 프로그래밍 패러다임이다. Spring AOP는 **프록시 패턴** 기반으로 동작한다.

## 동작 원리

### AOP가 없으면

```java
public class OrderService {
    public void createOrder(Order order) {
        long start = System.currentTimeMillis();  // 시간 측정
        log.info("createOrder 시작");              // 로깅
        // --- 비즈니스 로직 ---
        orderRepository.save(order);
        // --- 비즈니스 로직 끝 ---
        long end = System.currentTimeMillis();    // 시간 측정
        log.info("소요 시간: {}ms", end - start);  // 로깅
    }
}
// 모든 메서드에 동일한 코드 반복 → 횡단 관심사와 비즈니스 로직이 뒤섞임
```

### AOP 적용

```java
@Aspect
@Component
public class PerformanceAspect {

    @Around("execution(* com.example.service.*.*(..))")
    public Object measureTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();  // 실제 메서드 실행
        long end = System.currentTimeMillis();
        log.info("{} 소요 시간: {}ms", joinPoint.getSignature(), end - start);
        return result;
    }
}
```

### AOP 핵심 용어

| 용어 | 설명 |
|------|------|
| **Aspect** | 횡단 관심사를 모듈화한 것 (`@Aspect` 클래스) |
| **Advice** | 실제 부가 기능 (Before, After, Around 등) |
| **Join Point** | Advice가 적용될 수 있는 지점 (Spring AOP는 메서드 실행만) |
| **Pointcut** | Join Point를 선택하는 표현식 (`execution(* com.example..*.*(..))`) |
| **Target** | AOP가 적용되는 실제 객체 |
| **Proxy** | Target을 감싼 프록시 객체 |

### 프록시 동작 방식

Bean 생성 시 AOP 대상이면 **프록시 객체로 감싸서** 컨테이너에 등록한다.

```
Client → [프록시 객체] → [Advice 실행 (Before)] → [실제 객체 메서드] → [Advice 실행 (After)] → 반환
```

### JDK Dynamic Proxy vs CGLIB

| | JDK Dynamic Proxy | CGLIB |
|---|---|---|
| 조건 | **인터페이스 구현** 필요 | 인터페이스 없어도 가능 |
| 방식 | 인터페이스 기반 프록시 생성 | **클래스 상속**으로 서브클래스 생성 |
| 제약 | 인터페이스 없는 클래스 불가 | `final` 클래스/메서드 불가 |
| Spring Boot 기본 | | **CGLIB** (더 범용적) |

```
JDK Dynamic Proxy:
  Client → Proxy (interface 구현) → 실제 객체 (같은 interface 구현)

CGLIB:
  Client → Proxy (실제 클래스 extends) → 실제 객체 (부모 클래스)
```

### Spring AOP vs AspectJ

| | Spring AOP | AspectJ |
|---|---|---|
| 위빙 시점 | **런타임** (프록시) | **컴파일/로드 타임** (바이트코드 조작) |
| 적용 대상 | **Spring Bean만** | 모든 Java 객체 |
| Join Point | **메서드 실행**만 | 메서드, 필드, 생성자 등 |
| 설정 | 간단 (`@Aspect`) | 별도 컴파일러 필요 |
| 성능 | 프록시 오버헤드 | 빠름 (컴파일 시 적용) |

실무에서는 대부분 Spring AOP로 충분하다.

### Self-invocation 문제

프록시 기반이므로 **같은 클래스 내부에서 메서드를 호출하면 AOP가 적용되지 않는다.** 프록시를 거치지 않고 `this`로 직접 호출하기 때문이다.

```java
@Service
public class OrderService {

    @Transactional
    public void methodA() {
        this.methodB();  // 프록시를 거치지 않음 → methodB의 AOP 미적용
    }

    @Transactional(propagation = REQUIRES_NEW)
    public void methodB() { ... }
}
```

## 관련 문서

- [[Transactional]]
- [[IoC-DI]]
- [[Bean]]
- [[Filter-vs-Interceptor]]
