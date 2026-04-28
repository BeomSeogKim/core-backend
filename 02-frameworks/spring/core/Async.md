---
tags: [spring, async, non-blocking]
status: completed
created: 2026-04-02
---

# @Async — Spring 비동기 처리

## 핵심 개념

**@Async**는 메서드를 별도 스레드에서 비동기적으로 실행하는 Spring 어노테이션이다. 호출자는 메서드 완료를 기다리지 않고 즉시 다음 작업을 진행한다. AOP 프록시 기반으로 동작한다.

## 동작 원리

### 기본 사용

```java
@Configuration
@EnableAsync  // 반드시 필요
public class AsyncConfig { }

@Service
public class NotificationService {

    @Async
    public void sendEmail(String to) {
        // 별도 스레드에서 실행 — 호출자는 바로 리턴
        emailClient.send(to, "제목", "내용");
    }

    @Async
    public CompletableFuture<String> sendEmailWithResult(String to) {
        String result = emailClient.send(to, "제목", "내용");
        return CompletableFuture.completedFuture(result);  // 결과 반환 가능
    }
}
```

```
동기 호출:
[호출 스레드] ──── sendEmail() 실행 ──── 완료 후 다음 작업

@Async 호출:
[호출 스레드] ──── sendEmail() 호출 즉시 리턴 ──── 다음 작업 진행
[별도 스레드] ──── sendEmail() 실행 ────────────── 완료
```

### 스레드 풀 설정

기본 `SimpleAsyncTaskExecutor`는 매번 새 스레드를 생성하므로 실무에서는 **ThreadPoolTaskExecutor**로 커스텀한다.

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }
}
```

### 주의사항

**1. 같은 클래스 내부 호출 시 @Async 미적용**

AOP 프록시 기반이므로 self-invocation에서는 동작하지 않는다. [[AOP]]의 self-invocation 문제와 동일.

```java
@Service
public class OrderService {
    public void process() {
        this.sendNotification();  // 프록시를 거치지 않음 → 동기 실행
    }

    @Async
    public void sendNotification() { ... }
}
```

**2. 반환 타입 제한**

- `void` — 결과 없이 비동기 실행
- `Future` / `CompletableFuture` — 비동기 결과 반환
- 그 외 반환 타입은 무시됨

**3. 예외 처리**

`void` 반환 시 예외가 호출자에게 전파되지 않는다. `AsyncUncaughtExceptionHandler`를 구현하여 처리한다.

## 관련 문서

- [[AOP]] — @Async의 프록시 기반 동작
- [[dev/06-computer-science/os/동기-비동기-블로킹-논블로킹|동기-비동기-블로킹-논블로킹]] — OS 관점의 동기/비동기
- [[IoC-DI]]
