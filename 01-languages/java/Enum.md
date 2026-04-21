---
tags: [java, enum, type-safety, singleton]
status: completed
created: 2026-04-14
---

# Enum

## 핵심 개념

**Enum**은 고정된 상수 집합을 타입으로 정의하는 Java의 특수한 클래스다. `static final int` 상수와 달리 **컴파일 타임 타입 안정성**을 보장하며, 내부적으로 **싱글톤**으로 구현되어 인스턴스가 단 하나만 존재한다.

## 동작 원리

### static final 상수의 문제

```java
// static final 상수 — 타입 안정성 없음
public static final int SPRING = 1;
public static final int SUMMER = 2;
public static final int FALL = 3;
public static final int WINTER = 4;

void setSeason(int season) { ... }

setSeason(1);     // SPRING 의도? 의미 불명확
setSeason(999);   // 유효하지 않은 값인데 컴파일 통과!
setSeason(-1);    // 이것도 통과...
```

### Enum으로 해결

```java
enum Season { SPRING, SUMMER, FALL, WINTER }

void setSeason(Season season) { ... }

setSeason(Season.SPRING);  // ✓ 명확
setSeason(999);            // ✗ 컴파일 에러 — Season 타입만 허용
```

<div style="font-family: -apple-system, sans-serif; max-width: 480px; margin: 12px auto; font-size: 12px;">
<div style="display: flex; gap: 8px;">
<div style="flex: 1; border: 2px solid #E74C3C; border-radius: 8px; padding: 12px;">
<div style="background: #E74C3C; color: white; padding: 6px; border-radius: 4px; text-align: center; font-weight: bold; margin-bottom: 6px;">static final int</div>
<div style="font-size: 11px; line-height: 1.8;">
타입 = <code>int</code> → 아무 정수나 허용<br/>
의미 불명확 → 매직 넘버<br/>
namespace 충돌 가능
</div>
</div>
<div style="flex: 1; border: 2px solid #27AE60; border-radius: 8px; padding: 12px;">
<div style="background: #27AE60; color: white; padding: 6px; border-radius: 4px; text-align: center; font-weight: bold; margin-bottom: 6px;">Enum</div>
<div style="font-size: 11px; line-height: 1.8;">
타입 = <code>Season</code> → 정의된 값만 허용<br/>
자기 서술적 → 의미 명확<br/>
namespace 안전 (Season.SPRING)
</div>
</div>
</div>
</div>

### Enum의 내부 구조

Enum은 컴파일 시 `java.lang.Enum`을 상속하는 **final 클래스**로 변환된다.

```java
// 개발자가 작성
enum Season { SPRING, SUMMER, FALL, WINTER }

// 컴파일러가 변환 (개념적)
final class Season extends java.lang.Enum<Season> {
    public static final Season SPRING = new Season("SPRING", 0);
    public static final Season SUMMER = new Season("SUMMER", 1);
    public static final Season FALL   = new Season("FALL", 2);
    public static final Season WINTER = new Season("WINTER", 3);

    // static final로 인스턴스를 미리 생성 → 싱글톤
}
```

- 각 상수는 **static final 인스턴스** → 런타임에 딱 하나만 존재 (싱글톤)
- `final class` → 상속 불가
- `==` 비교 안전 (동일 인스턴스이므로)

### 기본 제공 메서드

| 메서드 | 설명 |
|--------|------|
| `name()` | 상수의 이름 반환 (String) |
| `ordinal()` | 선언 순서 반환 (0부터) |
| `values()` | 모든 상수 배열 반환 |
| `valueOf(String)` | 이름으로 상수 조회 |

### Enum에 필드와 메서드 추가

Enum은 일반 클래스처럼 **필드, 생성자, 메서드**를 가질 수 있다.

```java
enum HttpStatus {
    OK(200, "성공"),
    NOT_FOUND(404, "찾을 수 없음"),
    INTERNAL_ERROR(500, "서버 오류");

    private final int code;
    private final String message;

    HttpStatus(int code, String message) {  // private 생성자
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}

HttpStatus status = HttpStatus.OK;
status.getCode();     // 200
status.getMessage();  // "성공"
```

### Enum + switch 호환

```java
switch (status) {
    case OK -> handleSuccess();
    case NOT_FOUND -> handle404();
    case INTERNAL_ERROR -> handle500();
}
// Java 17+ sealed switch에서는 default 생략 가능 (모든 케이스 커버 시)
```

### Enum으로 싱글톤 구현

Effective Java에서 권장하는 싱글톤 구현 방식이다.

```java
enum Singleton {
    INSTANCE;

    private final Connection connection;

    Singleton() {
        connection = createConnection();
    }

    public Connection getConnection() {
        return connection;
    }
}

Singleton.INSTANCE.getConnection();
```

리플렉션, 직렬화에도 안전한 **가장 안전한 싱글톤**이다.

## 관련 문서

- [[Static-vs-Non-static]] — static final과 enum의 차이
- [[Interface-vs-Abstract-Class]] — enum이 인터페이스를 구현하는 패턴
- [[Collections-Framework]] — EnumSet, EnumMap
- [[직렬화-역직렬화]] — enum의 직렬화 안전성
