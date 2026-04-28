---
tags: [java, oop, solid]
status: completed
created: 2026-03-30
---

# SOLID 원칙

## 핵심 개념

**SOLID**는 객체지향 설계의 5가지 원칙이다. 유지보수성, 확장성, 유연성을 높이기 위한 설계 가이드라인으로, 각 원칙이 상호 보완적으로 작용한다.

## 동작 원리

### SRP — Single Responsibility Principle (단일 책임 원칙)

**클래스가 변경되는 이유는 하나여야 한다.**

"하나의 일만 해야 한다"보다 "변경 이유가 하나"가 더 정확한 정의다. 변경 이유가 여러 개면 한 가지 변경이 다른 기능에 영향을 줄 수 있다.

```java
// 위반: 주문 처리 + 이메일 발송 두 가지 변경 이유
public class OrderService {
    public void createOrder(Order order) { /* 주문 생성 */ }
    public void sendEmail(Order order) { /* 이메일 발송 */ }
}

// 준수: 각각 하나의 변경 이유만 가짐
public class OrderService {
    public void createOrder(Order order) { /* 주문 생성 */ }
}

public class OrderNotificationService {
    public void sendEmail(Order order) { /* 이메일 발송 */ }
}
```

### OCP — Open/Closed Principle (개방-폐쇄 원칙)

**확장에는 열려 있고, 변경에는 닫혀 있어야 한다.**

기존 코드를 수정하지 않고 새로운 기능을 추가할 수 있어야 한다. 인터페이스/추상 클래스를 활용하여 구현한다.

```java
// 위반: 새 할인 정책 추가 시 기존 코드 수정 필요
public double discount(String type, double price) {
    if (type.equals("VIP")) return price * 0.8;
    if (type.equals("GOLD")) return price * 0.9;
    // 새 타입 추가 시 여기를 수정해야 함
    return price;
}

// 준수: 새 정책은 구현체만 추가하면 됨
public interface DiscountPolicy {
    double discount(double price);
}

public class VipDiscount implements DiscountPolicy {
    public double discount(double price) { return price * 0.8; }
}

public class GoldDiscount implements DiscountPolicy {
    public double discount(double price) { return price * 0.9; }
}
// 새 할인 정책 → 새 클래스만 추가, 기존 코드 수정 없음
```

### LSP — Liskov Substitution Principle (리스코프 치환 원칙)

**자식 클래스가 부모 클래스를 대체해도 프로그램이 정상 동작해야 한다.**

부모 클래스의 계약(사전/사후 조건)을 자식이 깨뜨리면 안 된다.

```java
// 위반: Rectangle을 상속한 Square가 부모의 계약을 깨뜨림
public class Rectangle {
    protected int width, height;
    public void setWidth(int w) { this.width = w; }
    public void setHeight(int h) { this.height = h; }
    public int area() { return width * height; }
}

public class Square extends Rectangle {
    @Override
    public void setWidth(int w) {
        this.width = w;
        this.height = w;  // width 설정했는데 height까지 바뀜 → 부모 계약 위반
    }
}

// Rectangle r = new Square();
// r.setWidth(5); r.setHeight(3);
// r.area() → 기대값 15, 실제값 9 → LSP 위반
```

### ISP — Interface Segregation Principle (인터페이스 분리 원칙)

**범용 인터페이스 하나보다 특정 역할별 인터페이스 여러 개가 낫다.**

클라이언트가 자신이 사용하지 않는 메서드에 의존하지 않아야 한다.

```java
// 위반: 모든 기능을 하나의 인터페이스에 몰아넣음
public interface Worker {
    void work();
    void eat();
    void sleep();
}
// 로봇은 eat(), sleep()을 구현할 필요 없는데 강제됨

// 준수: 역할별로 분리
public interface Workable { void work(); }
public interface Eatable { void eat(); }
public interface Sleepable { void sleep(); }

public class Human implements Workable, Eatable, Sleepable { ... }
public class Robot implements Workable { ... }  // 필요한 것만 구현
```

### DIP — Dependency Inversion Principle (의존 역전 원칙)

**고수준 모듈이 저수준 모듈에 직접 의존하지 않고, 둘 다 추상화에 의존해야 한다.**

> [!note] DIP vs DI
> **DIP (Dependency Inversion Principle)** — 설계 원칙. "추상화에 의존하라."
> **DI (Dependency Injection)** — 구현 기법. 외부에서 의존성을 주입하는 방식. DIP를 구현하는 수단 중 하나.

```java
// 위반: 고수준(OrderService)이 저수준(MySQLRepository)에 직접 의존
public class OrderService {
    private MySQLRepository repo = new MySQLRepository();  // 구체 클래스 의존
}

// 준수: 둘 다 추상화(인터페이스)에 의존
public interface OrderRepository {
    void save(Order order);
}

public class MySQLRepository implements OrderRepository { ... }
public class MongoRepository implements OrderRepository { ... }

public class OrderService {
    private final OrderRepository repo;  // 추상화에 의존

    public OrderService(OrderRepository repo) {  // DI로 주입
        this.repo = repo;
    }
}
```

```
의존 역전 전:                    의존 역전 후:
OrderService → MySQLRepo      OrderService → OrderRepository ← MySQLRepo
(고수준 → 저수준)                (둘 다 추상화에 의존)
```

### SOLID 원칙 간 관계

```
SRP  하나의 변경 이유 → 클래스를 작게 유지
 ↓
OCP  변경 없이 확장 → 인터페이스/추상화 활용
 ↓
LSP  자식이 부모 대체 가능 → 올바른 상속 설계
 ↓
ISP  인터페이스 분리 → 불필요한 의존 제거
 ↓
DIP  추상화에 의존 → 유연한 구조 완성
```

## 관련 문서

- [[OOP-4가지-특징]]
- [[Interface-vs-Abstract-Class]]
- [[접근제어자]]
