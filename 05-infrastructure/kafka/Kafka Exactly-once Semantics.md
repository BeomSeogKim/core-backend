---
tags: [infrastructure, kafka, exactly-once, transaction, idempotency]
status: in-progress
created: 2026-03-24
---

# Kafka Exactly-once Semantics

## 핵심 개념

메시지 전달 보장에는 3가지 수준이 있다. **Exactly-once**는 메시지가 정확히 한 번만 처리되는 가장 이상적인 수준이며, Kafka는 **Idempotent Producer + Transaction**의 조합으로 이를 구현한다.

---

## 메시지 전달 보장 3단계

| 보장 수준 | 의미 | 유실 | 중복 | Kafka 구현 |
|-----------|------|------|------|-----------|
| **At-most-once** | 많아야 한 번 전달 | 가능 | 없음 | `acks=0`, 재시도 없음 |
| **At-least-once** | 적어도 한 번 전달 | 없음 | 가능 | `acks=all`, 재시도 활성화 |
| **Exactly-once** | 정확히 한 번 전달 | 없음 | 없음 | Idempotent Producer + Transaction |

### 각 단계에서 발생하는 문제 시나리오

<div style="background:#1e1e2e; border-radius:12px; padding:24px; font-family:monospace; font-size:13px; color:#cdd6f4;">
  <!-- At-most-once -->
  <div style="border:2px solid #f38ba8; border-radius:10px; padding:14px; margin-bottom:14px;">
    <div style="color:#f38ba8; font-weight:bold; margin-bottom:10px;">At-most-once — 유실 시나리오</div>
    <div style="display:flex; flex-direction:column; gap:4px; padding-left:8px;">
      <span><span style="color:#f9e2af;">①</span> Producer가 메시지 전송</span>
      <span><span style="color:#f9e2af;">②</span> 네트워크 오류로 ack를 못 받음</span>
      <span><span style="color:#f38ba8;">③</span> 재시도 안 함 → <strong>메시지 유실 가능</strong></span>
      <span style="color:#6c7086; margin-top:4px;">→ 로그 수집처럼 일부 유실이 허용되는 경우에 사용</span>
    </div>
  </div>
  <!-- At-least-once -->
  <div style="border:2px solid #f9e2af; border-radius:10px; padding:14px; margin-bottom:14px;">
    <div style="color:#f9e2af; font-weight:bold; margin-bottom:10px;">At-least-once — 중복 시나리오</div>
    <div style="display:flex; flex-direction:column; gap:4px; padding-left:8px;">
      <span><span style="color:#f9e2af;">①</span> Producer가 메시지 전송 → Broker가 저장 성공</span>
      <span><span style="color:#f9e2af;">②</span> ack 반환 중 네트워크 끊김</span>
      <span><span style="color:#f9e2af;">③</span> Producer는 실패로 판단 → 같은 메시지 재전송</span>
      <span><span style="color:#f38ba8;">④</span> Broker에 <strong>같은 메시지가 2번</strong> 저장됨</span>
      <span style="color:#6c7086; margin-top:4px;">→ 대부분의 시스템이 이 수준 + Consumer 멱등성으로 운영</span>
    </div>
  </div>
  <!-- Exactly-once -->
  <div style="border:2px solid #a6e3a1; border-radius:10px; padding:14px;">
    <div style="color:#a6e3a1; font-weight:bold; margin-bottom:10px;">Exactly-once — 이상적</div>
    <div style="display:flex; flex-direction:column; gap:4px; padding-left:8px;">
      <span>유실도 없고, 중복도 없음</span>
      <span>Kafka 내부에서 Idempotent Producer + Transaction으로 구현</span>
      <span style="color:#6c7086; margin-top:4px;">→ 금융, 결제 등 정확성이 필수인 도메인에서 사용</span>
    </div>
  </div>
</div>

---

## Exactly-once를 구현하는 두 축

Kafka의 Exactly-once는 독립적인 두 메커니즘의 조합이다:

| 구간 | 메커니즘 | 해결하는 문제 |
|------|---------|-------------|
| Producer → Broker | **Idempotent Producer** | 네트워크 재전송으로 인한 중복 발행 |
| Consume → Process → Produce | **Transaction** | 읽기 + 처리 + 쓰기의 원자적 실행 |

---

## 1. Idempotent Producer

