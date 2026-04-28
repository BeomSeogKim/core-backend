---
tags: [computer-science, os, thread-safe, cas, atomic]
status: completed
created: 2026-04-13
---

# Thread-safe

## 핵심 개념

**Thread-safe**란 여러 스레드가 동시에 공유 자원에 접근해도 **정확한 결과**가 보장되는 상태를 말한다. Race condition이 발생할 수 있는 코드 영역에서 원자성, 가시성, 순서 보장을 통해 안전한 연산을 수행하는 것이 핵심이다.

## 동작 원리

### Race Condition이 발생하는 이유

`count++`은 한 줄이지만 실제로는 **3단계** 연산이다:

```
1. READ  — count 값을 CPU 레지스터로 읽기
2. MODIFY — 레지스터 값에 1 더하기
3. WRITE  — 결과를 count에 저장
```

이 단계 사이에 **Context Switch**가 끼어들면 두 스레드의 연산 중 하나가 유실된다.

```
Thread-A            Thread-B            count
─────────────────────────────────────────────
READ  (count=0)                          0
                    READ  (count=0)      0
ADD   (0+1=1)                            0
                    ADD   (0+1=1)        0
WRITE (count=1)                          1
                    WRITE (count=1)      1

기대값: 2 → 실제값: 1  ← Race Condition!
```

### Thread-safe를 달성하는 방법

<div style="font-family: -apple-system, sans-serif; max-width: 540px; margin: 12px auto; font-size: 12px;">
<div style="display: flex; gap: 4px; flex-wrap: wrap;">
<div style="flex: 1; min-width: 240px; background: #2980B9; color: white; padding: 12px; border-radius: 6px;">
<div style="font-weight: bold; font-size: 13px;">1. 락 기반 (Blocking)</div>
<div style="font-size: 11px; color: #AED6F1; margin-top: 6px;">synchronized, Lock, Mutex</div>
<div style="font-size: 11px; margin-top: 4px;">임계 영역에 한 스레드만 진입</div>
<div style="font-size: 11px; margin-top: 2px;">경합 시 스레드 블로킹 (대기)</div>
</div>
<div style="flex: 1; min-width: 240px; background: #27AE60; color: white; padding: 12px; border-radius: 6px;">
<div style="font-weight: bold; font-size: 13px;">2. CAS 기반 (Non-blocking)</div>
<div style="font-size: 11px; color: #A9DFBF; margin-top: 6px;">AtomicInteger, AtomicReference</div>
<div style="font-size: 11px; margin-top: 4px;">락 없이 CPU 명령어로 원자적 연산</div>
<div style="font-size: 11px; margin-top: 2px;">경합 시 Spin 재시도</div>
</div>
</div>
<div style="display: flex; gap: 4px; flex-wrap: wrap; margin-top: 4px;">
<div style="flex: 1; min-width: 240px; background: #8E44AD; color: white; padding: 12px; border-radius: 6px;">
<div style="font-weight: bold; font-size: 13px;">3. volatile</div>
<div style="font-size: 11px; color: #D2B4DE; margin-top: 6px;">메모리 가시성만 보장</div>
<div style="font-size: 11px; margin-top: 4px;">CPU 캐시가 아닌 메인 메모리에서 직접 읽기/쓰기</div>
<div style="font-size: 11px; margin-top: 2px;">원자성은 보장하지 않음</div>
</div>
<div style="flex: 1; min-width: 240px; background: #E67E22; color: white; padding: 12px; border-radius: 6px;">
<div style="font-weight: bold; font-size: 13px;">4. 불변 객체 (Immutable)</div>
<div style="font-size: 11px; color: #FDEBD0; margin-top: 6px;">String, Integer 등</div>
<div style="font-size: 11px; margin-top: 4px;">공유 자원 자체를 변경 불가로 설계</div>
<div style="font-size: 11px; margin-top: 2px;">동기화가 필요 없음</div>
</div>
</div>
</div>

### CAS (Compare-And-Swap)

Java의 `AtomicInteger`, `AtomicReference` 등은 내부적으로 **CAS** CPU 명령어를 사용하여 락 없이 원자성을 보장한다.

#### CAS 동작 방식

세 가지 값으로 동작한다:

