---
tags: [spring, bean, lifecycle, scope]
status: completed
created: 2026-04-02
---

# Bean, 생명주기, Scope

## 핵심 개념

**Spring Bean**은 Spring IoC 컨테이너(ApplicationContext)가 관리하는 객체다. 컨테이너가 Bean의 생성, 의존관계 주입, 초기화, 소멸까지의 **생명주기**를 관리하며, **Scope**에 따라 객체의 생존 범위가 결정된다.

## 동작 원리

### Bean 등록 방식

```java
// 1. 컴포넌트 스캔 — 클래스에 어노테이션
@Service
public class OrderService { ... }

// 2. 직접 등록 — @Configuration + @Bean
@Configuration
public class AppConfig {
    @Bean
    public OrderService orderService() {
        return new OrderService(orderRepository());
    }
}
```

### Bean 생명주기

```
스프링 컨테이너 생성
    ↓
Bean 객체 생성 (new)
    ↓
의존관계 주입 (@Autowired, 생성자 주입 등)
    ↓
초기화 콜백 (@PostConstruct)
    ↓
사용
    ↓
소멸 전 콜백 (@PreDestroy)
    ↓
스프링 컨테이너 종료 → Bean 소멸
```

#### 초기화 / 소멸 콜백 방법 3가지

| 방법 | 초기화 | 소멸 | 특징 |
|------|--------|------|------|
| **어노테이션 (권장)** | `@PostConstruct` | `@PreDestroy` | 가장 간편, 스프링 권장 |
| 인터페이스 | `InitializingBean.afterPropertiesSet()` | `DisposableBean.destroy()` | 스프링 인터페이스 의존 |
| @Bean 속성 | `@Bean(initMethod="init")` | `@Bean(destroyMethod="close")` | 외부 라이브러리에 유용 |

```java
@Component
public class DataLoader {

    @PostConstruct
    public void init() {
        // 의존관계 주입 완료 후 실행 — DB 연결, 캐시 워밍 등
    }

    @PreDestroy
    public void cleanup() {
        // 컨테이너 종료 전 실행 — 리소스 해제, 커넥션 반환 등
    }
}
```

> [!note] 생성과 초기화를 분리하는 이유
> 객체 생성(메모리 할당)과 초기화(외부 리소스 연결 등)는 관심사가 다르다. 생성자에서는 가볍게 객체만 만들고, 무거운 초기화 작업은 `@PostConstruct`에서 수행하는 것이 좋다.

### Bean Scope

| Scope | 생명주기 | 특징 |
|---|---|---|
| **Singleton** (기본) | 컨테이너당 **하나** | 애플리케이션 종료까지 유지 |
| **Prototype** | 요청마다 **새로 생성** | 컨테이너가 소멸 관리 안 함 (`@PreDestroy` 호출 안 됨) |
| **Request** | HTTP 요청마다 생성 | 요청 끝나면 소멸 |
| **Session** | HTTP 세션마다 생성 | 세션 끝나면 소멸 |

#### Singleton + Prototype 주의점

Singleton Bean이 Prototype Bean을 주입받으면, Prototype Bean이 사실상 Singleton처럼 한 번만 생성된다. Singleton 생성 시점에 Prototype이 주입되고 이후 변경되지 않기 때문이다.

```java
@Component @Scope("prototype")
public class PrototypeBean { ... }

@Component
public class SingletonBean {
    private final PrototypeBean proto;  // 주입 시점에 한 번만 생성됨!

    public SingletonBean(PrototypeBean proto) {
        this.proto = proto;
    }
}
```

매번 새 Prototype을 받으려면 `ObjectProvider`를 사용:

```java
@Component
public class SingletonBean {
    private final ObjectProvider<PrototypeBean> protoProvider;

    public void logic() {
        PrototypeBean proto = protoProvider.getObject();  // 매번 새로 생성
    }
}
```

## 관련 문서

- [[IoC-DI]]
- [[Autowired]]
- [[AOP]] — Bean 생성 시 프록시 적용
