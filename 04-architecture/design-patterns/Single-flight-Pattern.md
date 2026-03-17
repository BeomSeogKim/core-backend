---
tags: [architecture, design-pattern, single-flight]
status: completed
created: 2026-03-03
---

# Single-flight Pattern (Request Coalescing)

## 핵심 개념

동일한 요청이 동시에 여러 개 들어오면, 실제 작업은 **딱 한 번만** 실행하고 그 결과를 모든 요청이 공유하는 패턴이다. **Request Coalescing**, **Request Deduplication**이라고도 불린다.

```
요청 A ─┐
요청 B ─┼─→ 실제 작업 1번만 실행 → 결과를 A, B, C 동시 반환
요청 C ─┘
```

> [!note] 락 기반 방식과의 차이
> 락 기반은 요청을 순차 처리하여 지연이 발생하지만, Single-flight는 하나의 Promise를 공유하여 모든 요청이 동시에 결과를 수령한다.

## 동작 원리

### 해결하는 문제

동시에 여러 요청이 동일한 외부 리소스에 접근할 때 불필요한 중복 호출이 발생하고, 외부 서비스에 부하가 집중된다.

```
락 기반 (기존):
요청 1 → 작업 실행 (락 획득)
요청 2 → 대기
요청 3 → 대기
         ↓ 완료
요청 2 → 순차 처리 → 요청 3 → 순차 처리 ...

Single-flight (개선):
요청 1 → Promise 생성 → Map에 저장
요청 2 → Map에서 Promise 발견 → 동일 Promise await
요청 3 → Map에서 Promise 발견 → 동일 Promise await
         ↓ 완료
요청 1, 2, 3 동시에 결과 수령
```

### 주요 사용 사례

| 상황 | 문제 | Single-flight 효과 |
|------|------|------------------|
| [[2-Areas/backend/06-computer-science/cache/캐시|캐시]] 미스 | 동시에 DB 쿼리 폭발 ([[2-Areas/backend/06-computer-science/cache/Cache-Refresh-Ahead|Cache Stampede]]) | 쿼리 1번만 실행 |
| 토큰 만료 | 동시에 토큰 갱신 요청 중복 | 갱신 1번만 실행 |
| 외부 API 호출 | 동일 리소스 중복 호출 | 호출 1번만 실행 |

### 한계

- 단일 인스턴스 내에서만 동작 (멀티 인스턴스 환경에서는 인스턴스 간 Promise 공유 불가)
- 멀티 인스턴스 환경에서는 분산 락(Redis Lock) 또는 외부 캐시 레이어 필요

> [!warning] Map 키 설계
> Map 키 설계가 중요하다. 같은 리소스에 대한 요청을 올바르게 묶어야 하며, `finally`에서 Map 정리가 필수다. 성공/실패 무관하게 제거해야 다음 요청이 새로 시도 가능하다.

## 코드 예시

### 기본 구현 (TypeScript)

```typescript
class TokenService {
    private readonly inflightMap: Map<string, Promise<string>> = new Map();

    async getToken(key: string): Promise<string> {
        // 진행 중인 요청이 있으면 동일 Promise 반환
        const existing = this.inflightMap.get(key);
        if (existing) {
            return existing;
        }

        // 없으면 새 Promise 생성 후 Map에 저장
        const promise = this.fetchToken(key);
        this.inflightMap.set(key, promise);

        try {
            return await promise;
        } finally {
            // 완료 후 반드시 제거
            this.inflightMap.delete(key);
        }
    }

    private async fetchToken(key: string): Promise<string> {
        // 실제 외부 호출
    }
}
```

### 실전 적용 예시 (eBay 토큰 동시성 처리)

```typescript
// 계정(ebayId) 단위로 Promise 공유
// 국가가 아닌 계정 단위인 이유: 같은 eBay 계정이 여러 국가를 담당
const accountKey = cache.tokenInfo.ebayId;

const existingRefreshPromise = this.refreshPromiseMap.get(accountKey);
if (existingRefreshPromise) {
    return existingRefreshPromise; // 동일 Promise 공유
}

const refreshPromise = this.performTokenRefreshWithRetry(countryCode, cache);
this.refreshPromiseMap.set(accountKey, refreshPromise);

try {
    return await refreshPromise;
} finally {
    this.refreshPromiseMap.delete(accountKey);
}
```

> [!tip] 적용 전후 비교
> **적용 전**: 토큰 만료 시 모든 요청이 순차 대기.
> **적용 후**: 첫 요청만 실제 갱신, 나머지는 동일 Promise 대기 후 동시 수령.

## 관련 문서

- [[Exponential-Backoff-Circuit-Breaker]]
- [[2-Areas/backend/06-computer-science/cache/캐시|캐시]]
- [[2-Areas/backend/06-computer-science/cache/Cache-Refresh-Ahead|Cache-Refresh-Ahead]]
