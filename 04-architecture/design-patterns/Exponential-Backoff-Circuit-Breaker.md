---
tags: [architecture, design-pattern, circuit-breaker]
status: completed
created: 2026-03-03
---

# Exponential Backoff & Circuit Breaker

## 핵심 개념

**Exponential Backoff**는 재시도 간격을 지수적으로 늘려가며 외부 서비스 부하를 줄이는 재시도 전략이다. **Circuit Breaker**는 연속 실패가 임계치를 넘으면 요청 자체를 차단해 외부 서비스 회복 시간을 확보하는 패턴이다. 두 전략을 조합하면 개별 요청 수준의 재시도 제어와 서비스 수준의 장애 전파 차단을 동시에 달성할 수 있다.

> [!note] Backoff + Circuit Breaker 조합
> Exponential Backoff는 **개별 요청 단위**의 재시도 간격을 조절하고, Circuit Breaker는 **서비스/채널 단위**의 누적 실패에 대응한다. 두 전략은 서로 다른 레벨에서 동작하므로 함께 적용해야 완전한 장애 대응이 가능하다.

## 동작 원리

### Exponential Backoff

고정 간격 재시도와 달리, 실패할수록 대기 시간을 지수적으로 증가시킨다.

```
고정 간격 재시도 (기존):
실패 → 1초 대기 → 실패 → 1초 대기 → 실패 → 1초 대기 ...

Exponential Backoff:
실패 → 1초 대기 → 실패 → 2초 대기 → 실패 → 4초 대기 ...
```

공식:

```
delay = base_delay * multiplier^(attempt - 1)

예시 (base: 1000ms, multiplier: 2):
attempt 1 실패 → 1000 * 2^0 = 1,000ms 대기
attempt 2 실패 → 1000 * 2^1 = 2,000ms 대기
attempt 3 실패 → 1000 * 2^2 = 4,000ms 대기
```

### Thundering Herd Problem & Jitter

Exponential Backoff만으로는 여러 클라이언트가 동시에 실패했을 때 동시에 재시도하는 문제가 발생한다.

```
클라이언트 A, B, C 동시 실패
    → 모두 1000ms 후 동시 재시도 → 서버 폭격
    → 또 실패
    → 모두 2000ms 후 동시 재시도 → 반복
```

해결책은 **Jitter**(무작위 지연)를 추가하는 것이다.

> [!tip] Jitter가 불필요한 경우
> - [[Single-flight-Pattern]] 패턴으로 동일 요청이 이미 1개로 합쳐지는 경우
> - 독립적인 처리 단위로 경쟁 조건이 없는 경우

### Circuit Breaker 3가지 상태

```
Closed (정상)
    - 요청 정상 통과
    - 실패 카운트 누적
    ↓ 연속 실패 N회 초과
Open (차단)
    - 모든 요청 즉시 차단 (외부 서비스 호출 없음)
    - 설정된 시간 동안 유지
    ↓ 타임아웃 경과
Half-Open (탐색)
    - 제한적 요청 허용
    - 성공 시 → Closed 복귀
    - 실패 시 → Open 복귀
```

### 세 전략 조합 흐름

```
요청 실패
    ↓
Exponential Backoff로 재시도 (attempt 1, 2, 3)
    ↓ 모두 실패
consecutiveFailures++
    ↓ N회 초과
Circuit Breaker Open → 요청 차단 (Cooldown)
    ↓ Cooldown 만료
Circuit Breaker Closed → 재시도 재개
```

**Jitter 추가 시**: 멀티 인스턴스 환경에서 동시 재시도 분산 효과

### Exponential Backoff vs Circuit Breaker 비교

| | Exponential Backoff | Circuit Breaker |
|--|--------------------|--------------------|
| 동작 시점 | 단일 요청의 재시도 간격 조절 | 누적 실패 시 요청 차단 |
| 역할 | 서버 부하 완화 | 장애 전파 차단, 서버 회복 시간 확보 |
| 적용 범위 | 개별 요청 단위 | 서비스/채널 단위 |

## 코드 예시

### Exponential Backoff (TypeScript)

```typescript
private readonly MAX_RETRY_ATTEMPTS = 3;
private readonly RETRY_DELAY_MS = 1000;
private readonly BACKOFF_MULTIPLIER = 2;

async performWithRetry(): Promise<void> {
    for (let attempt = 1; attempt <= this.MAX_RETRY_ATTEMPTS; attempt++) {
        try {
            await this.doWork();
            return;
        } catch (error) {
            if (attempt < this.MAX_RETRY_ATTEMPTS) {
                const delay = this.RETRY_DELAY_MS * Math.pow(this.BACKOFF_MULTIPLIER, attempt - 1);
                await this.sleep(delay);
            }
        }
    }
    throw new Error('Max retry attempts exceeded');
}
```

### Jitter 적용

```typescript
const baseDelay = this.RETRY_DELAY_MS * Math.pow(this.BACKOFF_MULTIPLIER, attempt - 1);
const jitter = Math.random() * baseDelay;
const delay = baseDelay + jitter;

// 결과: 각 클라이언트가 서로 다른 시점에 재시도
// 클라이언트 A: 1347ms, B: 1823ms, C: 1091ms ...
```

### Circuit Breaker (간략화된 버전)

```typescript
private readonly MAX_CONSECUTIVE_FAILURES = 5;
private readonly FAILURE_COOLDOWN_MS = 5 * 60 * 1000; // 5분

// Closed / Open 상태 판단
private isInCooldown(cache: CountryTokenCache): boolean {
    if (cache.consecutiveFailures < this.MAX_CONSECUTIVE_FAILURES) {
        return false; // Closed
    }
    const cooldownEndTime = cache.lastRefreshAttempt.getTime() + this.FAILURE_COOLDOWN_MS;
    return Date.now() < cooldownEndTime; // Open
}

// 요청 처리 시점
async getAccessToken(): Promise<string> {
    if (this.isInCooldown(cache)) {
        throw new Error('Circuit Open: token refresh in cooldown'); // 즉시 차단
    }
    // 정상 처리 ...
}
```

> [!warning] 단순화된 구현
> 위 Circuit Breaker 구현은 Half-Open 상태 없이 Cooldown 만료 시 자동으로 Closed 복귀하는 단순화된 버전이다. 프로덕션에서는 Half-Open 상태에서 제한적 요청을 허용하여 서비스 회복 여부를 확인하는 것이 안전하다.

## 관련 문서

- [[Single-flight-Pattern]]
- [[dev/06-computer-science/cache/캐시|캐시]]
- [[dev/04-architecture/system-architecture/멀티채널-분산처리|멀티채널-분산처리]]
