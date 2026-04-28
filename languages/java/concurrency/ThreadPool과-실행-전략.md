---
tags: [java, concurrency, threadpool, executorservice, threadpoolexecutor]
status: completed
created: 2026-03-17
---

# ThreadPool과 실행 전략

## 핵심 개념

**ThreadPool**은 미리 생성해둔 스레드들을 Pool에 저장해두고, 작업 요청 시 꺼내 사용하고 완료 후 반납하는 재사용 메커니즘이다.

## 왜 ThreadPool이 필요한가

### 문제 1 — 스레드 생성 비용

Java의 Platform Thread는 OS Thread를 직접 생성하는 방식이라 생성 비용이 비싸다. 요청마다 새 스레드를 만들면 불필요한 오버헤드가 발생한다.

### 문제 2 — 스레드 수 미제어

스레드 수를 제한하지 않으면 트래픽이 몰릴 때 스레드가 무한정 생성되어 메모리가 고갈되고 서버가 죽을 수 있다(OOM).

```
ThreadPool이 해결하는 것:
  1. 스레드 생성/삭제 비용 절감  → 재사용
  2. 스레드 수 제한              → 시스템 자원 보호
```

## ExecutorService — ThreadPool 추상화

`ExecutorService`는 ThreadPool을 추상화하여 제공하는 인터페이스다. `Executors` 팩토리 메서드로 생성한다.

### Executors 팩토리 메서드

| 메서드 | 스레드 수 | 적합한 상황 |
|---|---|---|
| `newFixedThreadPool(n)` | 고정 (n개) | 예측 가능한 트래픽, 자원 사용량 통제 |
| `newCachedThreadPool()` | 무제한 | 단기 작업, 불규칙 트래픽 |
| `newSingleThreadExecutor()` | 1개 | 순서 보장이 필요한 작업 |

### newCachedThreadPool 동작

```
요청 도착
  → 놀고 있는 스레드 있음? → 재사용
  → 없음?                  → 새로 생성
  → 작업 완료 후 60초 대기 → 새 요청 오면 재사용, 없으면 제거
```

스레드 수 상한이 없으므로 트래픽이 지속적으로 높으면 OOM 위험이 있다.

### newSingleThreadExecutor 특징

스레드가 1개이므로 작업이 **순서대로(FIFO)** 처리된다. 로그 처리, 이벤트 큐 등 순서 보장이 필요한 작업에 적합하다.

## ThreadPoolExecutor — 세밀한 제어

`Executors` 팩토리 메서드는 내부적으로 `ThreadPoolExecutor`를 사용한다. 직접 생성하면 더 세밀하게 제어할 수 있다.

```java
new ThreadPoolExecutor(
    corePoolSize,      // 기본 유지 스레드 수
    maximumPoolSize,   // 최대 스레드 수
    keepAliveTime,     // 초과 스레드 유지 시간
    timeUnit,
    workQueue,         // 작업 대기 큐
    handler            // 초과 시 거부 전략
);
```

### 작업 처리 흐름

```
작업 도착
  │
  ├─ corePoolSize 미만     → 새 스레드 생성
  │
  ├─ corePoolSize 초과     → workQueue에 적재
  │
  ├─ workQueue 가득 참     → maximumPoolSize까지 스레드 생성
  │
  └─ maximumPoolSize 초과  → RejectedExecutionHandler 실행
```

## RejectedExecutionHandler — 거부 전략

`maximumPoolSize`도 초과하면 아래 전략 중 하나로 처리한다.

| 전략 | 동작 |
|---|---|
| `AbortPolicy` | 예외 던짐 (기본값) |
| `DiscardPolicy` | 새 작업 조용히 버림 |
| `DiscardOldestPolicy` | 큐에서 가장 오래된 작업 버리고 새 작업 적재 |
| `CallerRunsPolicy` | 요청한 스레드가 직접 실행 |

### CallerRunsPolicy — Back Pressure 효과

```
요청 스레드가 직접 작업 실행
  → 새 요청을 받는 속도가 자연스럽게 느려짐
  → 시스템 과부하를 자연스럽게 조절

장점: 작업 유실 없음, 자연스러운 속도 조절 (Back Pressure)
단점: 요청 스레드 블로킹 → 연쇄 지연 가능
```

## 코드 예시

```java
ExecutorService pool = new ThreadPoolExecutor(
    10,                          // corePoolSize
    50,                          // maximumPoolSize
    60L, TimeUnit.SECONDS,       // keepAliveTime
    new LinkedBlockingQueue<>(100), // workQueue (최대 100개)
    new ThreadPoolExecutor.CallerRunsPolicy() // 거부 전략
);

pool.submit(() -> {
    // 작업
});

pool.shutdown(); // 새 작업 접수 중단, 기존 작업 완료 후 종료
```

## 관련 문서

- [[모니터]] — Lock과 Wait Set으로 구성된 Java 동기화 메커니즘
- [[synchronized vs Lock]] — Critical Section 보호를 위한 동기화 방식 비교
- [[volatile과 메모리 가시성]] — CPU 캐시와 메모리 가시성 문제
