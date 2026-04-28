---
tags: [spring, aop, proxy, self-invocation, cglib]
status: completed
created: 2026-04-24
---

# AOP Proxy 방식의 단점

## 핵심 개념

Spring AOP는 런타임에 **프록시 객체**를 생성하여 Advice를 적용한다. 이 프록시 기반 구조 때문에 발생하는 본질적인 한계가 있다.

## 동작 원리

Spring AOP의 프록시 동작:

```
외부 호출:
Client ──→ [Proxy (Advice 실행)] ──→ [실제 객체.method()]
                                         ✅ AOP 적용

내부 호출 (Self-invocation):
[실제 객체.methodA()] ──→ this.methodB()  ← Proxy 미경유
                                              ❌ AOP 미적용
```

## 한계 목록

### 1. Self-invocation (가장 중요)

같은 클래스 내에서 AOP 대상 메서드를 직접 호출하면 Proxy를 거치지 않아 AOP가 적용되지 않는다.

```java
@Service
public class OrderService {

    @Transactional
    public void createOrder() {
        // ...
        validate();  // ← this.validate() 와 동일. Proxy 미경유
    }

    @Transactional(propagation = REQUIRES_NEW)
    public void validate() {
        // @Transactional이 적용되지 않는다
    }
}
```

`validate()`의 `@Transactional`이 무시되고 `createOrder()`의 트랜잭션 안에서 그냥 실행된다.

**해결**: 별도 Bean으로 분리하거나, ApplicationContext에서 Self-lookup으로 Proxy를 통해 호출한다.

```java
// 해결책 1: 별도 Bean으로 분리
@Service
public class OrderValidator {
    @Transactional(propagation = REQUIRES_NEW)
    public void validate() { ... }
}

// 해결책 2: Self-lookup (권장하지 않지만 불가피할 때)
@Service
public class OrderService {
    @Autowired
    private OrderService self;  // 자기 자신의 Proxy 주입

    public void createOrder() {
        self.validate();  // Proxy 통해 호출
    }
}
```

### 2. private 메서드 미적용

Proxy는 대상 클래스를 상속하거나 인터페이스를 구현하는 방식이므로 `private` 메서드는 override할 수 없다.

```java
@Service
public class OrderService {
    @Transactional
    private void saveOrder() { ... }  // ← AOP 미적용
}
```

### 3. final 클래스/메서드 (CGLIB 한계)

CGLIB는 클래스를 **상속**하여 Proxy를 생성한다. `final` 클래스나 `final` 메서드는 상속/override가 불가능하므로 AOP를 적용할 수 없다.

```java
@Service
public final class OrderService {  // ← CGLIB Proxy 생성 불가
    ...
}
```

### 4. 메서드 레벨만 가능

Spring AOP는 메서드 실행(`execution`) 포인트컷만 지원한다. 필드 접근, 생성자 호출 등에는 AOP를 적용할 수 없다.

AspectJ는 컴파일/로드 타임 위빙으로 이 한계를 극복하지만 설정이 복잡하다.

| | Spring AOP | AspectJ |
|---|---|---|
| 위빙 시점 | 런타임 (프록시) | 컴파일/로드 타임 |
| 대상 | 메서드 실행만 | 필드, 생성자, 메서드 모두 |
| Spring Bean | Spring Bean만 | 모든 Java 객체 |
| 설정 복잡도 | 낮음 | 높음 |

## Self-invocation과 @Transactional 전파

Self-invocation은 `@Transactional` 전파 수준(propagation)에도 영향을 준다.

```java
@Transactional
public void outer() {
    inner();  // REQUIRES_NEW여도 같은 트랜잭션에서 실행됨
}

@Transactional(propagation = REQUIRES_NEW)
public void inner() { ... }  // Proxy 미경유 → 전파 설정 무시
```

별도 Bean으로 분리해야 `REQUIRES_NEW`가 의도대로 동작한다.

## 관련 문서

- [[AOP]]
- [[Transactional]]
- [[순환참조]]
