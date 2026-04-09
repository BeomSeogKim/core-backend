---
tags: [java, oop, interface, abstract]
status: completed
created: 2026-03-30
---

# Interface vs Abstract Class

## 핵심 개념

**Interface**는 행동의 계약(무엇을 해야 하는지)을 정의하고, **Abstract Class**는 공통 기능과 상태를 제공하면서 하위 클래스에서 확장하도록 한다. 설계 의도와 목적이 다르다.

## 동작 원리

### 구현 관점 비교

| | Interface | Abstract Class |
|---|---|---|
| 다중 상속 | **여러 개** 구현 가능 | **하나만** 상속 |
| 인스턴스 변수 | 불가 (`public static final`만) | **가능** |
| 생성자 | 없음 | **있음** |
| 접근 제어자 | `public`만 (Java 9+ `private` 메서드 허용) | 모든 접근 제어자 가능 |
| default 메서드 | Java 8+ 가능 | — (일반 메서드로 제공) |
| 추상 메서드 | 기본이 추상 | `abstract` 키워드 필요 |

### Interface — 행동 계약 정의

"**무엇을 해야 하는가**"를 정의. 구현 방법은 강제하지 않는다.

```java
public interface Flyable {
    void fly();  // 어떻게 나는지는 구현체가 결정
}

public interface Swimmable {
    void swim();
}

// 여러 행동을 조합 가능 (다중 구현)
public class Duck implements Flyable, Swimmable {
    @Override public void fly() { System.out.println("날개로 비행"); }
    @Override public void swim() { System.out.println("물에서 수영"); }
}
```

#### Java 8+ default 메서드

인터페이스에 기본 구현을 제공할 수 있게 되었다. 기존 구현체를 깨뜨리지 않고 인터페이스에 메서드를 추가할 수 있다.

```java
public interface Loggable {
    default void log(String message) {
        System.out.println("[LOG] " + message);  // 기본 구현 제공
    }
}

// 구현체는 log()를 오버라이드하지 않아도 사용 가능
public class UserService implements Loggable {
    public void createUser() {
        log("사용자 생성");  // default 메서드 호출
    }
}
```

### Abstract Class — 공통 기능 + 상태 제공

"**공통 기능을 제공하면서** 일부는 하위 클래스가 구현하도록 강제한다."

```java
public abstract class HttpClient {
    protected String baseUrl;      // 상태(인스턴스 변수) 보유
    protected int timeout = 3000;

    public HttpClient(String baseUrl) {  // 생성자로 초기화
        this.baseUrl = baseUrl;
    }

    // 공통 기능 제공
    public String buildUrl(String path) {
        return baseUrl + path;
    }

    // 하위 클래스가 반드시 구현
    public abstract String execute(String url);
}

public class OkHttpClient extends HttpClient {
    public OkHttpClient(String baseUrl) { super(baseUrl); }

    @Override
    public String execute(String url) {
        // OkHttp 라이브러리로 구현
        return "response";
    }
}
```

### 선택 기준

```
"행동(계약)을 정의하고 싶다"              → Interface
"여러 타입의 행동을 조합하고 싶다"         → Interface (다중 구현)
"공통 상태(필드)와 기본 구현을 제공하고 싶다" → Abstract Class
"생성자로 초기화 로직이 필요하다"           → Abstract Class
"is-a 관계가 명확하다"                   → Abstract Class
"can-do 관계다"                         → Interface
```

## 관련 문서

- [[OOP-4가지-특징]]
- [[SOLID]]
- [[접근제어자]]
