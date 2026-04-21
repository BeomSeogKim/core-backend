---
tags: [java, immutable, defensive-copy, thread-safe]
status: completed
created: 2026-04-14
---

# Immutable Object (불변 객체)

## 핵심 개념

**불변 객체(Immutable Object)**는 한 번 생성되면 내부 상태를 **변경할 수 없는** 객체다. Java의 `String`, `Integer`, `LocalDate` 등이 대표적이다. 동기화 없이 [[Thread-safe|Thread-safe]]하고, 안전한 공유와 캐싱이 가능하다.

## 동작 원리

### 불변 객체를 만드는 5가지 조건

```
┌────────────────────────────────────────────────────┐
│  불변 객체 5조건                                      │
├────────────────────────────────────────────────────┤
│  1. 클래스를 final로 선언     → 상속 차단               │
│  2. 모든 필드를 private final → 재할당 차단             │
│  3. setter 메서드 없음        → 외부 수정 차단          │
│  4. 생성자에서 방어적 복사     → 입력 참조 분리          │
│  5. getter에서 방어적 복사     → 출력 참조 분리          │
└────────────────────────────────────────────────────┘
```

### 방어적 복사 (Defensive Copy)

`final`을 붙여도 참조 타입 필드는 **내부 객체 변경이 가능**하다. 방어적 복사로 원본 참조와 분리해야 한다.

```
방어적 복사 없이 (위험):

외부 List ──참조──→ [A, B, C]
                       ↑
내부 필드 ──참조───────┘   ← 같은 객체 공유!

외부에서 add("D") → 내부도 [A, B, C, D]로 변경됨
```

```
방어적 복사 적용 (안전):

외부 List ──참조──→ [A, B, C]

내부 필드 ──참조──→ [A, B, C]   ← 독립된 복사본!

외부에서 add("D") → 내부는 [A, B, C] 유지
```

### 왜 불변 객체를 써야 하는가

| 장점 | 설명 |
|------|------|
| **Thread-safe** | 상태가 안 바뀌므로 동기화 없이 안전하게 공유 |
| **부수효과 없음** | 메서드 호출이 객체 상태를 바꾸지 않아 예측 가능 |
| **안전한 HashMap 키** | hashCode 불변 → 해시 충돌 방지 |
| **캐싱 가능** | 같은 값은 같은 인스턴스 재사용 (Integer 캐시 등) |
| **실패 원자성** | 연산 도중 실패해도 객체가 깨지지 않음 |

## 코드 예시

```java
// 잘못된 불변 객체 — 참조 타입 필드에 방어적 복사 없음
public final class BadImmutable {
    private final List<String> items;

    public BadImmutable(List<String> items) {
        this.items = items;  // 외부 참조 그대로 저장
    }

    public List<String> getItems() {
        return items;  // 내부 참조 그대로 노출
    }
}

List<String> original = new ArrayList<>(List.of("A", "B"));
BadImmutable bad = new BadImmutable(original);
original.add("C");         // 외부 변경이 내부에 영향!
bad.getItems().add("D");   // getter로 내부 수정 가능!
```

```java
// 올바른 불변 객체
public final class GoodImmutable {
    private final String name;
    private final List<String> items;

    public GoodImmutable(String name, List<String> items) {
        this.name = name;
        this.items = new ArrayList<>(items);  // 방어적 복사 (입력)
    }

    public String getName() {
        return name;  // String은 이미 불변
    }

    public List<String> getItems() {
        return Collections.unmodifiableList(items);  // 읽기 전용 뷰 (출력)
    }
}

List<String> original = new ArrayList<>(List.of("A", "B"));
GoodImmutable good = new GoodImmutable("test", original);
original.add("C");          // 내부에 영향 없음 ✓
good.getItems().add("D");   // UnsupportedOperationException ✓
```

```java
// Java 16+ record — 간결한 불변 객체
public record Point(int x, int y) { }
// final 클래스, private final 필드, 생성자, getter, equals, hashCode 자동 생성
// 단, 참조 타입 필드가 있으면 방어적 복사는 직접 해야 함

public record Team(String name, List<String> members) {
    public Team {  // compact constructor
        members = List.copyOf(members);  // 방어적 복사
    }
}
```

## 관련 문서

- [[String-심화]] — String이 불변인 이유와 String Pool
- [[Final-Finally-Finalize]] — final 키워드와 불변
- [[값 복사]] — 얕은 복사 vs 깊은 복사
- [[HashMap-HashTable-ConcurrentHashMap]] — 불변 객체가 좋은 HashMap 키인 이유
- [[Thread-safe]] — 불변 객체의 Thread-safe 특성
