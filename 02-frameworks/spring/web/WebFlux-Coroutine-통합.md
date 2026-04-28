---
tags: [spring, webflux, coroutine]
status: completed
created: 2026-03-02
---

# Spring WebFlux + Coroutine 통합 패턴

## 핵심 개념

Spring **WebFlux**는 Reactor(Mono/Flux) 기반의 비동기 웹 프레임워크다. Kotlin **Coroutine**은 이를 `suspend` 함수로 추상화하여 가독성을 크게 향상시킨다. Reactor의 선언적 체이닝 대신 순차적 코드 스타일로 동일한 비동기 동작을 구현할 수 있다.

> [!note] WebFlux에서 Coroutine을 쓰는 이유
> Reactor의 `zipWith`, `flatMap` 등 선언적 체이닝은 복잡한 로직에서 가독성이 떨어진다. Coroutine의 `suspend` 함수를 사용하면 동일한 비동기 동작을 순차적 코드처럼 작성할 수 있다.

```kotlin
// Reactor 방식
fun getProduct(id: Long): Mono<Product> {
    return productClient.getDetail(id)
        .zipWith(priceClient.getPrice(id))
        .map { (detail, price) -> Product(detail, price) }
}

// Coroutine 방식 (동일 동작, 가독성 우수)
suspend fun getProduct(id: Long): Product {
    val detail = productClient.getDetail(id).awaitSingle()
    val price = priceClient.getPrice(id).awaitSingle()
    return Product(detail, price)
}
```

## 동작 원리

### 안티패턴: 루프 내 awaitSingle()

```kotlin
// 잘못된 패턴 - 직렬 실행
suspend fun fetchProducts(chunks: List<List<Long>>): List<Product> {
    val result = mutableListOf<Product>()
    chunks.forEach { chunk ->
        val products = productClient.getProducts(chunk).awaitSingle()  // 직렬!
        result.addAll(products)
    }
    return result
}
// 120 chunks × 250ms = 30초
```

> [!warning] 루프 내 awaitSingle()은 직렬 실행된다
> `awaitSingle()`은 스레드를 blocking하지 않지만(suspend), 이 코루틴은 이전 청크 완료 후에만 다음 청크로 진행한다. 네트워크 I/O 지연이 청크 수만큼 직렬로 누적된다.

### 올바른 패턴: Scatter-Gather

[[dev/01-languages/kotlin/코루틴-동시성|코루틴-동시성]]의 `async`/`awaitAll`을 활용한 병렬 처리 패턴이다.

```kotlin
// 개선된 패턴 - 병렬 실행
suspend fun fetchProducts(chunks: List<List<Long>>): List<Product> {
    return coroutineScope {
        chunks.map { chunk ->
            async {
                productClient.getProducts(chunk)
                    .onErrorReturn(emptyList())  // 부분 실패 허용
                    .awaitSingle()
            }
        }.awaitAll().flatten()
    }
}
// max(각 청크 응답시간) ≈ 2초
```

### 이중 병렬화 (기획전 성능 개선 사례)

**상황**: 3,000개 상품 기획전 조회 (30s -> 2s)

```kotlin
suspend fun getExhibition(exhibitionId: Long): ExhibitionResponse {
    return coroutineScope {
        // 모듈 레벨 병렬화: 서로 의존성 없는 모듈들
        val carouselDeferred = async { fetchCarousel(exhibitionId) }
        val cheapDeferred = async { fetchCheapProducts(exhibitionId) }
        val productDeferred = async {
            // 데이터 레벨 병렬화: Scatter-Gather
            val productIds = fetchProductIds(exhibitionId)
            productIds
                .chunked(25)
                .map { chunk ->
                    async {
                        productClient.getProducts(chunk)
                            .onErrorReturn(emptyList())
                            .awaitSingle()
                    }
                }
                .awaitAll()
                .flatten()
        }

        ExhibitionResponse(
            carousel = carouselDeferred.await(),
            cheap = cheapDeferred.await(),
            products = productDeferred.await()
        )
    }
}
```

### Cache Refresh Ahead 전략

단순 TTL 방식의 문제:
```
TTL 만료 시점:
  요청 → 캐시 miss → DB 조회 (느림) → 응답 지연 (Latency Spike)
  동시 다수 요청 → Cache Stampede (DB 과부하)
```

> [!tip] Refresh Ahead로 Latency Spike 방지
> TTL 만료 전에 백그라운드에서 미리 캐시를 갱신하면, 사용자는 항상 캐시 hit을 경험한다. Cache Stampede도 방지된다.

Refresh Ahead 해결책:
```kotlin
suspend fun getExhibitionWithCache(id: Long): Exhibition {
    val cached = redis.get(key)

    if (cached != null) {
        val ttl = redis.ttl(key)
        // TTL 1분 미만 → 백그라운드에서 미리 갱신
        if (ttl < 60) {
            backgroundScope.launch {
                val fresh = fetchFromDb(id)
                redis.set(key, fresh, Duration.ofMinutes(10))
            }
        }
        return cached  // 사용자에게는 캐시된 데이터 즉시 반환
    }

    // 캐시 없을 때만 DB 조회
    val fresh = fetchFromDb(id)
    redis.set(key, fresh, Duration.ofMinutes(10))
    return fresh
}
```

### WebFlux vs Spring MVC + Virtual Thread

#### WebFlux가 여전히 필요한 경우

**Backpressure**: 생산 속도 > 소비 속도일 때 생산자 제어

```kotlin
// WebFlux: Backpressure 지원
fun streamProducts(): Flux<Product> {
    return productRepository.findAll()  // Flux - 소비자 속도에 맞춰 생산
        .onBackpressureBuffer(100)
}

// Virtual Thread: Backpressure 없음
fun getProducts(): List<Product> {
    return productRepository.findAll()  // 전부 메모리에 로드
}
```

| 상황 | 추천 |
|---|---|
| 단순 CRUD, I/O 위주 | Virtual Thread + Spring MVC |
| Kafka/스트리밍, Backpressure 필요 | WebFlux 유지 |
| SSE, WebSocket | WebFlux 유지 |
| 기존 WebFlux 코드베이스 | 유지 (마이그레이션 이유 없음) |

### 주의사항: WebFlux 스레드에서 Blocking 호출

```kotlin
// 위험 - event loop 스레드 blocking
@GetMapping("/products")
suspend fun getProducts(): List<Product> {
    return jdbcTemplate.query(...)  // blocking! event loop 스레드 점유
    // → 전체 서버 응답 불가 위험
}

// 안전 - IO Dispatcher로 전환
@GetMapping("/products")
suspend fun getProducts(): List<Product> {
    return withContext(Dispatchers.IO) {
        jdbcTemplate.query(...)  // IO 전용 스레드에서 blocking 허용
    }
}
```

## 관련 문서

- [[dev/01-languages/kotlin/코루틴-동시성|코루틴-동시성]]
- [[dev/01-languages/kotlin/코루틴-Dispatcher|코루틴-Dispatcher]]
- [[dev/01-languages/kotlin/코루틴-기초|코루틴-기초]]