- **V** — 현재 메모리 값
- **E** — 내가 기대하는 값 (Expected)
- **N** — 새로 쓸 값 (New)

```
if (V == E) → V = N  (성공: 아무도 안 건드렸으니 교체)
else        → 재시도  (실패: 누군가 바꿨으니 다시 읽기)
```

이 비교 + 교체가 **하나의 CPU 명령어**(x86의 `CMPXCHG`)로 원자적으로 실행된다.

#### CAS로 count++ 하기

```
Thread-A: count를 읽음 (E=5)
Thread-A: 새 값 계산 (N=6)
Thread-A: CAS(V, E=5, N=6) → "지금도 5야?" → Yes → count=6 (성공)

Thread-B: count를 읽음 (E=5)
Thread-B: 새 값 계산 (N=6)
Thread-B: CAS(V, E=5, N=6) → "지금도 5야?" → No (이미 6) → 재시도
Thread-B: count를 다시 읽음 (E=6)
Thread-B: 새 값 계산 (N=7)
Thread-B: CAS(V, E=6, N=7) → 성공 → count=7
```

### CAS의 단점

#### 1. Spin (바쁜 대기)

경합이 심한 경우 CAS 실패 → 재시도가 반복되면서 **CPU를 계속 점유**한다. 락 기반은 블로킹되어 CPU를 양보하지만, CAS는 성공할 때까지 루프를 돈다.

```
경합이 심한 상황:

Thread-A: CAS 성공 ✓
Thread-B: CAS 실패 → 재시도 → 실패 → 재시도 → ... → 성공 ✓
Thread-C: CAS 실패 → 재시도 → 실패 → ... (CPU 낭비)
```

> [!warning] 경합 수준에 따른 선택
> 경합이 낮으면 CAS(Lock-free)가 빠르고, 경합이 높으면 락 기반(synchronized/Lock)이 유리하다. CAS의 Spin은 CPU를 소모하기 때문이다.

#### 2. ABA 문제

값이 A → B → A로 변경되었을 때, CAS는 "기대값 A == 현재값 A"이므로 **변경이 없었다고 판단**한다. 실제로는 그 사이에 값이 바뀌었다 돌아온 것이다.

```
Thread-A: READ (V=A, E=A)
                          Thread-B: V를 A → B로 변경
                          Thread-B: V를 B → A로 변경
Thread-A: CAS(V, E=A, N=C) → "지금도 A야?" → Yes → 성공

→ Thread-A는 중간에 B를 거쳤다는 사실을 모름
```

- 단순 정수 연산에서는 문제 없음
- **연결 리스트 노드** 등 참조 기반 자료구조에서 같은 주소가 재활용되면 문제 발생
- Java 해결: **AtomicStampedReference** — 값에 버전(stamp)을 붙여서 A(v1)과 A(v2)를 구별

## 코드 예시

```java
// 1. synchronized (락 기반)
public class Counter {
    private int count = 0;

    public synchronized void increment() {
        count++;  // 한 스레드만 진입
    }
}

// 2. AtomicInteger (CAS 기반)
public class Counter {
    private final AtomicInteger count = new AtomicInteger(0);

    public void increment() {
        count.incrementAndGet();  // 내부적으로 CAS 사용
    }
}

// 3. volatile (가시성만 보장 — count++에는 부적합)
private volatile boolean running = true;  // flag 용도로 적합

// 4. 불변 객체
String s = "hello";
s = s + " world";  // 새 String 객체 생성 (원본 불변)
```

### 방법별 비교

| 방법 | 원자성 | 가시성 | 블로킹 | 적합한 상황 |
|------|--------|--------|--------|------------|
| synchronized / Lock | O | O | O (대기) | 경합 높은 경우, 복합 연산 |
| Atomic (CAS) | O | O | X (Spin) | 경합 낮은 단일 변수 연산 |
| volatile | X | O | X | 플래그, 상태 읽기 전용 |
| 불변 객체 | - | - | X | 공유 데이터 변경 불필요 시 |

## 관련 문서

- [[동기화]]
- [[스핀락-뮤텍스-세마포어]]
- [[프로세스,스레드]]
- [[데드락]]
- [[dev/01-languages/java/concurrency/volatile과-메모리-가시성|volatile과-메모리-가시성]]