### 문제: 네트워크 재전송에 의한 중복

```
Producer                         Broker
   │                               │
   │  send(msg-A)                  │
   │──────────────────────────────▶│  저장 완료
   │                               │
   │        ack 반환               │
   │◀ ─ ─ ─ ─ ─ ─ ─ ✕ ─ ─ ─ ─ ─ │  ← 네트워크 끊김, ack 유실
   │                               │
   │  ack 못 받음 → 재전송          │
   │  send(msg-A)  ← 같은 메시지!  │
   │──────────────────────────────▶│  또 저장 → 중복 발생!
```

### 해결: PID + Sequence Number

`enable.idempotence=true`를 설정하면 Producer에 **PID (Producer ID)**가 부여되고, 각 메시지에 **Sequence Number**가 자동으로 붙는다.

<div style="background:#1e1e2e; border-radius:12px; padding:20px; font-family:monospace; font-size:13px; color:#cdd6f4;">
  <div style="display:flex; flex-direction:column; gap:10px;">
    <div style="display:flex; align-items:center; gap:8px;">
      <span style="color:#a6e3a1;">①</span>
      <span>Producer <span style="color:#89b4fa;">(PID=1)</span> → send(msg-A, <span style="color:#f9e2af;">seq=0</span>)</span>
    </div>
    <div style="display:flex; align-items:center; gap:8px;">
      <span style="color:#a6e3a1;">②</span>
      <span>Broker: PID=1, seq=0 처음 봄 → <span style="color:#a6e3a1;">저장</span></span>
    </div>
    <div style="display:flex; align-items:center; gap:8px;">
      <span style="color:#f38ba8;">③</span>
      <span>ack 유실 → Producer 재전송: send(msg-A, <span style="color:#f9e2af;">seq=0</span>)</span>
    </div>
    <div style="display:flex; align-items:center; gap:8px;">
      <span style="color:#a6e3a1;">④</span>
      <span>Broker: PID=1, seq=0 <span style="color:#f9e2af;">이미 있음</span> → <span style="color:#f38ba8;">무시 (중복 방지)</span></span>
    </div>
  </div>
</div>

**동작 원리:**
- Producer가 시작할 때 Broker로부터 고유한 **PID**를 발급받는다
- 각 Partition별로 **Sequence Number**를 0부터 순차 증가시킨다
- Broker는 `(PID, Partition, Sequence Number)` 조합을 추적한다
- 같은 조합의 메시지가 다시 오면 **중복으로 판단하고 무시**한다

### 설정

```properties
# Producer 설정
enable.idempotence=true     # 멱등성 활성화
acks=all                    # 필수: idempotence 활성화 시 자동으로 all
retries=Integer.MAX_VALUE   # 필수: 자동 설정
max.in.flight.requests.per.connection=5  # 최대 5 (idempotence 제약)
```

> [!note] Idempotent Producer의 범위
> Idempotent Producer는 **단일 Producer → 단일 Partition** 구간에서만 중복을 방지한다. 여러 Partition에 걸친 원자적 쓰기나 Consumer offset commit까지 포함하려면 **Transaction**이 필요하다.

---

## 2. Kafka Transaction

### 문제: Consume-Process-Produce 패턴의 중복

MSA에서 흔한 패턴: Topic A에서 읽고 → 처리 → Topic B에 쓰기

```
[orders Topic] → 주문 처리 서비스 → [payments Topic]
```

이때 위험한 시나리오:

<div style="background:#1e1e2e; border-radius:12px; padding:20px; font-family:monospace; font-size:13px; color:#cdd6f4;">
  <div style="display:flex; flex-direction:column; gap:6px;">
    <div style="display:flex; align-items:center; gap:8px;">
      <span style="color:#a6e3a1;">①</span>
      <span>orders에서 메시지 읽음 (offset 5)</span>
    </div>
    <div style="display:flex; align-items:center; gap:8px;">
      <span style="color:#a6e3a1;">②</span>
      <span>비즈니스 로직 처리 완료</span>
    </div>
    <div style="display:flex; align-items:center; gap:8px;">
      <span style="color:#a6e3a1;">③</span>
      <span>payments Topic에 메시지 발행 → <span style="color:#a6e3a1;">성공</span></span>
    </div>
    <div style="display:flex; align-items:center; gap:8px;">
      <span style="color:#f38ba8;">④</span>
      <span style="color:#f38ba8;">offset 5를 commit하려는 순간 서비스 죽음!</span>
    </div>
    <div style="border-top:1px dashed #6c7086; margin:8px 0; padding-top:8px;">
      <span style="color:#f9e2af;">⑤</span>
      <span> 서비스 재시작 → offset 5 미 commit → 다시 읽음</span>
    </div>
    <div style="display:flex; align-items:center; gap:8px;">
      <span style="color:#f38ba8;">⑥</span>
      <span style="color:#f38ba8;">payments에 같은 메시지가 또 발행됨 → <strong>중복!</strong></span>
    </div>
  </div>
