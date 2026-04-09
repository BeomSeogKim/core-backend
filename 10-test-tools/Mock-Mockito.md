---
tags: [test, mock, mockito, java]
status: completed
created: 2026-04-04
---

# Mock + Mockito

## 핵심 개념

**Mock**은 실제 객체를 대신하는 가짜 객체로, 단위 테스트에서 **외부 의존성을 제거**하고 테스트 대상의 로직만 검증하기 위해 사용한다. **Mockito**는 Java의 대표적인 Mocking 프레임워크다.

## 동작 원리

### Mock이 필요한 이유

<div style="font-family: -apple-system, sans-serif; max-width: 480px; margin: 12px auto; padding: 16px; font-size: 12px;">
<div style="display: flex; gap: 12px;">
<div style="flex: 1; border: 2px solid #E74C3C; border-radius: 8px; padding: 12px;">
<div style="background: #E74C3C; color: white; padding: 6px; border-radius: 4px; text-align: center; font-weight: bold; margin-bottom: 8px;">Mock 없이</div>
<div style="font-size: 11px; line-height: 1.8; color: #333;">
OrderService 테스트<br/>
→ OrderRepository 필요<br/>
→ <b>실제 DB 필요</b><br/>
→ 느림, 환경 의존<br/>
→ <span style="color: #E74C3C;">Service 로직 외 실패 가능</span>
</div>
</div>
<div style="flex: 1; border: 2px solid #27AE60; border-radius: 8px; padding: 12px;">
<div style="background: #27AE60; color: white; padding: 6px; border-radius: 4px; text-align: center; font-weight: bold; margin-bottom: 8px;">Mock 사용</div>
<div style="font-size: 11px; line-height: 1.8; color: #333;">
OrderService 테스트<br/>
→ OrderRepository를 <b>Mock</b><br/>
→ DB 불필요<br/>
→ 빠름, 독립적<br/>
→ <span style="color: #27AE60;">Service 로직만 순수 검증</span>
</div>
</div>
</div>
</div>

### Test Double 종류

| 종류 | 역할 |
|------|------|
| **Mock** | 호출 여부/횟수/인자를 **검증** 가능한 가짜 객체 |
| **Stub** | 특정 호출에 대해 **미리 정해진 값을 반환**하는 가짜 객체 |
| **Spy** | 실제 객체를 감싸서 일부 메서드만 Mock으로 대체 |
| **Fake** | 동작하는 간단한 구현체 (예: In-Memory DB) |

Mockito에서는 `when().thenReturn()`이 Stub 역할, `verify()`가 Mock 역할을 한다.

### Mockito 핵심 어노테이션

| 어노테이션 | 역할 |
|---|---|
| `@Mock` | 가짜 객체 생성 |
| `@InjectMocks` | Mock을 주입받는 **테스트 대상** 객체 |
| `@Spy` | 실제 객체 기반 + 일부만 Mock |
| `@ExtendWith(MockitoExtension.class)` | JUnit 5에서 Mockito 활성화 |

### Mockito 주요 메서드

| 메서드 | 역할 |
|---|---|
| `when(mock.method()).thenReturn(value)` | 호출 시 **반환값 지정** |
| `when(mock.method()).thenThrow(exception)` | 호출 시 **예외 발생** |
| `verify(mock).method()` | 메서드가 **호출되었는지** 검증 |
| `verify(mock, times(2)).method()` | **호출 횟수** 검증 |
| `verify(mock, never()).method()` | **호출 안 됨** 검증 |
| `any()`, `anyLong()`, `anyString()` | **아무 값**이나 매칭 |

## 코드 예시

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;  // 가짜 객체

    @Mock
    NotificationService notificationService;  // 가짜 객체

    @InjectMocks
    OrderService orderService;  // Mock이 주입된 테스트 대상

    @Test
    @DisplayName("주문 조회 성공")
    void findOrder() {
        // given — Mock 행동 정의 (Stub)
        Order order = new Order(1L, "상품A", 1000);
        when(orderRepository.findById(1L))
            .thenReturn(Optional.of(order));

        // when — 테스트 실행
        Order result = orderService.findOrder(1L);

        // then — 결과 검증
        assertEquals("상품A", result.getName());
        verify(orderRepository).findById(1L);  // 호출 검증
    }

    @Test
    @DisplayName("주문 생성 시 알림 발송")
    void createOrderSendsNotification() {
        // given
        when(orderRepository.save(any(Order.class)))
            .thenReturn(new Order(1L, "상품A", 1000));

        // when
        orderService.createOrder("상품A", 1000);

        // then — 알림이 1번 호출되었는지 검증
        verify(notificationService, times(1))
            .sendNotification(anyLong());
    }

    @Test
    @DisplayName("존재하지 않는 주문 조회 시 예외")
    void findOrderNotFound() {
        // given
        when(orderRepository.findById(999L))
            .thenReturn(Optional.empty());

        // when & then
        assertThrows(OrderNotFoundException.class,
            () -> orderService.findOrder(999L));
    }
}
```

### BDDMockito — 가독성 향상

`when` 대신 `given`을 사용하여 Given-When-Then 패턴과 일치시킨다.

```java
import static org.mockito.BDDMockito.*;

// given
given(orderRepository.findById(1L))
    .willReturn(Optional.of(order));

// when
Order result = orderService.findOrder(1L);

// then
then(orderRepository).should().findById(1L);
```

## 관련 문서

- [[JUnit]]
- [[테스트-종류]]
