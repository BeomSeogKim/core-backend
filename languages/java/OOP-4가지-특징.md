---
tags: [java, oop]
status: completed
created: 2026-03-30
---

# OOP 4가지 특징

## 핵심 개념

**객체지향 프로그래밍(OOP)**의 4가지 핵심 특징은 **추상화, 다형성, 캡슐화, 상속**이다. 이 네 가지가 결합되어 유지보수성과 재사용성이 높은 코드를 작성할 수 있게 한다.

## 동작 원리

### 추상화 (Abstraction)

객체들의 **공통 속성과 행위를 추출**하여 상위 개념으로 정의하는 것. 불필요한 세부 구현을 감추고 핵심 특성만 노출한다.

```java
// 공통 속성/행위를 추상화
public abstract class Animal {
    protected String name;
    public abstract void speak();  // 무엇을 말하는지는 하위 클래스가 결정
}
```

Java에서의 구현 수단: `abstract class`, `interface`

### 다형성 (Polymorphism)

**같은 타입(인터페이스/부모 클래스)으로 여러 다른 객체를 다룰 수 있는 능력.** 코드를 변경하지 않고 동작을 바꿀 수 있어 유연성과 확장성의 핵심이 된다.

#### 오버라이딩 (Runtime Polymorphism)

상속 관계에서 부모의 메서드를 자식이 **재정의**. 런타임에 실제 객체의 메서드가 호출된다.

```java
Animal animal = new Dog();   // 부모 타입으로 자식 객체 참조
animal.speak();              // 런타임에 Dog의 speak() 실행 (동적 바인딩)

Animal animal2 = new Cat();
animal2.speak();             // 런타임에 Cat의 speak() 실행
```

같은 `animal.speak()` 코드인데 **객체에 따라 다른 동작** → 다형성의 핵심.

#### 오버로딩 (Compile-time Polymorphism)

같은 이름의 메서드를 **매개변수를 다르게** 하여 여러 개 정의. 컴파일 타임에 결정된다.

```java
public int add(int a, int b) { return a + b; }
public double add(double a, double b) { return a + b; }
public int add(int a, int b, int c) { return a + b + c; }
```

### 캡슐화 (Encapsulation)

**내부 구현을 숨기고 외부에는 메서드(인터페이스)만 제공하는 것.** 접근제어자(`private`, `protected`, `public`)로 접근 범위를 제어하여, 내부 변경이 외부에 영향을 주지 않도록 한다.

```java
public class Account {
    private int balance;  // 외부에서 직접 접근 불가

    public void deposit(int amount) {   // 메서드를 통해서만 조작
        if (amount > 0) {
            this.balance += amount;
        }
    }

    public int getBalance() {           // 읽기도 메서드를 통해
        return this.balance;
    }
}
```

- `balance`를 `private`으로 숨기고 `deposit()`, `getBalance()`만 제공
- 내부 검증 로직(`amount > 0`)을 캡슐 안에 포함
- 나중에 `balance` 저장 방식이 바뀌어도 외부 코드는 변경 불필요

### 상속 (Inheritance)

**상위 클래스의 속성과 메서드를 하위 클래스가 물려받아 재사용하거나 재정의(오버라이딩)**하는 것.

```java
public class Dog extends Animal {
    @Override
    public void speak() {
        System.out.println("멍멍");  // 재정의
    }

    public void fetch() {
        System.out.println("공 가져오기");  // 확장
    }
}
```

- **재사용**: 부모의 공통 기능을 다시 작성하지 않아도 됨
- **재정의**: `@Override`로 부모 메서드를 자식에 맞게 변경
- **확장**: 자식만의 고유 기능 추가 가능
- Java는 **단일 상속**만 허용 (`extends` 하나)

### 4가지 특징의 관계

```
추상화 ──→ 공통 속성/행위 추출 (설계)
  │
  ├─ 상속 ──→ 추상화된 것을 물려받아 재사용/확장
  │    │
  │    └─ 다형성 ──→ 상속/구현을 통해 같은 타입으로 다른 동작
  │
  └─ 캡슐화 ──→ 내부 구현 숨기고 인터페이스만 노출
```

## 관련 문서

- [[SOLID]]
- [[Interface-vs-Abstract-Class]]
- [[접근제어자]]