</div>

핵심 문제: **"메시지 발행"과 "offset commit"이 별개 동작**이라 둘 사이에서 장애가 나면 불일치가 발생한다.

### 해결: Transaction으로 원자적 실행

Kafka Transaction은 **메시지 발행 + offset commit**을 하나의 원자적 단위로 묶는다. 전부 성공하거나 전부 실패한다.

<div style="background:#1e1e2e; border-radius:12px; padding:24px; font-family:monospace; font-size:13px; color:#cdd6f4;">
  <!-- Transaction 성공 -->
  <div style="border:2px solid #a6e3a1; border-radius:10px; padding:14px; margin-bottom:14px;">
    <div style="color:#a6e3a1; font-weight:bold; margin-bottom:10px;">Transaction 성공 시</div>
    <div style="display:flex; flex-direction:column; gap:4px; padding-left:8px;">
      <span><span style="color:#f9e2af;">①</span> beginTransaction()</span>
      <span><span style="color:#f9e2af;">②</span> orders에서 메시지 읽음 (offset 5)</span>
      <span><span style="color:#f9e2af;">③</span> payments에 메시지 발행 <span style="color:#6c7086;">← uncommitted 상태</span></span>
      <span><span style="color:#f9e2af;">④</span> offset 5를 트랜잭션에 포함 <span style="color:#6c7086;">← uncommitted 상태</span></span>
      <span><span style="color:#a6e3a1;">⑤</span> <strong>commitTransaction()</strong> → ③, ④ 한꺼번에 committed</span>
    </div>
  </div>
  <!-- Transaction 실패 -->
  <div style="border:2px solid #f38ba8; border-radius:10px; padding:14px;">
    <div style="color:#f38ba8; font-weight:bold; margin-bottom:10px;">Transaction 실패 시</div>
    <div style="display:flex; flex-direction:column; gap:4px; padding-left:8px;">
      <span><span style="color:#f9e2af;">①</span> beginTransaction()</span>
      <span><span style="color:#f9e2af;">②</span> orders에서 메시지 읽음 (offset 5)</span>
      <span><span style="color:#f9e2af;">③</span> payments에 메시지 발행 <span style="color:#6c7086;">← uncommitted 상태</span></span>
      <span><span style="color:#f38ba8;">④</span> 서비스 죽음 또는 예외 발생</span>
      <span><span style="color:#f38ba8;">⑤</span> <strong>abortTransaction()</strong> → ③ 롤백, offset도 미 commit</span>
      <span style="color:#6c7086; margin-top:4px;">→ 재시작 시 offset 5부터 다시 읽음. payments에는 아무것도 안 들어감</span>
    </div>
  </div>
</div>

### Transaction 코드 예시

```java
// Producer 설정: transactional.id 필수
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("transactional.id", "order-processor-1");  // 트랜잭션 식별자
props.put("enable.idempotence", true);                // 자동 활성화되지만 명시

KafkaProducer<String, String> producer = new KafkaProducer<>(props);
KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);

producer.initTransactions();  // 트랜잭션 초기화 (최초 1회)

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

    producer.beginTransaction();

    try {
        for (ConsumerRecord<String, String> record : records) {
            // 비즈니스 로직 처리
            String result = processOrder(record.value());

            // 결과를 다른 Topic에 발행
            producer.send(new ProducerRecord<>("payments", record.key(), result));
        }

        // Consumer offset을 트랜잭션에 포함
        Map<TopicPartition, OffsetAndMetadata> offsets = getConsumedOffsets(records);
        producer.sendOffsetsToTransaction(offsets, consumer.groupMetadata());

        // 모두 성공 → 한꺼번에 commit
        producer.commitTransaction();

    } catch (Exception e) {
        // 하나라도 실패 → 전부 롤백
        producer.abortTransaction();
    }
}
```

