---
tags: [test, junit, java]
status: completed
created: 2026-04-04
---

# JUnit 5

## 핵심 개념

**JUnit 5**는 Java의 표준 단위 테스트 프레임워크다. `@Test` 어노테이션으로 테스트 메서드를 정의하고, `Assertions`로 결과를 검증한다.

## 동작 원리

### 주요 어노테이션

| 어노테이션 | 역할 | 실행 시점 |
|---|---|---|
| `@Test` | 테스트 메서드 표시 | — |
| `@DisplayName` | 테스트 이름 지정 (한글 가능) | — |
| `@BeforeEach` | 각 테스트 **전**에 실행 | 매 테스트 전 |
| `@AfterEach` | 각 테스트 **후**에 실행 | 매 테스트 후 |
| `@BeforeAll` | 전체 테스트 **전** 1번 (`static`) | 클래스 시작 시 |
| `@AfterAll` | 전체 테스트 **후** 1번 (`static`) | 클래스 종료 시 |
| `@Disabled` | 테스트 비활성화 | — |
| `@ParameterizedTest` | 여러 값으로 반복 테스트 | — |
| `@Nested` | 테스트 그룹화 (내부 클래스) | — |

### 실행 흐름

```
@BeforeAll (1번)
│
├── @BeforeEach
│   └── @Test (테스트 1)
├── @AfterEach
│
├── @BeforeEach
│   └── @Test (테스트 2)
├── @AfterEach
│
@AfterAll (1번)
```

### Assertions — 결과 검증

```java
// 값 비교
assertEquals(expected, actual);
assertNotEquals(a, b);

// null 체크
assertNotNull(object);
assertNull(object);

// 조건 검증
assertTrue(condition);
assertFalse(condition);

// 예외 검증
assertThrows(IllegalArgumentException.class, () -> {
    new Order(-1, 3);  // 예외가 발생해야 통과
});

// 시간 제한
assertTimeout(Duration.ofSeconds(2), () -> {
    heavyOperation();  // 2초 안에 완료되어야 통과
});
```

## 코드 예시

```java
class OrderServiceTest {

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService();  // 매 테스트 전 초기화
    }

    @Test
    @DisplayName("주문 금액 계산 - 정상")
    void calculateTotalPrice() {
        // given
        Order order = new Order(1000, 3);

        // when
        int total = order.getTotalPrice();

        // then
        assertEquals(3000, total);
    }

    @Test
    @DisplayName("주문 수량 0 이하 시 예외")
    void invalidQuantity() {
        assertThrows(IllegalArgumentException.class,
            () -> new Order(1000, 0));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 100})
    @DisplayName("다양한 수량으로 주문 생성")
    void createOrderWithVariousQuantity(int quantity) {
        Order order = new Order(1000, quantity);
        assertEquals(1000 * quantity, order.getTotalPrice());
    }

    @Nested
    @DisplayName("할인 적용 테스트")
    class DiscountTest {
        @Test
        void VIP_할인_적용() { ... }

        @Test
        void 일반_할인_미적용() { ... }
    }
}
```

## 관련 문서

- [[테스트-종류]]
- [[Mock-Mockito]]
