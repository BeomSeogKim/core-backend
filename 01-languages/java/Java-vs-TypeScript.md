---
tags:
  - java
  - typescript
  - comparison
status: completed
created: 2026-03-03
---

# Java vs TypeScript 주요 차이점

## 핵심 개념

Java 백엔드 개발자가 TypeScript로 이식 작업 시 마주치는 핵심 차이점 정리. 타이핑 방식, Null 처리, 컬렉션, Enum, 비동기 패턴에서 근본적인 차이가 있다.

| 개념 | Java | TypeScript |
|------|------|-----------|
| 타이핑 | **Nominal** (이름 기반) | **Structural** (구조 기반) |
| Null 처리 | `Optional<T>` | `T \| null`, `??`, `?.` |
| 컬렉션 | Stream API | Array 메서드 |
| Enum | 메서드/필드 포함 강력한 enum | `as const` + 유틸 함수 |
| 비동기 | `CompletableFuture` | `Promise` / `async-await` |

## 동작 원리

### 1. Interface - 타이핑 방식

**Java: Nominal Typing** - `implements` 명시 필수. 인터페이스 이름으로 타입을 판단한다.

**TypeScript: Structural Typing (Duck Typing)** - `implements` 없어도 shape(구조)만 맞으면 자동으로 인터페이스를 만족한다.

> [!note]
> TypeScript는 "같은 모양이면 같은 타입"이라는 철학을 따른다. Java 개발자가 가장 먼저 적응해야 할 차이점이다.

### 2. Null 처리

Java는 `Optional<T>` 래퍼를 사용하고, TypeScript는 **Union Type**과 연산자(`??`, `?.`)로 간결하게 처리한다.

### 3. 컬렉션 처리

Java의 **Stream API**는 `.stream()` 래핑이 필요하지만, TypeScript의 **Array 메서드**는 바로 체이닝 가능하다.

### 4. Enum

> [!warning]
> TypeScript에서는 내장 `enum` 대신 `as const` 패턴을 권장한다. 리터럴 타입 추론, 런타임 오버헤드 제거, IDE 자동완성 정확도에서 이점이 있다.

### 5. 타입 추출 패턴

`(typeof COUNTRY)[keyof typeof COUNTRY]` 패턴으로 `as const` 객체에서 Union Type을 추출한다.

### 6. Promise.all vs Promise.allSettled

| | `Promise.all()` | `Promise.allSettled()` |
|--|----------------|----------------------|
| 실패 시 동작 | 하나라도 실패하면 전체 reject (fail-fast) | 모두 기다리고 각각의 결과 반환 |
| 반환값 | 성공 값 배열 | `{ status, value/reason }` 배열 |
| 사용 시점 | 하나 실패 시 전체 롤백 필요 | 부분 실패 허용, 개별 결과 필요 |

## 코드 예시

### Interface 비교

```java
// Java: Nominal Typing - implements 필수
interface CategoryMapper {
    String map(String category);
}

class KoreaMapper implements CategoryMapper {
    public String map(String category) { ... }
}
```

```typescript
// TypeScript: Structural Typing - implements 없어도 OK
interface CategoryMapper {
    map(category: string): string;
}

const koreaMapper = { map: (c: string) => c.toUpperCase() };

function process(mapper: CategoryMapper) { ... }
process(koreaMapper); // 정상 동작
```

### Null 처리 비교

```java
// Java: Optional<T>
Optional<String> category = Optional.ofNullable(value);
String result = category.orElse("DEFAULT");
```

```typescript
// TypeScript: Union Type + 연산자
const category: string | null = value;

// Nullish Coalescing
const result = category ?? "DEFAULT";

// Optional Chaining
const upper = category?.toUpperCase();
```

### 컬렉션 처리 비교

```java
// Java: Stream API
List<String> result = list.stream()
    .filter(x -> x.isActive())
    .map(x -> x.getName())
    .collect(Collectors.toList());
```

```typescript
// TypeScript: Array 메서드 (Stream 래핑 불필요)
const result = list
    .filter(x => x.isActive)
    .map(x => x.name);
```

### Enum 비교

```java
// Java: 메서드와 필드를 가진 강력한 Enum
enum Country {
    UNITED_STATES("US", "USD"),
    AUSTRALIA("AU", "AUD");

    private final String code;
    private final String currency;

    Country(String code, String currency) {
        this.code = code;
        this.currency = currency;
    }

    public static Country from(String code) {
        return Arrays.stream(values())
            .filter(c -> c.code.equals(code))
            .findFirst()
            .orElseThrow();
    }
}
```

```typescript
// TypeScript: as const + 유틸 함수 패턴 (권장)
export const COUNTRY = {
    UNITED_STATES: { code: 'US', currency: 'USD', marketPlaceId: 'EBAY_US' },
    AUSTRALIA:     { code: 'AU', currency: 'AUD', marketPlaceId: 'EBAY_AU' },
} as const;

// 타입 추출
export type Country = (typeof COUNTRY)[keyof typeof COUNTRY];

// 유틸 함수
export function from(countryCode: string): Country {
    const country = Object.values(COUNTRY).find(c => c.code === countryCode);
    if (!country) throw new Error(`Unknown country code: ${countryCode}`);
    return country;
}
```

### 타입 추출 패턴 상세

```typescript
// (typeof COUNTRY)[keyof typeof COUNTRY] 분해

typeof COUNTRY
// → { UNITED_STATES: { code: 'US', ... }, AUSTRALIA: { code: 'AU', ... } }

keyof typeof COUNTRY
// → "UNITED_STATES" | "AUSTRALIA"

(typeof COUNTRY)[keyof typeof COUNTRY]
// → { code: 'US', ... } | { code: 'AU', ... }
// 즉, 각 국가 객체 중 하나의 타입
```

```typescript
// 활용
function processOrder(country: Country) { ... }

processOrder(COUNTRY.UNITED_STATES) // OK
processOrder({ code: 'XX', ... })   // 컴파일 에러
```

### Promise 비교

```typescript
// Promise.all - 하나 실패 시 전체 실패
const results = await Promise.all([task1(), task2(), task3()]);

// Promise.allSettled - 모두 완료 후 결과 확인
const results = await Promise.allSettled([task1(), task2(), task3()]);
results.forEach(r => {
    if (r.status === 'fulfilled') console.log(r.value);
    else console.error(r.reason);
});

// 내부 try-catch로 Promise.all을 allSettled처럼 사용
await Promise.all(
    items.map(async (item) => {
        try {
            await process(item);
            record.success(item);
        } catch (e) {
            record.fail(item, e); // reject 전파 안 함
        }
    })
);
```

## 관련 문서
- [[dev/01-languages/java/JVM|JVM]]
- [[dev/01-languages/java/Class|Class]]
