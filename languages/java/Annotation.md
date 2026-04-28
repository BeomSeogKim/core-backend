---
tags: [java, annotation, reflection, retention]
status: completed
created: 2026-04-14
---

# Annotation

## 핵심 개념

**Annotation(어노테이션)**은 코드에 **메타데이터**를 부착하는 기능이다. 주석과 달리 컴파일러, 프레임워크, 런타임이 읽고 처리할 수 있다. `@Retention`에 따라 처리 시점이 달라지며, Spring의 핵심 기반 기술이다.

## 동작 원리

### Annotation의 처리 시점 — @Retention

어노테이션이 **언제까지 유지되느냐**에 따라 세 가지로 나뉜다.

<div style="font-family: -apple-system, sans-serif; max-width: 540px; margin: 12px auto; font-size: 12px;">
<div style="display: flex; gap: 4px; flex-wrap: wrap;">
<div style="flex: 1; min-width: 150px; background: #3498DB; color: white; padding: 10px; border-radius: 6px;">
<div style="font-weight: bold;">SOURCE</div>
<div style="font-size: 11px; margin-top: 6px;">소스 코드에만 존재</div>
<div style="font-size: 11px;">컴파일 후 .class에서 사라짐</div>
<div style="font-size: 10px; color: #AED6F1; margin-top: 6px;">@Override<br/>@SuppressWarnings</div>
</div>
<div style="flex: 1; min-width: 150px; background: #E67E22; color: white; padding: 10px; border-radius: 6px;">
<div style="font-weight: bold;">CLASS</div>
<div style="font-size: 11px; margin-top: 6px;">.class 파일에 남음</div>
<div style="font-size: 11px;">런타임에는 사라짐</div>
<div style="font-size: 10px; color: #FDEBD0; margin-top: 6px;">기본값 (잘 안 쓰임)</div>
</div>
<div style="flex: 1; min-width: 150px; background: #E74C3C; color: white; padding: 10px; border-radius: 6px;">
<div style="font-weight: bold;">RUNTIME</div>
<div style="font-size: 11px; margin-top: 6px;">런타임에도 유지</div>
<div style="font-size: 11px;">리플렉션으로 조회 가능</div>
<div style="font-size: 10px; color: #FADBD8; margin-top: 6px;">@Component<br/>@Transactional<br/>@Autowired</div>
</div>
</div>
</div>

```
소스 코드 (.java)     컴파일     바이트코드 (.class)     JVM 로딩      런타임
      │                           │                               │
  SOURCE ────── 여기서 소멸         │                               │
      │                       CLASS ──────────── 여기서 소멸         │
      │                           │                           RUNTIME ── 리플렉션으로 조회 가능
```

Spring의 어노테이션이 대부분 **RUNTIME**인 이유 — 런타임에 [[리플렉션]]으로 어노테이션을 읽어서 Bean 등록, AOP 적용 등을 처리하기 때문이다.

### Annotation 적용 대상 — @Target

어노테이션을 **어디에 붙일 수 있느냐**를 제한한다.

| ElementType | 적용 대상 |
|-------------|----------|
| `TYPE` | 클래스, 인터페이스, enum |
| `METHOD` | 메서드 |
| `FIELD` | 필드 |
| `PARAMETER` | 매개변수 |
| `CONSTRUCTOR` | 생성자 |
| `ANNOTATION_TYPE` | 다른 어노테이션 (메타 어노테이션) |

### 커스텀 Annotation 정의

```java
@Retention(RetentionPolicy.RUNTIME)   // 런타임 유지
@Target(ElementType.METHOD)            // 메서드에만 사용 가능
public @interface LogExecutionTime {
    String value() default "";         // 기본값 있는 속성
}
```

### 메타 Annotation

**어노테이션을 정의할 때 사용하는 어노테이션**이다.

| 메타 어노테이션 | 역할 |
|---------------|------|
| `@Retention` | 유지 기간 (SOURCE / CLASS / RUNTIME) |
| `@Target` | 적용 대상 (TYPE / METHOD / FIELD 등) |
| `@Documented` | Javadoc에 포함 여부 |
| `@Inherited` | 자식 클래스에 상속 여부 |
| `@Repeatable` | 같은 위치에 반복 사용 가능 (Java 8+) |

### Spring에서의 활용

```
@Component ── @Retention(RUNTIME) + @Target(TYPE)
     │
     ├── @Service      ← @Component를 메타 어노테이션으로 사용
     ├── @Repository   ← @Component를 메타 어노테이션으로 사용
     └── @Controller   ← @Component를 메타 어노테이션으로 사용
```

Spring은 `@Component`를 메타 어노테이션으로 포함하는 어노테이션을 **컴포넌트 스캔** 대상으로 인식한다. 이것이 `@Service`, `@Repository` 등이 Bean으로 등록되는 원리이다.

## 코드 예시

```java
// Java 기본 어노테이션
@Override                  // SOURCE — 컴파일러가 오버라이딩 검증
public String toString() { ... }

@SuppressWarnings("unchecked")  // SOURCE — 경고 무시
List list = new ArrayList();

@Deprecated                // RUNTIME — IDE/컴파일러 경고
public void oldMethod() { ... }

@FunctionalInterface       // SOURCE — 추상 메서드 1개 강제
interface MyFunc { void run(); }

// 런타임에 리플렉션으로 어노테이션 읽기
Method method = MyClass.class.getMethod("myMethod");
if (method.isAnnotationPresent(LogExecutionTime.class)) {
    LogExecutionTime anno = method.getAnnotation(LogExecutionTime.class);
    System.out.println(anno.value());
}
```

## 관련 문서

- [[리플렉션]] — 런타임 어노테이션 조회 메커니즘
- [[다이나믹-프록시]] — 어노테이션 기반 AOP 프록시 생성
- [[OOP-4가지-특징]] — @Override와 다형성
- [[Stream-Lambda]] — @FunctionalInterface