---

## Transaction 내부 동작: Transaction Coordinator

Kafka Broker 내부에 **Transaction Coordinator**가 있다. 이것이 트랜잭션의 상태를 관리한다.

<div style="background:#1e1e2e; border-radius:12px; padding:24px; font-family:monospace; font-size:13px; color:#cdd6f4;">
  <div style="color:#89b4fa; font-weight:bold; margin-bottom:16px;">Transaction 내부 동작 흐름</div>
  <div style="display:flex; flex-direction:column; gap:8px;">
    <div style="background:#313244; padding:10px 14px; border-radius:8px; border-left:3px solid #f38ba8;">
      <span style="color:#f9e2af;">①</span> Producer → <strong>initTransactions()</strong>
      <div style="color:#6c7086; padding-left:20px; margin-top:4px;">Transaction Coordinator에 transactional.id 등록</div>
    </div>
    <div style="background:#313244; padding:10px 14px; border-radius:8px; border-left:3px solid #89b4fa;">
      <span style="color:#f9e2af;">②</span> Producer → <strong>beginTransaction()</strong>
      <div style="color:#6c7086; padding-left:20px; margin-top:4px;">Coordinator: 트랜잭션 상태를 "Ongoing"으로 기록</div>
    </div>
    <div style="background:#313244; padding:10px 14px; border-radius:8px; border-left:3px solid #89b4fa;">
      <span style="color:#f9e2af;">③</span> Producer → <strong>send()</strong>
      <div style="color:#6c7086; padding-left:20px; margin-top:4px;">메시지가 Partition에 쓰이지만 "uncommitted" 상태. Coordinator가 어떤 Partition에 썼는지 추적</div>
    </div>
    <div style="background:#313244; padding:10px 14px; border-radius:8px; border-left:3px solid #89b4fa;">
      <span style="color:#f9e2af;">④</span> Producer → <strong>sendOffsetsToTransaction()</strong>
      <div style="color:#6c7086; padding-left:20px; margin-top:4px;">Consumer offset도 트랜잭션에 포함</div>
    </div>
    <div style="background:#313244; padding:10px 14px; border-radius:8px; border-left:3px solid #a6e3a1;">
      <span style="color:#f9e2af;">⑤</span> Producer → <strong>commitTransaction()</strong>
      <div style="color:#6c7086; padding-left:20px; margin-top:4px;">Coordinator: 모든 관련 Partition에 "COMMIT" 마커를 기록. 메시지가 "committed" 상태로 전환</div>
    </div>
  </div>
</div>

### Transaction Coordinator의 핵심 역할

1. **Transaction Log**: `__transaction_state`라는 내부 Topic에 트랜잭션 상태를 기록한다 (DB의 WAL과 비슷)
2. **상태 추적**: 각 transactional.id별로 현재 진행 중인 트랜잭션의 상태를 관리한다
3. **COMMIT/ABORT 마커**: 트랜잭션 완료 시 관련된 모든 Partition에 마커를 기록한다
4. **타임아웃 처리**: 일정 시간 내에 commit/abort되지 않은 트랜잭션을 자동으로 abort한다

---

## Consumer의 isolation.level

Transaction으로 쓴 메시지를 읽는 Consumer 쪽에서도 설정이 필요하다:

| isolation.level | 동작 |
|----------------|------|
| `read_uncommitted` (기본값) | uncommitted 메시지도 읽음. Transaction 효과 없음 |
| `read_committed` | committed 메시지만 읽음. Transaction과 함께 사용 |

```properties
# Consumer 설정
isolation.level=read_committed
```

> [!warning] isolation.level 설정 누락 주의
> Producer에서 Transaction을 사용해도 Consumer가 `read_uncommitted`(기본값)이면 uncommitted 메시지를 읽게 된다. Transaction의 Exactly-once 보장이 깨지므로 **반드시 `read_committed`로 설정**해야 한다.

---

## Exactly-once의 범위와 한계

### Kafka 내부: 완전한 Exactly-once

```
Kafka Topic A → Consumer/Producer → Kafka Topic B
```

Idempotent Producer + Transaction + `read_committed`로 **완전한 Exactly-once**가 가능하다.

