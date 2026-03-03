# Cache Refresh Ahead

> 관련 문서:
> - [로컬캐시 & 리모트캐시](./로컬캐시_리모트캐시.md)
> - [캐시 기초](./캐시.md)
> - [코루틴 Dispatcher](../../01-languages/kotlin/코루틴-Dispatcher.md)

---

## 문제: TTL 기반 캐시의 Latency Spike

### Cache Stampede (Thundering Herd)

```
TTL 만료 순간:
  요청 1 → cache miss → DB 조회 시작
  요청 2 → cache miss → DB 조회 시작  ← 동시에!
  요청 3 → cache miss → DB 조회 시작  ← 동시에!
  ...N개 요청 → DB에 동시 N개 쿼리 폭탄
  → DB 응답 지연 → 전체 Latency Spike
```

---

## Cache Stampede 방지 전략 비교

### A. Mutex Lock (분산 락)

```
miss 발생
  → Redis SETNX로 락 획득 시도
  → 락 획득 성공: DB 조회 → 캐시 갱신 → 락 해제
  → 락 획득 실패: 대기 → 락 해제 후 캐시 재조회
```

- 단점: 락 대기 시간 발생, 락 만료 타이밍 관리 복잡

### B. Refresh Ahead (이력서 적용)

```
TTL 만료 전에 백그라운드에서 미리 갱신
  → 사용자는 항상 캐시 hit
  → Latency Spike 없음
```

### C. TTL Jitter

```
TTL = 기본값 + random(0, 30초)
  → 같은 시점에 대량 만료 방지
  → 부하 분산 (완전 해결은 아님, 확률적 완화)
```

---

## Refresh Ahead 구현

### 기본 흐름

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

분산 락으로 하나만 갱신하도록 보장.

### Refresh Ahead vs 일반 Cache Miss에서의 락 차이

| | 일반 Cache Miss + 락 | Refresh Ahead + 락 |
|---|---|---|
| 캐시 상태 | 없음 | 있음 (만료 임박) |
| 락 실패 시 | 대기 필수 (응답할 데이터 없음) | 즉시 반환 (캐시 데이터 있음) |
| 복잡도 | 높음 | 낮음 |

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

---

## SupervisorJob vs Job

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

앱 전체 생명주기를 가진 스코프에는 항상 `SupervisorJob()`.

---

## coroutineScope를 못 쓰는 이유

```kotlin
// 잘못된 구현
suspend fun getExhibition(id: Long): Exhibition {
    val cached = redis.get(key)

    coroutineScope {
        launch { backgroundRefresh() }  // 갱신 시작
        // coroutineScope는 자식이 모두 완료될 때까지 대기!
    }  // ← backgroundRefresh() 완료 후에야 여기 도달

    return cached  // 갱신 완료 후 반환 → 의미 없음
}

// 올바른 구현
suspend fun getExhibition(id: Long): Exhibition {
    val cached = redis.get(key)
    applicationScope.launch { backgroundRefresh() }  // 독립적 백그라운드 실행
    return cached  // 즉시 반환
}
```

---

## 전략 선택 기준

| 상황 | 추천 전략 |
|---|---|
| 단순 조회, 갱신 빈도 낮음 | TTL Jitter |
| Cache miss 자체를 막아야 함 | Refresh Ahead |
| miss 시 DB 동시 요청을 막아야 함 | Mutex Lock |
| 실시간성이 중요하지 않은 데이터 | Refresh Ahead + 긴 TTL |
| 실시간성이 중요한 데이터 | Write-Through 또는 캐시 미사용 |
