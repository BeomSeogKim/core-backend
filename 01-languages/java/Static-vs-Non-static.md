---
tags: [java, static, class-variable, instance-variable]
status: completed
created: 2026-04-14
---

# Static vs Non-static

## 핵심 개념

**static 멤버**는 클래스에 속하며 클래스 로딩 시점에 한 번만 생성되고, **non-static 멤버**는 인스턴스에 속하며 인스턴스 생성 시마다 각각 생성된다. static 멤버는 모든 인스턴스가 공유하고, non-static 멤버는 인스턴스별로 독립적이다.

## 동작 원리

### 메모리 구조

```
JVM 메모리
┌──────────────────────────────────────────────┐
│  Method Area (클래스 로딩 시 한 번 적재)          │
│  ┌─────────────────────────────────────────┐ │
│  │ MyClass.class                           │ │
│  │  static int count = 0    ← 클래스 변수    │ │
│  │  static void util()     ← 클래스 메서드   │ │
│  └─────────────────────────────────────────┘ │
├──────────────────────────────────────────────┤
│  Heap (인스턴스 생성 시마다)                      │
│  ┌──────────────┐  ┌──────────────┐          │
│  │ MyClass obj1 │  │ MyClass obj2 │          │
│  │ name = "A"   │  │ name = "B"   │          │
│  │ age = 20     │  │ age = 30     │          │
│  └──────────────┘  └──────────────┘          │
│   인스턴스 변수는      인스턴스마다 독립             │
└──────────────────────────────────────────────┘
```

### static 멤버

- **클래스 로딩 시점**에 초기화 → 애플리케이션 동안 단 하나만 존재
- 모든 인스턴스가 **공유** → 인스턴스 없이도 `ClassName.member`로 접근 가능
- `this` 키워드 사용 불가 (인스턴스 컨텍스트가 아니므로)

### non-static 멤버

- **인스턴스 생성 시(`new`)** 초기화 → 인스턴스마다 독립적으로 존재
- 인스턴스를 통해서만 접근 가능
- `this`로 현재 인스턴스 참조

### 핵심 제약: static → non-static 접근 불가

```java
class MyClass {
    int instanceVar = 10;        // non-static

    static void staticMethod() {
        // instanceVar 접근 불가 — 컴파일 에러
        // 이유: static 메서드는 클래스 로딩 시 존재하지만,
        //       instanceVar는 인스턴스가 생성돼야 존재
        System.out.println(instanceVar);  // ✗ 컴파일 에러
    }

    void instanceMethod() {
        staticMethod();  // ✓ non-static → static 접근은 가능
    }
}
```

<div style="font-family: -apple-system, sans-serif; max-width: 480px; margin: 12px auto; font-size: 12px;">
<div style="display: flex; gap: 4px;">
<div style="flex: 1; background: #2980B9; color: white; padding: 10px; border-radius: 6px;">
<div style="font-weight: bold;">클래스 로딩 시점</div>
<div style="font-size: 11px; margin-top: 6px;">static 멤버 존재 ✓</div>
<div style="font-size: 11px;">non-static 멤버 ✗ (아직 없음)</div>
<div style="font-size: 11px; color: #AED6F1; margin-top: 4px;">→ static에서 non-static 접근 불가</div>
</div>
<div style="flex: 1; background: #27AE60; color: white; padding: 10px; border-radius: 6px;">
<div style="font-weight: bold;">인스턴스 생성 후</div>
<div style="font-size: 11px; margin-top: 6px;">static 멤버 존재 ✓</div>
<div style="font-size: 11px;">non-static 멤버 존재 ✓</div>
<div style="font-size: 11px; color: #A9DFBF; margin-top: 4px;">→ non-static에서 static 접근 가능</div>
</div>
</div>
</div>

### 비교 정리

| | static | non-static |
|---|---|---|
| **소속** | 클래스 | 인스턴스 |
| **생성 시점** | 클래스 로딩 시 | `new` 연산자 실행 시 |
| **개수** | 클래스당 1개 | 인스턴스마다 1개 |
| **접근 방법** | `ClassName.member` | `instance.member` |
| **메모리 위치** | Method Area | Heap |
| **`this` 사용** | 불가 | 가능 |

### main이 static인 이유

```java
public static void main(String[] args) { ... }
```

`main()`은 JVM이 **인스턴스를 만들기 전에** 호출해야 하므로 `static`이다. 클래스 로딩 후 인스턴스 생성 없이 바로 실행 가능해야 하기 때문이다.

## 코드 예시

```java
class Counter {
    static int totalCount = 0;   // 모든 인스턴스가 공유
    int instanceCount = 0;       // 인스턴스별 독립

    Counter() {
        totalCount++;     // 전체 카운트 증가
        instanceCount++;  // 항상 1
    }
}

Counter c1 = new Counter();  // totalCount=1, c1.instanceCount=1
Counter c2 = new Counter();  // totalCount=2, c2.instanceCount=1
Counter c3 = new Counter();  // totalCount=3, c3.instanceCount=1

// static 변수는 클래스명으로 접근
System.out.println(Counter.totalCount);  // 3
```

## 관련 문서

- [[JVM]] — Method Area와 Heap의 메모리 구조
- [[Class]] — 클래스 로딩 과정
- [[OOP-4가지-특징]] — 캡슐화와 static의 관계
