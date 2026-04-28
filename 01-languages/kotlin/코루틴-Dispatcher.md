---
tags: [kotlin, coroutine, dispatcher]
status: completed
created: 2026-02-25
---

# 코루틴 Dispatcher

## 핵심 개념

**CoroutineDispatcher**는 코루틴을 **어느 스레드(풀)에서 실행할지** 결정하는 컴포넌트다. 작업 특성(CPU-bound vs I/O-bound)에 따라 적절한 Dispatcher를 선택해야 최적의 성능을 낼 수 있다.

```kotlin
// Dispatcher 지정 방법
launch(Dispatchers.IO) { ... }
withContext(Dispatchers.Default) { ... }
async(Dispatchers.Main) { ... }
```

## 동작 원리

### 기본 Dispatcher 종류

#### Dispatchers.Default
- **스레드 수**: CPU 코어 수 (최소 2)
- **내부 구현**: ForkJoinPool (Work-Stealing)
- **적합한 작업**: CPU 집약적 연산 (정렬, 이미지 처리, 암호화 등)

```kotlin
// CPU-bound 작업
withContext(Dispatchers.Default) {
    val sorted = largeList.sortedBy { it.score }  // CPU 연산
}
```

> [!note] Default Dispatcher 스레드 수가 코어 수인 이유
> 스레드가 코어 수보다 많으면 [[dev/06-computer-science/os/스레드-모델|Context Switching]]이 발생한다. CPU는 한 번에 하나의 스레드만 실행하므로, 코어 수 = Context Switching 없이 최대 병렬 실행 가능 수이다.

#### Dispatchers.IO
- **스레드 수**: 최대 64개 (기본값), `kotlinx.coroutines.io.parallelism`으로 조정 가능
- **Default와 스레드풀 공유** (별도 풀 아님, 확장 방식)
- **적합한 작업**: 네트워크 I/O, 파일 I/O, DB 쿼리 등

```kotlin
// I/O-bound 작업
withContext(Dispatchers.IO) {
    val response = httpClient.get(url)  // 네트워크 대기
}
```

**왜 64개인가?**
- I/O 작업 중 스레드는 대부분 suspend 상태 (CPU 점유 없음)
- 많은 스레드를 둬도 실제 동시 CPU 사용은 소수
- 동시 I/O 요청 처리량 확보가 목적

```
IO 스레드 64개 중:
  - 실제 CPU 사용: 소수 (나머지는 I/O 대기 중 suspend)
  - 동시 진행 가능한 네트워크 요청: 최대 64개
```

#### Dispatchers.Main
- **스레드 수**: 1개 (UI 메인 스레드)
- **적합한 작업**: UI 업데이트 (Android, JavaFX)
- 서버 사이드에서는 거의 사용 안 함

#### Dispatchers.Unconfined
- 특정 스레드에 제한 없음
- 첫 번째 suspend 이전: 호출한 스레드에서 실행
- 재개 시: resume한 스레드에서 실행
- **일반적으로 사용 비권장** (예측 불가능한 스레드 전환)

### Default vs IO 비교

```
CPU-bound 작업:
  스레드: ████████████████████████  (계속 CPU 사용)
  코어 수 초과 시:
    스레드A: ████░░░████░░░████     (컨텍스트 스위칭 낭비)
    스레드B:    ░░░████░░░████░░░

I/O-bound 작업:
  스레드: ████░░░░░░░░░░░░░░████   (대부분 대기)
         ↑ CPU  ↑ 네트워크 대기 ↑ CPU
  64개 스레드가 있어도 동시 CPU 사용은 소수
```

| | Default | IO |
|---|---|---|
| 스레드 수 | CPU 코어 수 | 최대 64 |
| 적합한 작업 | CPU 연산 | 네트워크/파일/DB |
| 초과 시 문제 | Context Switching | 불필요한 메모리 |

### withContext: Dispatcher 전환

```kotlin
suspend fun processData(id: Long): Result {
    // I/O: DB 조회
    val data = withContext(Dispatchers.IO) {
        repository.findById(id)
    }

    // CPU: 데이터 가공
    val processed = withContext(Dispatchers.Default) {
        heavyComputation(data)
    }

    return processed
}
```

`withContext`는 새 코루틴을 만들지 않고 현재 코루틴의 Dispatcher만 전환.

## 코드 예시

### 커스텀 Dispatcher

```kotlin
// 스레드 수 제한이 필요한 경우
val limitedDispatcher = Dispatchers.IO.limitedParallelism(10)

// 단일 스레드 (순서 보장 필요 시)
val singleThread = newSingleThreadContext("MyThread")

// 고정 스레드풀
val fixedPool = newFixedThreadPoolContext(4, "WorkerPool")
```

### Spring WebFlux에서의 Dispatcher

[[dev/02-frameworks/spring/web/WebFlux-Coroutine-통합|WebFlux]] + Coroutine 환경에서는 대부분 Dispatcher를 명시하지 않아도 됨.

```kotlin
@GetMapping("/products/{id}")
suspend fun getProduct(@PathVariable id: Long): Product {
    // WebFlux가 적절한 스레드(Netty event loop)에서 실행
    return productService.findById(id)
}
```

> [!warning] WebFlux event loop에서 blocking 호출 금지
> Netty event loop 스레드에서 blocking 코드를 호출하면 전체 서버 응답이 불가능해질 수 있다. blocking 코드가 섞이면 반드시 `Dispatchers.IO`로 전환해야 한다.

```kotlin
suspend fun findById(id: Long): Product {
    return withContext(Dispatchers.IO) {
        // JDBC, 파일 IO 등 blocking 작업
        jdbcTemplate.queryForObject(...)
    }
}
```

## 관련 문서

- [[코루틴-기초]]
- [[코루틴-동시성]]
- [[dev/06-computer-science/os/스레드-모델|스레드-모델]]
