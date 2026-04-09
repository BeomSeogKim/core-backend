---
tags: [spring, autowired, di]
status: completed
created: 2026-04-02
---

# @Autowired 동작 원리

## 핵심 개념

**@Autowired**는 Spring이 Bean 간의 의존관계를 자동으로 주입하는 어노테이션이다. Bean 생성 후 **BeanPostProcessor**(`AutowiredAnnotationBeanPostProcessor`)가 `@Autowired`가 붙은 지점을 찾아 적절한 Bean을 주입한다.

## 동작 원리

### 내부 처리 흐름

```
Bean 객체 생성
    ↓
AutowiredAnnotationBeanPostProcessor 동작
    ↓
@Autowired 필드/생성자/수정자 탐색
    ↓
타입(Type)으로 매칭되는 Bean 조회
    ↓
주입
```

### 동일 타입 Bean이 여러 개인 경우

같은 타입의 Bean이 2개 이상 등록되면 `NoUniqueBeanDefinitionException`이 발생한다. 해결 방법 3가지:

#### 1. @Qualifier — 주입 시점에 지정

```java
@Autowired
@Qualifier("mysqlRepo")
private OrderRepository repo;
```

#### 2. @Primary — Bean 정의 시 우선순위 지정

```java
@Bean @Primary
public OrderRepository mysqlRepo() {
    return new MySQLRepository();
}

@Bean
public OrderRepository mongoRepo() {
    return new MongoRepository();
}

// @Primary가 붙은 mysqlRepo가 우선 주입됨
```

#### 3. 필드명 매칭

변수명이 Bean 이름과 일치하면 자동으로 해당 Bean이 주입된다.

```java
@Autowired
private OrderRepository mysqlRepo;  // Bean 이름이 "mysqlRepo"인 것과 매칭
```

### 우선순위

`@Qualifier` > `@Primary` > 필드명 매칭

## 관련 문서

- [[IoC-DI]]
- [[Bean]]
- [[AOP]] — BeanPostProcessor와 프록시 생성
