# Single-flight Pattern (Request Coalescing)

## 개념

동일한 요청이 동시에 여러 개 들어오면, 실제 작업은 **딱 한 번만** 실행하고 그 결과를 모든 요청이 공유하는 패턴.

```
요청 A ─┐
요청 B ─┼─→ 실제 작업 1번만 실행 → 결과를 A, B, C 동시 반환
요청 C ─┘
```

**별칭**: Request Coalescing, Request Deduplication

---

## 해결하는 문제

동시에 여러 요청이 동일한 외부 리소스에 접근할 때:
- 불필요한 중복 호출 발생
- 외부 서비스에 부하 집중
- 락 기반 방식은 순차 처리로 인한 지연 발생

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

---

## TypeScript 구현

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

**핵심 포인트**
- `finally`에서 Map 정리 필수 → 성공/실패 무관하게 제거해야 다음 요청이 새로 시도 가능
- Map 키 설계가 중요 → 같은 리소스에 대한 요청을 올바르게 묶어야 함

---

## 주요 사용 사례

| 상황 | 문제 | Single-flight 효과 |
|------|------|------------------|
| 캐시 미스 | 동시에 DB 쿼리 폭발 (Cache Stampede) | 쿼리 1번만 실행 |
| 토큰 만료 | 동시에 토큰 갱신 요청 중복 | 갱신 1번만 실행 |
| 외부 API 호출 | 동일 리소스 중복 호출 | 호출 1번만 실행 |

---

## 실전 적용 예시 (eBay 토큰 동시성 처리)

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

**적용 전**: 토큰 만료 시 모든 요청이 순차 대기
**적용 후**: 첫 요청만 실제 갱신, 나머지는 동일 Promise 대기 후 동시 수령

---

## 한계

- 단일 인스턴스 내에서만 동작 (멀티 인스턴스 환경에서는 인스턴스 간 Promise 공유 불가)
- 멀티 인스턴스 환경에서는 분산 락(Redis Lock) 또는 외부 캐시 레이어 필요
