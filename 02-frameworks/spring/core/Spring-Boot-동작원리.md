---
tags: [spring, spring-boot, auto-configuration]
status: completed
created: 2026-04-02
---

# Spring Boot 동작원리

## 핵심 개념

**Spring Boot**는 Spring 애플리케이션의 설정을 자동화하여 빠르게 실행할 수 있게 하는 프레임워크다. 핵심은 **Auto Configuration**으로, classpath와 조건에 따라 필요한 Bean을 자동으로 등록한다.

## 동작 원리

### @SpringBootApplication

```java
@SpringBootApplication  // 아래 3개의 조합
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

`@SpringBootApplication` = 3개 어노테이션의 조합:

| 어노테이션 | 역할 |
|---|---|
| `@SpringBootConfiguration` | `@Configuration`과 동일. 설정 클래스 표시 |
| `@ComponentScan` | 현재 패키지 기준으로 `@Component`, `@Service`, `@Repository` 등 스캔 → Bean 등록 |
| `@EnableAutoConfiguration` | Auto Configuration 활성화 |

### Auto Configuration 동작 흐름

```
1. @EnableAutoConfiguration 활성화
    ↓
2. META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports 읽기
   (Spring Boot 2.x: META-INF/spring.factories)
    ↓
3. 자동 설정 클래스 목록 로드 (수백 개)
    ↓
4. @Conditional 계열 어노테이션으로 조건 평가
    ↓
5. 조건에 맞는 클래스만 Bean으로 등록
```

### @Conditional 계열 어노테이션

| 어노테이션 | 조건 |
|---|---|
| `@ConditionalOnClass` | 특정 클래스가 classpath에 **있을 때** |
| `@ConditionalOnMissingClass` | 특정 클래스가 classpath에 **없을 때** |
| `@ConditionalOnBean` | 특정 Bean이 이미 **등록되어 있을 때** |
| `@ConditionalOnMissingBean` | 특정 Bean이 **아직 없을 때** |
| `@ConditionalOnProperty` | 특정 설정값(`application.yml`)이 있을 때 |

#### 예시: DataSource 자동 설정

```java
@Configuration
@ConditionalOnClass(DataSource.class)           // classpath에 DataSource가 있고
@ConditionalOnProperty("spring.datasource.url") // 설정에 URL이 있으면
public class DataSourceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean                    // 개발자가 직접 등록 안 했으면
    public DataSource dataSource() {
        return new HikariDataSource();           // 자동으로 HikariCP 생성
    }
}
```

`spring-boot-starter-web`을 의존성에 추가하면:
1. Tomcat 클래스가 classpath에 존재
2. `@ConditionalOnClass(Tomcat.class)` 조건 충족
3. 내장 Tomcat이 자동 설정되어 Bean 등록

### SpringApplication.run() 실행 흐름

```
SpringApplication.run()
    ↓
ApplicationContext (IoC 컨테이너) 생성
    ↓
@ComponentScan → 개발자 정의 Bean 등록
    ↓
@EnableAutoConfiguration → 자동 설정 Bean 등록
    ↓
내장 서버 (Tomcat) 시작
    ↓
ApplicationReadyEvent 발행 → 애플리케이션 준비 완료
```

## 관련 문서

- [[IoC-DI]] — Spring Boot가 자동화하는 DI
- [[Bean]] — Auto Configuration으로 등록되는 Bean
- [[Filter-vs-Interceptor]] — DispatcherServlet과 요청 처리
