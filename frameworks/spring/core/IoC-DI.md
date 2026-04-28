---
tags: [spring, ioc, di]
status: completed
created: 2026-04-02
---

# IoC와 DI

## 핵심 개념

**IoC(Inversion of Control, 제어의 역전)**는 객체의 생성과 의존관계 설정을 개발자가 아닌 프레임워크가 담당하는 설계 원칙이다. **DI(Dependency Injection, 의존성 주입)**는 IoC를 구현하는 핵심 기법으로, 외부에서 의존 객체를 주입한다.

## 동작 원리

### IoC — 제어의 역전

```java
// IoC 없이 — 개발자가 직접 제어
public class OrderService {
    private OrderRepository repo = new MySQLRepository();  // 직접 생성, 직접 선택
}

// IoC 적용 — 프레임워크가 제어
public class OrderService {
    private final OrderRepository repo;  // 무엇이 들어올지 모름

    public OrderService(OrderRepository repo) {
        this.repo = repo;  // 외부(Spring)가 결정하여 주입
    }
}
```

제어의 주체가 **개발자 → 프레임워크**로 역전된다. 어떤 구현체를 사용할지, 언제 객체를 생성할지를 Spring이 결정한다.

### DI — 3가지 주입 방식

#### 1. 생성자 주입 (권장)

```java
@Service
public class OrderService {
    private final OrderRepository repo;

    // 생성자가 하나면 @Autowired 생략 가능
    public OrderService(OrderRepository repo) {
        this.repo = repo;
    }
}
```

**권장 이유:**
- **불변성** — `final` 필드로 선언 가능. 한번 주입 후 변경 불가
- **테스트 용이** — 생성자에 Mock 객체를 직접 전달 가능
- **순환 참조 감지** — 컴파일/기동 시점에 순환 참조를 발견할 수 있음 (필드/수정자는 런타임에 발견)
- **NPE 방지** — 객체 생성 시점에 모든 의존성이 보장됨

#### 2. 수정자 주입 (Setter)

```java
@Service
public class OrderService {
    private OrderRepository repo;

    @Autowired
    public void setRepo(OrderRepository repo) {
        this.repo = repo;
    }
}
```

- 선택적 의존성에 사용 가능
- 변경 가능하므로 불변성 보장 안 됨

#### 3. 필드 주입

```java
@Service
public class OrderService {
    @Autowired
    private OrderRepository repo;
}
```

- 코드가 간결하지만 테스트 시 DI 프레임워크 없이 주입 어려움
- `final` 선언 불가
- 순환 참조를 숨길 수 있어 권장되지 않음

## 관련 문서

- [[Bean]]
- [[Autowired]]
- [[AOP]]
- [[SOLID]] — DIP(의존 역전 원칙)와 DI의 관계
