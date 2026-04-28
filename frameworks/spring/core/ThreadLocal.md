---
tags: [spring, threadlocal, transaction, security, concurrency]
status: completed
created: 2026-04-24
---

# ThreadLocal

## 핵심 개념

**ThreadLocal**은 스레드마다 독립적인 변수 저장 공간을 제공하는 Java 클래스다. 같은 ThreadLocal 객체라도 스레드가 다르면 완전히 별개의 값을 가진다.

```
Thread-1 ──→ ThreadLocal.get() ──→ "UserA" (Thread-1 전용)
Thread-2 ──→ ThreadLocal.get() ──→ "UserB" (Thread-2 전용)
Thread-3 ──→ ThreadLocal.get() ──→ null    (Thread-3 전용)
```

## 동작 원리

각 Thread 객체 내부에 `ThreadLocalMap`이 있고, ThreadLocal 객체를 키로 값을 저장한다.

```java
// 내부 구조 (개념적)
class Thread {
    ThreadLocal.ThreadLocalMap threadLocals;  // 스레드별 맵
}

// ThreadLocal.get() 동작
public T get() {
    Thread t = Thread.currentThread();
    ThreadLocalMap map = t.threadLocals;     // 현재 스레드의 맵에서
    return map.get(this);                    // this(ThreadLocal 객체)를 키로 조회
}
```

## Spring에서의 활용

### 1. @Transactional — TransactionSynchronizationManager

`@Transactional`이 같은 트랜잭션 안에서 여러 Repository가 동일한 DB Connection을 공유하는 것은 ThreadLocal 덕분이다.

```
@Transactional 시작
    ↓
HikariCP에서 Connection 획득
    ↓
TransactionSynchronizationManager(ThreadLocal)에 Connection 저장
    ↓
Repository A 호출 ──→ ThreadLocal에서 Connection 꺼냄 (같은 커넥션)
Repository B 호출 ──→ ThreadLocal에서 Connection 꺼냄 (같은 커넥션)
    ↓
@Transactional 종료 ──→ commit/rollback ──→ Connection 반납 + ThreadLocal.remove()
```

```java
// TransactionSynchronizationManager 내부 (개략)
private static final ThreadLocal<Map<Object, Object>> resources =
    new NamedThreadLocal<>("Transactional resources");
```

### 2. Spring Security — SecurityContextHolder

인증 정보를 요청 처리 스레드에 바인딩한다.

```java
// 인증 필터에서 저장
SecurityContextHolder.getContext().setAuthentication(authentication);

// 컨트롤러/서비스에서 조회
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();

// 요청 완료 후 반드시 클리어
SecurityContextHolder.clearContext();  // ← 내부적으로 ThreadLocal.remove()
```

기본 전략은 `MODE_THREADLOCAL`. `MODE_INHERITABLETHREADLOCAL`로 변경하면 자식 스레드에도 인증 정보가 전파된다.

## 주의사항: 스레드 풀 오염

웹 서버는 스레드 풀에서 스레드를 재사용한다. ThreadLocal을 `remove()` 없이 두면 이전 요청의 데이터가 다음 요청에 남아있다.

```
요청 1 (User A)
    → ThreadLocal에 "UserA" 저장
    → 처리 완료 (remove() 안 함)

요청 2 (User B)
    → 같은 스레드 재사용
    → ThreadLocal.get() == "UserA"  ← 데이터 오염 ❌
```

**반드시 finally에서 remove()**:

```java
try {
    threadLocal.set(value);
    // 비즈니스 로직
} finally {
    threadLocal.remove();  // 필수
}
```

Spring의 `@Transactional`과 `SecurityContextHolder`는 내부적으로 요청 완료 후 `remove()`를 호출하도록 구현되어 있다.

## 관련 문서

- [[Transactional]]
- [[dev/06-computer-science/os/Thread-safe]]
- [[dev/06-computer-science/os/프로세스,스레드]]
