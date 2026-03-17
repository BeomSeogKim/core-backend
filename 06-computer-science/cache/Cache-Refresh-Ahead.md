---
tags: [computer-science, cache, refresh-ahead]
status: completed
created: 2026-03-03
---

# Cache Refresh Ahead

## 핵심 개념

**Refresh Ahead**는 TTL 만료 전에 백그라운드에서 캐시 데이터를 미리 갱신하여, 사용자가 항상 **Cache Hit**을 경험하도록 하는 전략이다. TTL 기반 [[캐시]]의 고질적 문제인 **Cache Stampede**(Thundering Herd) 현상을 근본적으로 방지한다.

## 동작 원리

### 문제: TTL 기반 캐시의 Latency Spike

TTL 만료 순간 다수의 요청이 동시에 **Cache Miss**를 경험하면, 모든 요청이 DB로 몰리는 **Cache Stampede** 현상이 발생한다.

```
TTL 만료 순간:
  요청 1 → cache miss → DB 조회 시작
  요청 2 → cache miss → DB 조회 시작  ← 동시에!
  요청 3 → cache miss → DB 조회 시작  ← 동시에!
  ...N개 요청 → DB에 동시 N개 쿼리 폭탄
  → DB 응답 지연 → 전체 Latency Spike
```

### Cache Stampede 방지 전략 비교

#### A. Mutex Lock (분산 락)

```
miss 발생
  → Redis SETNX로 락 획득 시도
  → 락 획득 성공: DB 조회 → 캐시 갱신 → 락 해제
  → 락 획득 실패: 대기 → 락 해제 후 캐시 재조회
```

- 단점: 락 대기 시간 발생, 락 만료 타이밍 관리 복잡

#### B. Refresh Ahead

```
TTL 만료 전에 백그라운드에서 미리 갱신
  → 사용자는 항상 캐시 hit
  → Latency Spike 없음
```

#### C. TTL Jitter

```
TTL = 기본값 + random(0, 30초)
  → 같은 시점에 대량 만료 방지
  → 부하 분산 (완전 해결은 아님, 확률적 완화)
```

> [!note] 전략 조합
> 실무에서는 단일 전략만 사용하기보다, Refresh Ahead + TTL Jitter를 결합하거나, Refresh Ahead에 Mutex Lock을 함께 적용하여 안정성을 높인다.

### Refresh Ahead 구현 흐름

```
요청 도착
  ├─ 캐시 없음 → DB 조회 → 캐시 저장 → 반환
  └─ 캐시 있음
       ├─ TTL > 1분 → 즉시 반환
       └─ TTL < 1분
            ├─ 분산 락 획득 성공 → 백그라운드 갱신 시작 → 즉시 반환
            └─ 분산 락 획득 실패 → 즉시 반환 (다른 인스턴스가 갱신 중)
```

### 왜 분산 락이 필요한가

TTL < 1분인 상태에서 동시 요청 100개가 들어오면:
- 100개 모두 TTL < 1분 감지
- 100개 모두 백그라운드 갱신 시도 → 미니 Stampede 재발

분산 락으로 하나만 갱신하도록 보장한다. 이는 [[2-Areas/backend/04-architecture/design-patterns/Single-flight-Pattern|Single-flight-Pattern]]과 유사한 개념이다.

### Refresh Ahead vs 일반 Cache Miss에서의 락 차이

| | 일반 Cache Miss + 락 | Refresh Ahead + 락 |
|---|---|---|
| 캐시 상태 | 없음 | 있음 (만료 임박) |
| 락 실패 시 | 대기 필수 (응답할 데이터 없음) | 즉시 반환 (캐시 데이터 있음) |
| 복잡도 | 높음 | 낮음 |

> [!tip] Refresh Ahead의 핵심 이점
> Refresh Ahead에서 락 획득에 실패하더라도 기존 캐시 데이터가 남아있으므로 즉시 반환이 가능하다. 일반 Cache Miss + 락 방식에서는 락 실패 시 대기해야 하므로 응답 지연이 불가피하다.

## 코드 예시

### 전체 구현 코드

