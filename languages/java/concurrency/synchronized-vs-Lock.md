---
tags: [java, concurrency, synchronized, lock, reentrantlock]
status: completed
created: 2026-03-17
---

# synchronized vs Lock

## 핵심 개념

Java의 동기화 방식은 크게 두 가지다. JVM이 자동으로 관리하는 **`synchronized`** 와 개발자가 명시적으로 제어하는 **`Lock` 인터페이스(`ReentrantLock`)**.

## synchronized의 한계

```
1. 타임아웃 불가  — 락을 얻을 때까지 무한정 블로킹
2. 즉시 시도 불가 — 락 획득 가능 여부를 확인할 방법 없음
3. 인터럽트 불가  — 대기 중인 스레드를 깨울 수 없음
4. 공정성 미보장  — 어떤 스레드가 락을 얻을지 JVM이 임의 결정
```

이 한계를 해결하기 위해 `Lock` 인터페이스가 등장했다.

## 비교

| | `synchronized` | `ReentrantLock` |
|---|---|---|
| 락 관리 | JVM 자동 | 개발자 직접 |
| 타임아웃 | 불가 | `tryLock(3, SECONDS)` |
| 즉시 시도 | 불가 | `tryLock()` |
| 인터럽트 | 불가 | `lockInterruptibly()` |
| 공정성 | 미보장 | `new ReentrantLock(true)` |
| 코드 간결성 | 높음 | 낮음 (finally 필수) |

## 동작 원리

### synchronized — JVM 자동 관리

```java
public synchronized void increment() {
    count++;
    // 블록 종료 시 JVM이 자동으로 락 반납
}
```

### ReentrantLock — 명시적 관리

```java
Lock lock = new ReentrantLock();

lock.lock();
try {
    // 작업
} finally {
    lock.unlock();  // 예외가 터져도 반드시 반납
}
```

`finally`에 `unlock()`을 넣지 않으면 예외 발생 시 락이 영구적으로 반납되지 않아 Deadlock으로 이어진다.

## 코드 예시

### tryLock — 타임아웃

```java
Lock lock = new ReentrantLock();

if (lock.tryLock(3, TimeUnit.SECONDS)) {
    try {
        // 작업
    } finally {
        lock.unlock();
    }
} else {
    // 3초 내 락 획득 실패 → 다른 처리
}
```

### tryLock — 즉시 시도

```java
if (lock.tryLock()) {
    try {
        // 락 획득 성공
    } finally {
        lock.unlock();
    }
} else {
    // 락 획득 실패 → 즉시 반환
}
```

### 공정성 (Fairness)

```java
// fair  — 대기 순서(FIFO) 보장, 성능 약간 저하
Lock fairLock = new ReentrantLock(true);

// unfair — 순서 미보장, 성능 좋음 (기본값)
Lock unfairLock = new ReentrantLock(false);
```

## 재진입 (Reentrant)

`synchronized`와 `ReentrantLock` 모두 **같은 스레드의 재진입을 허용**한다.

```java
public synchronized void methodA() {
    methodB();  // 같은 스레드 → 블로킹 없이 진입 가능
}

public synchronized void methodB() {
    // 락을 이미 보유한 스레드이므로 통과
}
```

재진입이 없다면 `methodA`에서 락을 잡은 채 `methodB`를 호출할 때 Deadlock이 발생한다.
내부적으로 **획득 횟수를 카운팅**하여 관리한다.

## 선택 기준

```
단순한 임계 구역 보호
  → synchronized
  → 코드 간결, 실수 가능성 낮음

타임아웃 / 즉시 시도 / 공정성 / 다중 Condition 필요
  → ReentrantLock
  → 복잡한 동시성 로직, 세밀한 제어 필요
```

## 관련 문서

- [[모니터]] — Lock과 Wait Set으로 구성된 Java 동기화 메커니즘
- [[volatile과 메모리 가시성]] — 락 없이 가시성만 보장하는 방법
- [[ThreadPool과 실행 전략]] — 스레드 생성 및 관리 전략
