# Java vs TypeScript 주요 차이점

Java 백엔드 개발자가 TypeScript로 이식 작업 시 마주치는 핵심 차이점 정리.

---

## 1. Interface — 타이핑 방식

### Java: 명목적 타이핑 (Nominal Typing)
- `implements` 명시 필수
- 인터페이스 이름으로 타입을 판단

```java
interface CategoryMapper {
    String map(String category);
}

class KoreaMapper implements CategoryMapper {
    public String map(String category) { ... }
}
```

### TypeScript: 구조적 타이핑 (Structural Typing / Duck Typing)
- `implements` 없어도 shape(구조)만 맞으면 자동으로 인터페이스 만족

```typescript
interface CategoryMapper {
    map(category: string): string;
}

// implements 없어도 OK
const koreaMapper = { map: (c: string) => c.toUpperCase() };

function process(mapper: CategoryMapper) { ... }
process(koreaMapper); // 정상 동작
```

---

## 2. Null 처리

### Java: Optional<T>

```java
Optional<String> category = Optional.ofNullable(value);
String result = category.orElse("DEFAULT");
```

### TypeScript: Union Type + 연산자

```typescript
const category: string | null = value;

// Nullish Coalescing
const result = category ?? "DEFAULT";

// Optional Chaining
const upper = category?.toUpperCase();
```

---

## 3. 컬렉션 처리

### Java: Stream API

```java
List<String> result = list.stream()
    .filter(x -> x.isActive())
    .map(x -> x.getName())
    .collect(Collectors.toList());
```

### TypeScript: Array 메서드 (거의 동일)

```typescript
const result = list
    .filter(x => x.isActive)
    .map(x => x.name);
// Stream 래핑 불필요
```

---

## 4. Enum

### Java: 메서드와 필드를 가진 강력한 Enum

```java
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

### TypeScript: `as const` + 유틸 함수 패턴 (권장)

```typescript
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

**TypeScript `enum`이 아닌 `as const`를 권장하는 이유**
- 리터럴 타입으로 좁혀짐 → 타입 안전성 강화
- 런타임 오버헤드 없음
- IDE 자동완성, 타입 추론이 더 정확

---

## 5. 타입 추출 패턴

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

processOrder(COUNTRY.UNITED_STATES) // ✅
processOrder({ code: 'XX', ... })   // ❌ 컴파일 에러
```

---

## 6. Promise.all vs Promise.allSettled

| | `Promise.all()` | `Promise.allSettled()` |
|--|----------------|----------------------|
| 실패 시 동작 | 하나라도 실패하면 전체 reject (fail-fast) | 모두 기다리고 각각의 결과 반환 |
| 반환값 | 성공 값 배열 | `{ status, value/reason }` 배열 |
| 사용 시점 | 하나 실패 시 전체 롤백 필요 | 부분 실패 허용, 개별 결과 필요 |

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

---

## 요약

| 개념 | Java | TypeScript |
|------|------|-----------|
| 타이핑 | Nominal (이름 기반) | Structural (구조 기반) |
| Null 처리 | `Optional<T>` | `T \| null`, `??`, `?.` |
| 컬렉션 | Stream API | Array 메서드 |
| Enum | 메서드/필드 포함 강력한 enum | `as const` + 유틸 함수 |
| 비동기 | `CompletableFuture` | `Promise` / `async-await` |
