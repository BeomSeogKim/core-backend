---
tags: [java, string, immutable]
status: completed
created: 2026-03-30
---

# String · StringBuilder · StringBuffer

## 핵심 개념

**String**은 불변(Immutable) 객체, **StringBuilder**와 **StringBuffer**는 가변(Mutable) 객체다. String은 변경 시마다 새 객체가 생성되므로, 빈번한 문자열 조작에는 StringBuilder/StringBuffer를 사용한다.

## 동작 원리

### String — 불변 객체

```java
String s = "hello";
s = s + " world";  // "hello"는 그대로, 새로운 "hello world" 객체 생성
```

`+` 연산을 할 때마다 **새로운 String 객체가 Heap에 생성**된다. 기존 객체는 변경되지 않고, 참조만 바뀐다.

```
Heap:
  "hello"         ← 더 이상 참조 없으면 GC 대상
  "hello world"   ← s가 이제 이걸 참조
```

#### String Pool

JVM은 문자열 리터럴을 **String Pool**(Heap 내부)에서 관리한다. 같은 값의 문자열은 하나의 객체를 **공유(재사용)**한다.

```java
String a = "hello";
String b = "hello";
System.out.println(a == b);  // true — 같은 String Pool 객체 참조

String c = new String("hello");
System.out.println(a == c);  // false — new는 Pool 밖 Heap에 별도 생성
System.out.println(a.equals(c));  // true — 값은 같음
```

```
String Pool (Heap 내부):        Heap (Pool 외부):
┌─────────────┐               ┌─────────────┐
│   "hello"   │←── a, b 참조  │   "hello"   │←── c 참조
└─────────────┘               └─────────────┘
```

`intern()` 메서드를 호출하면 Pool에 있는 객체를 반환하거나, 없으면 Pool에 등록한다:
```java
String c = new String("hello").intern();
System.out.println(a == c);  // true — Pool의 객체를 반환
```

#### String이 불변인 이유

**1. 안전한 공유 (String Pool)**
- 여러 참조가 같은 String Pool 객체를 공유하는데, 하나의 참조를 통해 값이 변경되면 다른 모든 참조에 사이드 이펙트 발생
- 불변이므로 안전하게 공유 가능

**2. 보안**
- 파일 경로, DB 커넥션 URL, 클래스 로딩 경로 등에 String이 사용됨
- 변경 가능하면 보안 취약점 발생 가능 (경로 변조 등)

**3. 해시 캐싱**
- String은 `HashMap`의 키로 빈번하게 사용됨
- 불변이므로 `hashCode()`를 **한 번만 계산하고 캐싱** 가능 (`private int hash`)
- 가변이면 값이 바뀔 때마다 해시 재계산 필요

**4. Thread Safety**
- 불변 객체는 동기화 없이 여러 스레드에서 안전하게 사용 가능

### StringBuilder vs StringBuffer

둘 다 내부적으로 **가변 `char[]` (또는 `byte[]`)**을 가지고 있어 문자열을 변경할 때 새 객체를 생성하지 않는다.

```java
StringBuilder sb = new StringBuilder("hello");
sb.append(" world");  // 같은 객체의 내부 배열을 변경 (새 객체 생성 안 함)
```

| | String | StringBuilder | StringBuffer |
|---|---|---|---|
| 가변성 | **불변** | **가변** | **가변** |
| Thread Safety | 안전 (불변) | **안전하지 않음** | **안전** (`synchronized`) |
| 성능 | 조작 시 느림 (객체 생성) | **빠름** | 느림 (동기화 오버헤드) |
| 사용 환경 | 변경 적은 경우 | **싱글 스레드** 문자열 조작 | **멀티 스레드** 문자열 조작 |

```java
// StringBuffer — 메서드에 synchronized 키워드
public synchronized StringBuffer append(String str) {
    // ...
}

// StringBuilder — synchronized 없음
public StringBuilder append(String str) {
    // ...
}
```

### 선택 기준

```
문자열 변경이 거의 없다       → String
싱글 스레드에서 빈번한 조작    → StringBuilder (대부분의 경우)
멀티 스레드에서 빈번한 조작    → StringBuffer
```

실무에서는 **대부분 StringBuilder**를 사용한다. 멀티 스레드 환경에서도 지역 변수로 사용하면 스레드 간 공유가 안 되므로 StringBuilder로 충분하다.

## 관련 문서

- [[JVM]] — String Pool이 위치하는 Heap 영역
- [[GC]] — 불필요한 String 객체의 GC 수거
- [[OOP-4가지-특징]] — 불변 객체와 캡슐화
