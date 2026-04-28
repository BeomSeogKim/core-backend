---
tags: [java, stream, lambda, functional]
status: completed
created: 2026-04-02
---

# Stream + Lambda

## 핵심 개념

**Lambda**는 함수형 인터페이스의 구현을 간결하게 표현하는 **익명 함수** 문법이다. **Stream API**는 컬렉션 데이터를 **선언적(what)**으로 처리하는 파이프라인으로, Lambda와 결합하여 함수형 스타일의 데이터 처리를 가능하게 한다.

## 동작 원리

### Lambda 표현식

**함수형 인터페이스**(추상 메서드가 하나인 인터페이스)의 구현을 간결하게 표현한다.

```java
// 기존 — 익명 클래스
Comparator<String> comp = new Comparator<String>() {
    @Override
    public int compare(String a, String b) {
        return a.length() - b.length();
    }
};

// Lambda — 같은 동작
Comparator<String> comp = (a, b) -> a.length() - b.length();

// 메서드 레퍼런스 — 더 간결
Function<String, Integer> toLength = String::length;
```

#### 주요 함수형 인터페이스

| 인터페이스 | 메서드 | 설명 |
|---|---|---|
| `Predicate<T>` | `boolean test(T t)` | 조건 판별 |
| `Function<T, R>` | `R apply(T t)` | 변환 |
| `Consumer<T>` | `void accept(T t)` | 소비 (반환값 없음) |
| `Supplier<T>` | `T get()` | 공급 (파라미터 없음) |

```java
Predicate<String> isLong = s -> s.length() > 5;
Function<String, Integer> toLength = String::length;
Consumer<String> printer = System.out::println;
Supplier<String> greeter = () -> "Hello";
```

### Stream API

컬렉션 데이터를 **파이프라인 형태**로 처리한다. for문의 **명령형(how)** 방식 대신 **선언적(what)** 방식으로 기술한다.

```java
// 명령형 — 어떻게(how) 처리할지 기술
List<String> result = new ArrayList<>();
for (String name : names) {
    if (name.length() > 3) {
        result.add(name.toUpperCase());
    }
}

// 선언적 — 무엇을(what) 할지 기술
List<String> result = names.stream()
    .filter(name -> name.length() > 3)    // 조건 필터링
    .map(String::toUpperCase)              // 변환
    .collect(Collectors.toList());         // 수집
```

### 중간 연산 vs 최종 연산

```
stream() → [중간 연산] → [중간 연산] → [최종 연산] → 결과
            (Lazy)        (Lazy)       (실행 트리거)
```

| | 중간 연산 (Intermediate) | 최종 연산 (Terminal) |
|---|---|---|
| 실행 시점 | **지연(Lazy)** — 최종 연산 전까지 실행 안 됨 | **즉시 실행** — 파이프라인 전체 트리거 |
| 반환 타입 | Stream (체이닝 가능) | 결과값 (List, Optional, void 등) |
| 대표 메서드 | `filter`, `map`, `sorted`, `distinct`, `flatMap` | `collect`, `forEach`, `count`, `findFirst`, `reduce` |

#### 지연 평가 (Lazy Evaluation)

중간 연산은 최종 연산이 호출될 때까지 **실행되지 않는다.** 이 덕분에 불필요한 연산을 줄일 수 있다.

```java
names.stream()
    .filter(n -> {
        System.out.println("filter: " + n);  // 최종 연산 없으면 출력 안 됨
        return n.length() > 3;
    })
    .map(String::toUpperCase);
// → 최종 연산이 없으므로 filter도 map도 실행되지 않음

// findFirst()가 최종 연산이면, 조건에 맞는 첫 요소를 찾는 순간 나머지는 처리하지 않음
Optional<String> first = names.stream()
    .filter(n -> n.length() > 3)
    .findFirst();  // 하나 찾으면 나머지 요소는 filter도 안 함
```

### 주요 연산 예시

```java
List<String> names = List.of("Alice", "Bob", "Charlie", "Dave", "Eve");

// filter — 조건에 맞는 요소만
names.stream().filter(n -> n.length() > 3)
    // → [Alice, Charlie, Dave]

// map — 요소 변환
names.stream().map(String::toUpperCase)
    // → [ALICE, BOB, CHARLIE, DAVE, EVE]

// flatMap — 중첩 구조 평탄화
List<List<String>> nested = List.of(List.of("a", "b"), List.of("c", "d"));
nested.stream().flatMap(Collection::stream)
    // → [a, b, c, d]

// reduce — 누적 연산
int totalLength = names.stream()
    .map(String::length)
    .reduce(0, Integer::sum);  // → 23

// collect — 결과 수집
Map<Integer, List<String>> byLength = names.stream()
    .collect(Collectors.groupingBy(String::length));
    // → {3=[Bob, Eve], 4=[Dave], 5=[Alice], 7=[Charlie]}
```

## 관련 문서

- [[제네릭]] — Stream API가 제네릭을 활용하는 방식
- [[Collections-Framework]] — Stream의 데이터 소스
- [[Interface-vs-Abstract-Class]] — 함수형 인터페이스와 default 메서드
