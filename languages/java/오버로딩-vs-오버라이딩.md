---
tags: [java, overloading, overriding, polymorphism]
status: completed
created: 2026-04-14
---

# 오버로딩 vs 오버라이딩

## 핵심 개념

**오버로딩(Overloading)**은 같은 이름의 메서드를 매개변수를 다르게 하여 여러 개 정의하는 것이고, **오버라이딩(Overriding)**은 부모 클래스의 메서드를 자식 클래스에서 재정의하는 것이다. 오버로딩은 **컴파일 타임(정적 바인딩)**, 오버라이딩은 **런타임(동적 바인딩)**에 결정된다.

## 동작 원리

### 오버로딩 (Overloading)

같은 이름, **다른 매개변수(타입, 개수, 순서)**로 여러 메서드를 정의한다. 컴파일러가 **호출 시점의 인자 타입/개수**를 보고 어떤 메서드를 호출할지 결정한다.

```java
public class Calculator {
    int add(int a, int b) { return a + b; }           // int 2개
    int add(int a, int b, int c) { return a + b + c; } // int 3개
    double add(double a, double b) { return a + b; }   // double 2개
}

calculator.add(1, 2);       // → add(int, int) 호출
calculator.add(1, 2, 3);   // → add(int, int, int) 호출
calculator.add(1.0, 2.0);  // → add(double, double) 호출
```

> [!warning] 반환 타입만 다르면 오버로딩이 아니다
> `int add(int a, int b)`와 `double add(int a, int b)`는 매개변수가 동일하므로 컴파일 에러. 반환 타입은 오버로딩 구분 기준이 아니다.

### 오버라이딩 (Overriding)

부모 클래스의 메서드를 자식 클래스에서 **같은 시그니처**로 재정의한다. 런타임에 실제 객체 타입에 따라 어떤 구현이 실행될지 결정된다.

```java
class Animal {
    void sound() { System.out.println("..."); }
}

class Dog extends Animal {
    @Override
    void sound() { System.out.println("멍멍"); }
}

class Cat extends Animal {
    @Override
    void sound() { System.out.println("야옹"); }
}
```

### 정적 바인딩 vs 동적 바인딩

<div style="font-family: -apple-system, sans-serif; max-width: 540px; margin: 12px auto; font-size: 12px;">
<div style="display: flex; gap: 8px;">
<div style="flex: 1; border: 2px solid #2980B9; border-radius: 8px; padding: 12px;">
<div style="background: #2980B9; color: white; padding: 6px; border-radius: 4px; text-align: center; font-weight: bold; margin-bottom: 8px;">정적 바인딩 (오버로딩)</div>
<div style="font-size: 11px; line-height: 1.8; color: #333;">
<b>시점:</b> 컴파일 타임<br/>
<b>기준:</b> 매개변수 타입/개수<br/>
<b>결정 주체:</b> 컴파일러<br/>
<span style="font-family: monospace; font-size: 10px;">
add(1, 2) → 컴파일러가<br/>
add(int, int) 선택 확정
</span>
</div>
</div>
<div style="flex: 1; border: 2px solid #E74C3C; border-radius: 8px; padding: 12px;">
<div style="background: #E74C3C; color: white; padding: 6px; border-radius: 4px; text-align: center; font-weight: bold; margin-bottom: 8px;">동적 바인딩 (오버라이딩)</div>
<div style="font-size: 11px; line-height: 1.8; color: #333;">
<b>시점:</b> 런타임<br/>
<b>기준:</b> 실제 객체 타입<br/>
<b>결정 주체:</b> JVM<br/>
<span style="font-family: monospace; font-size: 10px;">
Animal a = new Dog();<br/>
a.sound() → JVM이 런타임에<br/>
Dog.sound() 실행
</span>
</div>
</div>
</div>
</div>

동적 바인딩이 바로 **다형성(Polymorphism)**의 핵심 메커니즘이다. 컴파일 타임에는 `Animal.sound()`로 보이지만, 런타임에 실제 객체 타입(`Dog`)의 구현이 실행된다.

### 오버로딩 vs 오버라이딩 비교

| | 오버로딩 (Overloading) | 오버라이딩 (Overriding) |
|---|---|---|
| **위치** | 같은 클래스 내 | 상속 관계 (부모 → 자식) |
| **메서드 이름** | 동일 | 동일 |
| **매개변수** | **달라야** 함 | **같아야** 함 |
| **반환 타입** | 무관 | 같거나 하위 타입 (공변 반환) |
| **접근 제어자** | 무관 | 같거나 **더 넓어야** 함 |
| **바인딩** | 정적 (컴파일 타임) | 동적 (런타임) |
| **다형성** | 컴파일 타임 다형성 | 런타임 다형성 |

### 오버라이딩 제약 조건

```
부모: protected void process() throws IOException
자식: public void process() throws FileNotFoundException ← OK

1. 접근 제어자: 같거나 넓게 (protected → public ✓)
2. 예외: 같거나 좁게 (IOException → FileNotFoundException ✓)
3. static, final, private 메서드는 오버라이딩 불가
```

## 코드 예시

```java
// 다형성 활용 — 오버라이딩의 핵심 가치
List<Animal> animals = List.of(new Dog(), new Cat(), new Dog());

for (Animal a : animals) {
    a.sound();  // 런타임에 실제 타입에 따라 다른 메서드 실행
}
// 출력: 멍멍 / 야옹 / 멍멍

// 새 동물 추가 시 Animal을 상속하고 sound()만 오버라이드하면 됨
// → 기존 코드(for문) 수정 없이 확장 가능 (OCP 원칙)
```

## 관련 문서

- [[OOP-4가지-특징]] — 다형성과 오버라이딩의 관계
- [[Interface-vs-Abstract-Class]] — 추상 메서드 오버라이딩
- [[SOLID]] — OCP(개방-폐쇄 원칙)와 다형성