### Kafka → 외부 시스템: 사실상 Exactly-once

```
Kafka Topic → Consumer → DB / API / 파일
```

Kafka Transaction은 Kafka 내부 동작(메시지 발행 + offset commit)만 원자적으로 묶는다. **외부 시스템(DB 저장, API 호출 등)**은 Kafka Transaction 범위 밖이다.

<div style="background:#1e1e2e; border-radius:12px; padding:20px; font-family:monospace; font-size:13px; color:#cdd6f4;">
  <div style="display:flex; flex-direction:column; gap:8px;">
    <div style="display:flex; align-items:center; gap:8px; flex-wrap:wrap;">
      <div style="background:#89b4fa; color:#1e1e2e; padding:8px 14px; border-radius:8px;">Kafka Topic</div>
      <span style="color:#a6e3a1;">→ Consumer →</span>
      <div style="background:#a6e3a1; color:#1e1e2e; padding:8px 14px; border-radius:8px;">DB에 저장</div>
      <span style="color:#a6e3a1;">→</span>
      <div style="background:#a6e3a1; color:#1e1e2e; padding:8px 14px; border-radius:8px;">offset commit</div>
    </div>
    <div style="padding-left:180px; color:#6c7086;">
      <span style="color:#f38ba8;">↑ 여기서 죽으면?</span>
    </div>
    <div style="padding-left:180px; color:#6c7086;">
      DB에는 저장됨, offset은 미 commit → 재시작 시 중복 저장
    </div>
  </div>
</div>

이 경우의 해결책: **At-least-once + Consumer 멱등성**

```java
// Consumer 측 멱등성 구현 예시
@Transactional
public void processMessage(ConsumerRecord<String, String> record) {
    String orderId = record.key();

    // DB에서 이미 처리했는지 확인 (unique key 또는 처리 이력 테이블)
    if (orderRepository.existsByOrderId(orderId)) {
        log.info("이미 처리된 주문: {}", orderId);
        return; // skip
    }

    // 처리 로직
    orderRepository.save(new Order(orderId, record.value()));
}
```

### 정리: Exactly-once 적용 가이드

| 시나리오 | 전략 |
|---------|------|
| Kafka → Kafka (스트림 처리) | Idempotent Producer + Transaction + read_committed |
| Kafka → DB | At-least-once + Consumer 멱등성 (unique key / 이력 테이블) |
| Kafka → 외부 API | At-least-once + API 멱등성 (idempotency key) |

---

## transactional.id의 역할

`transactional.id`는 Producer의 **논리적 식별자**다. PID와 다르게 애플리케이션이 직접 지정한다.

**왜 필요한가?**

Producer가 죽었다가 재시작하면 새로운 PID를 받는다. 이전 Producer가 진행 중이던 미완료 트랜잭션이 남아있을 수 있다. `transactional.id`가 같으면 Coordinator가 이전 미완료 트랜잭션을 **자동으로 abort**하고, 새 Producer가 이어서 작업할 수 있다.

```
Producer (PID=1, txn.id="order-processor-1")
  → beginTransaction() → send() → 여기서 죽음 (미완료 트랜잭션)

Producer 재시작 (PID=2, txn.id="order-processor-1")  ← 같은 txn.id
  → initTransactions()
  → Coordinator: PID=1의 미완료 트랜잭션 발견 → 자동 abort
  → 새 트랜잭션 시작 가능
```

> [!note] transactional.id 네이밍
> 인스턴스별로 고유해야 한다. 보통 `{서비스명}-{처리하는 Partition}` 형태로 명명한다. 예: `order-processor-partition-0`

---

## 관련 문서

- [[2-Areas/backend/05-infrastructure/kafka/Kafka 아키텍처와 동작 원리|Kafka 아키텍처와 동작 원리]] — Kafka 전체 구조 및 동작
- [[2-Areas/backend/05-infrastructure/kafka/Kafka DLQ와 에러 처리|Kafka DLQ와 에러 처리]] — 실패 메시지 처리 전략
- [[2-Areas/backend/05-infrastructure/kafka/Kafka 메타데이터 관리, Zookeeper에서 KRaft로|Zookeeper에서 KRaft로]] — 클러스터 메타데이터 관리
- [[2-Areas/backend/03-database/트랜잭션|트랜잭션]] — DB 트랜잭션과의 비교
