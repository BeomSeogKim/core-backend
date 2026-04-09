---
tags: [java, jvm, gc, garbage-collection]
status: completed
created: 2026-03-30
---

# GC (Garbage Collection)

## 핵심 개념

**GC(Garbage Collection)**는 JVM이 Heap 영역에서 더 이상 참조되지 않는 객체를 자동으로 수거하는 메모리 관리 메커니즘이다. 개발자가 명시적으로 메모리를 해제하지 않아도 된다.

## 동작 원리

### GC 대상 판별 — Reachability

GC는 **GC Root**에서 참조 체인을 따라 도달할 수 없는 객체를 수거 대상으로 판별한다.

```
GC Root → 객체 A → 객체 B → 객체 C    (Reachable → 수거 안 함)
          객체 D → 객체 E              (Unreachable → 수거 대상)
```

**GC Root가 될 수 있는 것:**
- JVM Stack의 지역변수/매개변수가 참조하는 객체
- Method Area의 static 변수가 참조하는 객체
- JNI에 의해 생성된 객체
- 실행 중인 스레드

### Heap 구조 — Generational GC

대부분의 객체는 생명주기가 짧다는 **약한 세대 가설(Weak Generational Hypothesis)**에 기반하여, Heap을 세대별로 나누어 관리한다.

```
┌──────────────────────────────────┬─────────────────────┐
│         Young Generation          │   Old Generation     │
│                                  │                     │
│  ┌────────┬─────────┬─────────┐  │                     │
│  │  Eden  │Survivor0│Survivor1│  │   오래 살아남은 객체   │
│  │        │  (S0)   │  (S1)   │  │   (Age > 임계치)     │
│  │새 객체  │         │         │  │                     │
│  │ 할당   │  S0↔S1 번갈아 사용  │  │                     │
│  └────────┴─────────┴─────────┘  │                     │
│                                  │                     │
│  ← Minor GC (자주, 빠름) →       │← Major/Full GC      │
│                                  │   (드물게, 느림) →   │
└──────────────────────────────────┴─────────────────────┘
```

### Minor GC 동작 과정

```
1단계: 새 객체 → Eden에 할당

2단계: Eden이 가득 참 → Minor GC 발생
       - Eden의 살아남은 객체 → S0으로 이동 (Age = 1)
       - Eden 비움

3단계: 다시 Eden이 가득 참 → Minor GC 발생
       - Eden의 살아남은 객체 → S1으로 이동
       - S0의 살아남은 객체 → S1으로 이동 (Age 증가)
       - Eden + S0 비움

4단계: 반복. Minor GC마다 S0 ↔ S1 번갈아 사용
       - 항상 하나의 Survivor는 비어 있음

5단계: Age가 임계치(기본 15) 초과 → Old Generation으로 승격 (Promotion)
```

### Major GC / Full GC

Old Generation이 가득 차면 **Major GC(Full GC)**가 발생한다.

- **전체 Heap을 대상**으로 수거 → Minor GC보다 훨씬 느림
- **Stop-The-World (STW)** — GC 동안 모든 애플리케이션 스레드 정지
- Full GC의 STW 시간을 최소화하는 것이 GC 튜닝의 핵심 목표

### GC 알고리즘

#### Serial GC

```
[App 스레드 정지] → [GC 스레드 1개가 순차 처리] → [App 재개]
```

- **싱글 스레드**로 GC 수행
- 단순하고 오버헤드 적음
- 소규모 애플리케이션, 싱글 코어 환경에 적합
- `-XX:+UseSerialGC`

#### Parallel GC

```
[App 스레드 정지] → [GC 스레드 N개가 병렬 처리] → [App 재개]
```

- **멀티 스레드**로 GC 수행 → Serial보다 빠름
- Java 8 기본 GC
- STW 시간은 여전히 존재
- `-XX:+UseParallelGC`

#### G1 GC (Garbage First)

```
기존 GC:                    G1 GC:
┌────────┬──────┐          ┌──┬──┬──┬──┬──┐
│ Young  │ Old  │          │E │S │O │E │H │
│        │      │          ├──┼──┼──┼──┼──┤
│ 연속된  │영역   │          │O │E │  │O │S │
└────────┴──────┘          ├──┼──┼──┼──┼──┤
                           │  │O │E │  │O │
                           └──┴──┴──┴──┴──┘
                           E=Eden S=Survivor O=Old H=Humongous
                           (Region 단위로 동적 할당)
```

- **Java 9+ 기본 GC**
- Heap을 동일 크기의 **Region**(1~32MB)으로 분할
- Young/Old가 고정 영역이 아닌 **Region 단위로 동적** 할당
- 가비지가 가장 많은 Region부터 우선 수거 → **Garbage First**
- **목표 Pause Time** 설정 가능 (`-XX:MaxGCPauseMillis=200`, 기본 200ms)
- 설정된 시간 내에서 최대한 많은 Region을 수거
- 큰 객체는 **Humongous Region**에 할당

#### ZGC

- **거의 모든 GC 작업을 애플리케이션과 동시(Concurrent) 수행**
- STW가 수 ms 이하로 매우 짧음 (Heap 크기와 무관)
- 대용량 Heap(수 TB)에 적합
- Java 15+ 프로덕션 사용 가능
- `-XX:+UseZGC`

### GC 알고리즘 비교

| | Serial | Parallel | G1 | ZGC |
|---|---|---|---|---|
| GC 스레드 | 1개 | N개 | N개 | N개 (동시 수행) |
| STW | 김 | 중간 | 목표 시간 내 | **수 ms 이하** |
| Heap 분할 | Young/Old 고정 | Young/Old 고정 | **Region 동적** | Region + 컬러 포인터 |
| 기본 GC | — | Java 8 | **Java 9+** | — |
| 적합 환경 | 소규모/싱글코어 | 배치 처리 | 범용 서버 | 대용량/저지연 |

## 관련 문서

- [[JVM]] — JVM 전체 구조
- [[2-Areas/backend/06-computer-science/os/가상-메모리|가상-메모리]] — JVM Heap이 올라가는 OS 가상 메모리
- [[2-Areas/backend/06-computer-science/os/프로세스,스레드|프로세스,스레드]] — GC 스레드와 애플리케이션 스레드
