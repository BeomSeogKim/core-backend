---
tags: [spring, transactional, transaction, propagation]
status: completed
created: 2026-04-02
---

# @Transactional과 전파 수준

## 핵심 개념

**@Transactional**은 선언적 트랜잭션 관리 어노테이션으로, AOP 프록시를 통해 메서드 실행 전후에 트랜잭션을 시작/커밋/롤백한다. **전파 수준(Propagation)**은 이미 트랜잭션이 존재할 때 새 트랜잭션을 어떻게 처리할지 결정한다.

## 동작 원리

### @Transactional 내부 동작

```
Client 호출
    ↓
[프록시 객체]
    ↓ 트랜잭션 시작 (TransactionManager.begin)
[실제 객체 메서드 실행]
    ↓ 정상 → 커밋 / 예외 → 롤백
[프록시 객체]
    ↓
Client에 반환
```

AOP 프록시 기반이므로 **같은 클래스 내부 호출(self-invocation)에는 적용되지 않는다.** [[AOP]] 참고.

### 롤백 규칙

- **Unchecked Exception** (`RuntimeException`) → 기본적으로 **롤백**
- **Checked Exception** → 기본적으로 **롤백 안 함**
- 커스텀 설정 가능: `@Transactional(rollbackFor = Exception.class)`

### 전파 수준 (Propagation)

| 전파 수준 | 기존 트랜잭션 있을 때 | 기존 트랜잭션 없을 때 |
|---|---|---|
| **REQUIRED** (기본) | 참여 | 새로 생성 |
| **REQUIRES_NEW** | 기존 일시 중단 + **새로 생성** | 새로 생성 |
| **SUPPORTS** | 참여 | 트랜잭션 없이 실행 |
| **MANDATORY** | 참여 | **예외 발생** |
| **NOT_SUPPORTED** | 기존 일시 중단 + 트랜잭션 없이 실행 | 트랜잭션 없이 실행 |
| **NEVER** | **예외 발생** | 트랜잭션 없이 실행 |
| **NESTED** | **세이브포인트** 생성 (부분 롤백 가능) | 새로 생성 |

#### REQUIRED vs REQUIRES_NEW

```
REQUIRED (기본):
ServiceA.methodA() ─── 트랜잭션 T1 ───────────────────────────
    └→ ServiceB.methodB()  ── T1에 참여 (같은 트랜잭션) ──
       methodB 예외 → T1 전체 롤백

REQUIRES_NEW:
ServiceA.methodA() ─── 트랜잭션 T1 ─── 일시 중단 ─── T1 재개 ──
    └→ ServiceB.methodB()  ── 새 트랜잭션 T2 ──
       methodB 예외 → T2만 롤백, T1은 영향 없음
```

### 주요 옵션

```java
@Transactional(
    propagation = Propagation.REQUIRED,       // 전파 수준
    isolation = Isolation.READ_COMMITTED,      // 격리 수준
    timeout = 30,                              // 타임아웃 (초)
    readOnly = true,                           // 읽기 전용 (최적화 힌트)
    rollbackFor = Exception.class              // 롤백 대상 예외
)
```

`readOnly = true`는 JPA에서 변경 감지(Dirty Checking)를 생략하여 성능을 최적화한다.

## 관련 문서

- [[AOP]] — @Transactional의 프록시 기반 동작
- [[Bean]] — 트랜잭션 관리 Bean
- [[dev/03-database/트랜잭션|트랜잭션]] — DB 트랜잭션 격리 수준
