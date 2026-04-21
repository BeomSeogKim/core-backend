---
tags: [java, wrapper, auto-boxing, auto-unboxing]
status: completed
created: 2026-04-14
---

# Wrapper Class

## 핵심 개념

**Wrapper Class**는 primitive 타입을 객체로 감싼 클래스다. primitive 타입은 **객체가 아니고**, **null이 불가능하며**, **Collection에 넣을 수 없는** 한계가 있어 이를 해결하기 위해 존재한다.

## 동작 원리

### Primitive → Wrapper 매핑

| Primitive | Wrapper | 크기 |
|-----------|---------|------|
| `byte` | `Byte` | 1B |
| `short` | `Short` | 2B |
| `int` | **`Integer`** | 4B |
| `long` | `Long` | 8B |
| `float` | `Float` | 4B |
| `double` | `Double` | 8B |
| `char` | **`Character`** | 2B |
| `boolean` | `Boolean` | 1B |

### Wrapper Class가 필요한 이유

```
Primitive 타입의 한계:
┌─────────────────────────────────────────────┐
│  1. 객체가 아님 → 메서드 호출 불가              │
│  2. null 불가 → DB의 NULL 매핑 못 함           │
│  3. Collection 불가 → List<int> 컴파일 에러    │
│  4. 제네릭 타입 파라미터로 사용 불가              │
└─────────────────────────────────────────────┘
                    ↓ Wrapper로 해결
┌─────────────────────────────────────────────┐
│  Integer i = null;           // null 가능     │
│  List<Integer> list = ...;   // Collection OK │
│  i.toString();               // 메서드 호출 OK │
└─────────────────────────────────────────────┘
```

### Auto-boxing / Auto-unboxing

Java 5부터 primitive ↔ Wrapper 간 **자동 변환**을 지원한다.

```java
// Auto-boxing: primitive → Wrapper (자동)
Integer num = 42;  // Integer.valueOf(42) 호출

// Auto-unboxing: Wrapper → primitive (자동)
int value = num;   // num.intValue() 호출
```

```
Auto-boxing                    Auto-unboxing
int 42  →  Integer.valueOf(42)    Integer obj  →  obj.intValue()
           → Integer 객체 생성                  → int 값 추출
```

### Auto-boxing의 성능 문제

Auto-boxing은 **새 객체 생성**을 수반한다. 대량 반복에서 발생하면 심각한 성능 저하를 일으킨다.

```java
// 나쁜 예 — 반복마다 Auto-boxing 발생
Long sum = 0L;
for (long i = 0; i < 1_000_000; i++) {
    sum += i;  // sum = Long.valueOf(sum.longValue() + i)
               // 매번 Long 객체 생성 → 100만 개 임시 객체
}

// 좋은 예 — primitive 사용
long sum = 0L;
for (long i = 0; i < 1_000_000; i++) {
    sum += i;  // 단순 덧셈, 객체 생성 없음
}
```

### Integer 캐시 (IntegerCache)

`Integer.valueOf()`는 **-128 ~ 127** 범위의 값을 캐싱하여 동일 객체를 반환한다.

```
Integer.valueOf(n) 호출 시:
┌───────────────────────────────────────────┐
│  -128 <= n <= 127 ?                       │
│    YES → 캐시된 객체 반환 (같은 객체)         │
│    NO  → new Integer(n) (새 객체 생성)      │
└───────────────────────────────────────────┘
```

```java
Integer a = 127;  // 캐시 범위
Integer b = 127;
System.out.println(a == b);      // true  (같은 캐시 객체)
System.out.println(a.equals(b)); // true

Integer c = 128;  // 캐시 범위 밖
Integer d = 128;
System.out.println(c == d);      // false (다른 객체)
System.out.println(c.equals(d)); // true  (값은 같음)
```

> [!warning] Wrapper 비교는 반드시 equals()
> `==`는 객체의 주소를 비교하므로, 캐시 범위 밖에서는 같은 값이라도 `false`가 된다. Wrapper 타입 비교는 항상 `equals()`를 사용한다.

## 코드 예시

```java
// null 처리 — DB 매핑에서 유용
Integer dbValue = resultSet.getObject("column", Integer.class);
// column이 NULL이면 dbValue = null (primitive int는 불가능)

// 제네릭에서 Wrapper 필수
Map<String, Integer> scores = new HashMap<>();
scores.put("math", 90);     // Auto-boxing
int math = scores.get("math"); // Auto-unboxing

// NullPointerException 주의
Integer nullable = null;
int value = nullable;  // Auto-unboxing 시 NPE 발생!
```

## 관련 문서

- [[Collections-Framework]] — 제네릭과 Wrapper의 관계
- [[equals와-hashCode]] — Wrapper 비교와 equals
- [[제네릭]] — 타입 파라미터에 primitive 사용 불가
- [[JVM]] — 캐시 범위와 메모리 구조