```kotlin
// applicationScope Bean 등록
@Configuration
class CoroutineScopeConfig {
    @Bean
    fun applicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // SupervisorJob: 자식 하나 실패해도 스코프 자체는 살아있음
    // Dispatchers.IO: 네트워크/DB I/O 작업용
}

@Component
class ExhibitionService(
    private val redis: RedisTemplate<String, Exhibition>,
    private val repository: ExhibitionRepository,
    private val applicationScope: CoroutineScope
) {
    suspend fun getExhibition(id: Long): Exhibition {
        val key = "exhibition:$id"
        val cached = redis.opsForValue().get(key)

        if (cached != null) {
            val ttl = redis.getExpire(key, TimeUnit.SECONDS)

            if (ttl < 60) {
                val lockKey = "refresh:lock:$id"
                val acquired = redis.opsForValue()
                    .setIfAbsent(lockKey, "1", 30, TimeUnit.SECONDS)

                if (acquired == true) {
                    applicationScope.launch {  // 요청 스코프와 분리된 백그라운드 실행
                        try {
                            val fresh = repository.findById(id)
                            redis.opsForValue().set(key, fresh, 10, TimeUnit.MINUTES)
                        } finally {
                            redis.delete(lockKey)
                        }
                    }
                }
                // 락 실패 → 아무것도 안 함
            }

            return cached  // 항상 즉시 반환
        }

        // 캐시 없을 때만 동기 조회
        val fresh = repository.findById(id)
        redis.opsForValue().set(key, fresh, 10, TimeUnit.MINUTES)
        return fresh
    }
}
```

### SupervisorJob vs Job

applicationScope에서 `Job()` 대신 `SupervisorJob()`을 써야 하는 이유:

```
Job() 사용 시:
  전시 A 갱신 실패 → 예외가 Job으로 전파
  → applicationScope 전체 취소
  → 이후 모든 백그라운드 갱신 불가 (서버 재시작 전까지)

SupervisorJob() 사용 시:
  전시 A 갱신 실패 → 해당 코루틴만 실패
  → applicationScope 살아있음
  → 전시 B, C, D 갱신은 계속 정상 동작
```

앱 전체 생명주기를 가진 스코프에는 항상 `SupervisorJob()`을 사용한다.

> [!warning] coroutineScope를 사용하면 안 되는 이유
> `coroutineScope`는 자식 코루틴이 모두 완료될 때까지 대기하므로, 백그라운드 갱신이 끝날 때까지 응답이 차단된다. 반드시 요청 스코프와 분리된 `applicationScope`를 사용해야 즉시 반환이 가능하다.

```kotlin
// 잘못된 구현 — coroutineScope는 자식 완료를 대기
suspend fun getExhibition(id: Long): Exhibition {
    val cached = redis.get(key)
    coroutineScope {
        launch { backgroundRefresh() }
    }  // ← backgroundRefresh() 완료 후에야 여기 도달
    return cached  // 갱신 완료 후 반환 → 의미 없음
}

// 올바른 구현 — applicationScope로 독립 실행
suspend fun getExhibition(id: Long): Exhibition {
    val cached = redis.get(key)
    applicationScope.launch { backgroundRefresh() }  // 독립적 백그라운드 실행
    return cached  // 즉시 반환
}
```

[[2-Areas/backend/01-languages/kotlin/코루틴-Dispatcher|코루틴-Dispatcher]] 문서에서 Dispatchers.IO의 동작 방식을 확인할 수 있다.

### 전략 선택 기준

| 상황 | 추천 전략 |
|---|---|
| 단순 조회, 갱신 빈도 낮음 | TTL Jitter |
| Cache miss 자체를 막아야 함 | Refresh Ahead |
| miss 시 DB 동시 요청을 막아야 함 | Mutex Lock |
| 실시간성이 중요하지 않은 데이터 | Refresh Ahead + 긴 TTL |
| 실시간성이 중요한 데이터 | Write-Through 또는 캐시 미사용 |

## 관련 문서

- [[캐시]] - 캐시의 기본 개념과 읽기/쓰기 패턴
- [[로컬캐시_리모트캐시]] - 로컬 캐시와 리모트 캐시 비교
- [[2-Areas/backend/04-architecture/design-patterns/Single-flight-Pattern|Single-flight-Pattern]] - 동일 요청 중복 실행 방지 패턴
- [[2-Areas/backend/01-languages/kotlin/코루틴-Dispatcher|코루틴-Dispatcher]] - Kotlin 코루틴 Dispatcher 동작 원리
