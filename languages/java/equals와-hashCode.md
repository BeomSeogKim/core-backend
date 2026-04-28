---
tags: [java, equals, hashcode]
status: completed
created: 2026-04-02
---

# == vs equals() 와 hashCode()

## 핵심 개념

**== 연산자**는 참조(주소) 비교, **equals() 메서드**는 논리적 동등성 비교다. equals()를 오버라이드하면 반드시 hashCode()도 함께 오버라이드해야 하며, 이를 어기면 HashMap 등 해시 기반 컬렉션에서 오동작한다.

## 동작 원리

### == 연산자

두 참조 변수가 **같은 객체(같은 메모리 주소)**를 가리키는지 비교한다.

```java
String a = new String("hello");
String b = new String("hello");

System.out.println(a == b);       // false — 서로 다른 객체
System.out.println(a.equals(b));  // true  — 값이 같음
```

```
Heap:
  0x100: "hello"  ← a가 참조
  0x200: "hello"  ← b가 참조
  a == b → 0x100 != 0x200 → false
```

기본 타입(primitive)에서는 **값 자체**를 비교한다:
```java
int x = 5;
int y = 5;
System.out.println(x == y);  // true — 값 비교
```

### equals() 메서드

Object 클래스의 기본 구현은 `==`과 동일하다. 논리적 동등성 비교를 위해 **오버라이드**해야 한다.

```java
// Object의 기본 구현
public boolean equals(Object obj) {
    return (this == obj);  // 참조 비교와 동일
}

// 오버라이드 — 논리적 동등성 비교
public class User {
    private String name;
    private int age;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return age == user.age && Objects.equals(name, user.name);
    }
}
```

### hashCode() — equals()와 반드시 함께 오버라이드

**계약**: `equals()`가 true인 두 객체는 반드시 **같은 hashCode()**를 반환해야 한다.

#### 왜 함께 오버라이드해야 하는가?

HashMap이 객체를 찾는 과정:

```
1. hashCode()로 버킷 위치 결정
2. 해당 버킷에서 equals()로 실제 키 비교

┌───────────────────────────────────────────────┐
│ map.get(key)                                   │
│                                               │
│ Step 1: hashCode() → 버킷 인덱스 결정           │
│         hash("홍길동") → 버킷 3번               │
│                                               │
│ Step 2: 버킷 3번에서 equals()로 키 매칭          │
│         [홍길동:데이터] → equals() true → 반환   │
└───────────────────────────────────────────────┘
```

**equals()만 오버라이드하고 hashCode()를 안 하면:**

```java
User u1 = new User("홍길동", 25);
User u2 = new User("홍길동", 25);

u1.equals(u2);  // true — equals 오버라이드했으니까

Map<User, String> map = new HashMap<>();
map.put(u1, "데이터");
map.get(u2);  // null! — hashCode가 달라서 다른 버킷을 탐색
```

```
u1.hashCode() → 버킷 3  ← put은 여기에 저장
u2.hashCode() → 버킷 7  ← get은 여기서 찾음 → 못 찾음!

equals()는 true인데 서로 다른 버킷 → HashMap에서 찾지 못함
```

```java
// 올바른 구현 — equals()와 hashCode() 함께 오버라이드
@Override
public int hashCode() {
    return Objects.hash(name, age);  // equals에서 사용한 같은 필드로 계산
}
```

### hashCode 계약 정리

| 규칙 | 설명 |
|------|------|
| `equals()가 true` → `hashCode()` 같아야 함 | **필수** — 위반 시 HashMap 오동작 |
| `hashCode()` 같음 → `equals()가 true`일 필요 없음 | 해시 충돌은 허용됨 |
| `equals()가 false` → `hashCode()` 달라야 할 필요 없음 | 다르면 성능에 유리할 뿐 |

## 관련 문서

- [[HashMap-HashTable-ConcurrentHashMap]]
- [[String-심화]] — String의 hashCode 캐싱
- [[Collections-Framework]]
