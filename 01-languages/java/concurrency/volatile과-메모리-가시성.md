---
tags: [java, concurrency, volatile, memory-visibility]
status: completed
created: 2026-03-17
---

# volatile과 메모리 가시성

## 핵심 개념

**volatile**은 변수를 CPU 캐시가 아닌 메인 메모리에서 직접 읽고 쓰도록 강제하는 키워드로, 멀티스레드 환경에서 **메모리 가시성(Memory Visibility)** 을 보장한다.

## 메모리 가시성 문제

CPU는 성능을 위해 RAM에 직접 접근하지 않고 **CPU Cache(L1, L2, L3)** 를 통해 값을 읽고 쓴다. 이때 각 CPU의 캐시가 서로 다른 값을 가지면 문제가 생긴다.

```
Thread 1 (CPU 1) → L1 캐시: count = 0
Thread 2 (CPU 2) → L1 캐시: count = 0

Thread 1이 count = 1로 업데이트 (캐시에만 반영)
Thread 2는 여전히 캐시에서 count = 0을 읽음  ← stale value
```

이처럼 한 스레드의 변경이 다른 스레드에게 보이지 않는 문제를 **메모리 가시성 문제**라고 한다.

## volatile 동작 원리

```
volatile 변수 읽기 → CPU 캐시 무시, 메인 메모리에서 직접 읽음
volatile 변수 쓰기 → CPU 캐시 거치지 않고, 즉시 메인 메모리에 반영
```

```
         CPU 1              CPU 2
      ┌─────────┐        ┌─────────┐
      │ L1 Cache│        │ L1 Cache│
      └────┬────┘        └────┬────┘
           │ volatile: 캐시 무시 │
      ┌────▼────────────────▼────┐
      │         메인 메모리        │
      └──────────────────────────┘
```

## volatile의 한계 — 원자성 미보장

```java
volatile int count = 0;
count++;  // 읽기 → 연산 → 쓰기 (3단계, 원자적 연산 아님)
```

`volatile`은 각 단계를 메인 메모리에서 읽고 쓰게 해주지만, 3단계 사이에 다른 스레드가 끼어드는 것은 막지 못한다. Race Condition은 여전히 발생한다.

```
가시성(Visibility) 보장  O
원자성(Atomicity) 보장   X
```

## volatile 단독 사용 조건

**쓰는 스레드가 하나뿐**일 때만 안전하다.

```java
volatile boolean running = true;

// Thread 1 — 읽기만
while (running) {
    // 작업
}

// Thread 2 — 쓰기 (하나뿐)
running = false;
```

`running`을 변경하는 스레드가 Thread 2 하나뿐이므로 Race Condition이 없다. 가시성만 보장하면 충분한 상황이다.

## JVM 내부 구현 — Memory Barrier

`volatile`은 시스템 콜(OS 개입) 없이 **CPU 명령어 레벨**에서 처리된다.

```
Java 소스코드
    ↓ javac
바이트코드 (.class)
    ↓ 인터프리터 (처음 실행 시)     ← volatile 시맨틱 보장
    ↓ JIT 컴파일러 (hotspot 시)    ← Memory Barrier 명령어 삽입
네이티브 코드 (CPU 명령어)
    ↓
CPU 실행 (MFENCE, SFENCE, LFENCE 등)
```

JIT 컴파일러는 자주 호출되는 코드(hotspot)를 네이티브 코드로 변환할 때 `volatile` 접근 지점에 **Memory Barrier** 명령어를 삽입한다.

```
volatile 쓰기 → Store Barrier (SFENCE): 캐시 → 메인 메모리 즉시 flush
volatile 읽기 → Load Barrier  (LFENCE): 캐시 무효화, 메인 메모리에서 직접 읽기
```

처음 실행되는 코드는 인터프리터가 바이트코드를 한 줄씩 해석하며 실행하는데, 이때도 `volatile` 시맨틱은 인터프리터 수준에서 보장된다. JIT는 성능 최적화를 위한 네이티브 변환이며, `volatile`의 의미 자체는 항상 유지된다.

> [!note] 시스템 콜과의 차이
> 시스템 콜은 OS 커널로 전환하는 비용이 있어 오버헤드가 크다.
> Memory Barrier는 CPU 명령어 수준이라 OS를 거치지 않으므로 훨씬 가볍다.

## volatile vs synchronized

| | `volatile` | `synchronized` |
|---|---|---|
| 가시성 | O | O |
| 원자성 | X | O |
| 성능 | 가벼움 | 무거움 (락 획득/반납) |
| 용도 | 단순 플래그, 단일 쓰기 | 복합 연산, 다중 쓰기 |

`synchronized`는 블록 진입/종료 시 메인 메모리 동기화도 함께 수행하므로 가시성도 보장한다.

## 원자성이 필요한 경우

`volatile`로 해결되지 않는 원자성 문제는 `Atomic` 클래스를 사용한다.

```java
AtomicInteger count = new AtomicInteger(0);
count.incrementAndGet();  // 원자적 연산 보장
```

## 관련 문서

- [[모니터]] — Lock과 Wait Set으로 구성된 Java 동기화 메커니즘
- [[synchronized vs Lock]] — Critical Section 보호를 위한 동기화 방식 비교
- [[ThreadPool과 실행 전략]] — 스레드 생성 및 관리 전략
