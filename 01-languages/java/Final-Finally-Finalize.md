---
tags: [java, final, finally, finalize, exception]
status: completed
created: 2026-04-14
---

# final / finally / finalize

## 핵심 개념

이름이 비슷하지만 완전히 다른 세 가지 키워드/메서드다.

- **final** — 변경을 금지하는 키워드 (변수, 메서드, 클래스에 사용)
- **finally** — try-catch에서 반드시 실행되는 블록
- **finalize** — GC 직전에 호출되는 Object 메서드 (deprecated)

## 동작 원리

### final

`final`은 적용 대상에 따라 의미가 달라진다.

<div style="font-family: -apple-system, sans-serif; max-width: 520px; margin: 12px auto; font-size: 12px;">
<div style="display: flex; gap: 4px; flex-wrap: wrap;">
<div style="flex: 1; min-width: 150px; background: #E74C3C; color: white; padding: 10px; border-radius: 6px;">
<div style="font-weight: bold;">final 변수</div>
<div style="font-size: 11px; margin-top: 6px;">값 변경 불가</div>
<div style="font-size: 11px; color: #FADBD8;">= 상수</div>
</div>
<div style="flex: 1; min-width: 150px; background: #E67E22; color: white; padding: 10px; border-radius: 6px;">
<div style="font-weight: bold;">final 메서드</div>
<div style="font-size: 11px; margin-top: 6px;">오버라이딩 불가</div>
<div style="font-size: 11px; color: #FDEBD0;">자식 클래스에서 재정의 X</div>
</div>
<div style="flex: 1; min-width: 150px; background: #8E44AD; color: white; padding: 10px; border-radius: 6px;">
<div style="font-weight: bold;">final 클래스</div>
<div style="font-size: 11px; margin-top: 6px;">상속 불가</div>
<div style="font-size: 11px; color: #D2B4DE;">String, Integer 등</div>
</div>
</div>
</div>

```java
// final 변수 — 재할당 불가
final int MAX = 100;
MAX = 200;  // 컴파일 에러

// final 참조 — 참조 변경 불가, 내부 상태 변경은 가능
final List<String> list = new ArrayList<>();
list.add("hello");       // ✓ 내부 상태 변경 가능
list = new ArrayList<>(); // ✗ 참조 변경 불가

// final 메서드 — 오버라이딩 불가
class Parent {
    final void criticalMethod() { ... }
}
class Child extends Parent {
    void criticalMethod() { ... }  // 컴파일 에러
}

// final 클래스 — 상속 불가
final class ImmutableClass { ... }
class SubClass extends ImmutableClass { ... }  // 컴파일 에러
```

> [!warning] final 참조 ≠ 불변 객체
> `final List`는 참조(변수)가 불변인 것이지, 리스트 내부 요소를 변경하는 것은 막지 못한다. 완전한 불변 객체를 만들려면 [[Immutable-Object]]의 모든 조건을 충족해야 한다.

### finally

try-catch 블록에서 **예외 발생 여부와 관계없이 반드시 실행**되는 블록. 리소스 해제에 주로 사용한다.

```
try 성공    → try → finally
try 실패    → try → catch → finally
catch 실패  → try → catch → finally (catch 예외는 finally 이후 전파)
```

```java
Connection conn = null;
try {
    conn = getConnection();
    // DB 작업
} catch (SQLException e) {
    // 예외 처리
} finally {
    // 예외가 나든 안 나든 반드시 실행
    if (conn != null) conn.close();
}

// Java 7+ try-with-resources가 더 깔끔
try (Connection conn = getConnection()) {
    // DB 작업
}  // AutoCloseable이면 자동 close
```

> [!note] finally가 실행되지 않는 경우
> 1. `System.exit()` 호출 — JVM 자체가 종료
> 2. JVM 강제 종료 — OS의 kill 시그널, 전원 차단 등

### finalize (Deprecated since Java 9)

`Object` 클래스의 메서드로, **GC가 객체를 회수하기 직전에** 호출된다. Java 9부터 deprecated이며, 사용해서는 안 된다.

```java
@Override
protected void finalize() throws Throwable {
    // GC 직전에 호출 (언제 호출될지 보장 없음)
    super.finalize();
}
```

**사용하면 안 되는 이유:**
1. **호출 시점을 예측할 수 없음** — GC가 언제 실행될지 모름
2. **성능 저하** — finalize 큐에서 별도 처리, 객체 수명이 연장됨
3. **예외가 무시됨** — finalize에서 발생한 예외가 삼켜짐

대안: `try-with-resources` + `AutoCloseable`, 또는 `Cleaner` (Java 9+)

### 세 가지 비교

| | final | finally | finalize |
|---|---|---|---|
| **종류** | 키워드 | 블록 | 메서드 |
| **용도** | 변경 금지 | 예외 후 정리 | GC 전 정리 |
| **적용 대상** | 변수, 메서드, 클래스 | try-catch 블록 | Object의 메서드 |
| **현재 상태** | 현역 | 현역 (try-with-resources 권장) | **Deprecated** |

## 관련 문서

- [[Checked-vs-Unchecked-Exception]] — try-catch-finally와 예외 처리
- [[Immutable-Object]] — final과 불변 객체의 관계
- [[GC]] — finalize와 GC의 관계
- [[String-심화]] — String이 final 클래스인 이유
