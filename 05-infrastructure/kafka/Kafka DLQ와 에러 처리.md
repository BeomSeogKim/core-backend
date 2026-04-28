---
tags: [infrastructure, kafka, dlq, error-handling]
status: in-progress
created: 2026-03-24
---

# Kafka DLQ와 에러 처리

## 핵심 개념

**DLQ (Dead Letter Queue)**는 Consumer가 처리에 실패한 메시지를 격리하는 별도의 Kafka Topic이다. Kafka가 자체적으로 DLQ를 제공하는 것이 아니라, **애플리케이션 레벨에서 설계**하는 패턴이다. 원본 Topic의 메시지 소비 흐름을 막지 않으면서, 실패한 메시지를 안전하게 보관하고 추후 재처리할 수 있게 해준다.

---

## 동작 원리

### 1. DLQ가 필요한 이유

Consumer가 메시지를 처리하다 실패하면 여러 문제가 발생한다:

- **무한 재시도**: 실패한 메시지를 계속 retry하면 뒤의 정상 메시지들이 밀림
- **전체 중단**: 하나의 실패 때문에 Partition 전체 소비가 멈출 수 있음
- **순서 역전**: 실패한 메시지를 건너뛰면 비즈니스 로직의 순서가 깨질 수 있음

DLQ는 이 문제를 해결한다: **일정 횟수 재시도 후에도 실패하면 DLQ Topic으로 보내고, 원본 Topic은 계속 소비**한다.

### 2. 전체 흐름

<div style="background:#1e1e2e; border-radius:12px; padding:24px; font-family:monospace; font-size:13px; color:#cdd6f4; overflow-x:auto;">
  <div style="display:flex; flex-direction:column; gap:10px;">
    <!-- 정상 흐름 -->
    <div style="display:flex; align-items:center; gap:8px; flex-wrap:wrap;">
      <div style="background:#89b4fa; color:#1e1e2e; padding:8px 14px; border-radius:8px; font-weight:bold;">원본 Topic</div>
      <span style="color:#a6e3a1;">→ poll() →</span>
      <div style="background:#313244; border:2px solid #a6e3a1; padding:8px 14px; border-radius:8px;">Consumer 처리</div>
      <span style="color:#a6e3a1;">→ 성공 →</span>
      <div style="background:#a6e3a1; color:#1e1e2e; padding:8px 14px; border-radius:8px; font-weight:bold;">offset commit</div>
    </div>
    <!-- 실패 흐름 -->
    <div style="padding-left:200px; display:flex; align-items:center; gap:8px; flex-wrap:wrap;">
      <span style="color:#f9e2af;">↓ 실패</span>
    </div>
    <div style="display:flex; align-items:center; gap:8px; flex-wrap:wrap; padding-left:140px;">
      <div style="background:#313244; border:2px solid #f9e2af; padding:8px 14px; border-radius:8px;">재시도 (1차, 2차, 3차...)</div>
      <span style="color:#f38ba8;">→ N회 실패 →</span>
      <div style="background:#f38ba8; color:#1e1e2e; padding:8px 14px; border-radius:8px; font-weight:bold;">DLQ Topic으로 발행</div>
    </div>
    <!-- DLQ 처리 -->
    <div style="padding-left:140px; margin-top:8px; display:flex; align-items:center; gap:8px; flex-wrap:wrap;">
      <span style="color:#6c7086;">└→</span>
      <div style="background:#45475a; padding:8px 14px; border-radius:8px;">원본 Topic은 다음 메시지로 진행 (offset commit)</div>
    </div>
  </div>
</div>

### 3. Retry 전략: 즉시 재시도 vs 지연 재시도

#### 즉시 재시도 (Immediate Retry)

실패하면 바로 재시도한다. 일시적 오류(네트워크 순단, DB 일시적 과부하)에 효과적이다.

```java
int maxRetries = 3;
for (int attempt = 1; attempt <= maxRetries; attempt++) {
    try {
        processMessage(record);
        consumer.commitSync();
        break;
    } catch (Exception e) {
        if (attempt == maxRetries) {
            // DLQ로 발행
            producer.send(new ProducerRecord<>("orders.DLQ", record.key(), record.value()));
            consumer.commitSync(); // 원본 offset도 commit → 다음 메시지로 진행
        }
    }
}
```

#### 지연 재시도 (Delayed Retry with Retry Topics)

실패 후 일정 시간 대기했다가 재시도한다. 외부 시스템 장애 등 복구에 시간이 필요한 경우에 적합하다.

<div style="background:#1e1e2e; border-radius:12px; padding:20px; font-family:monospace; font-size:13px; color:#cdd6f4;">
  <div style="display:flex; flex-direction:column; gap:8px;">
    <div style="display:flex; align-items:center; gap:8px; flex-wrap:wrap;">
      <div style="background:#89b4fa; color:#1e1e2e; padding:6px 12px; border-radius:6px; font-weight:bold;">orders</div>
      <span style="color:#f38ba8;">→ 실패 →</span>
      <div style="background:#f9e2af; color:#1e1e2e; padding:6px 12px; border-radius:6px;">orders.retry-1 <small>(30초 후)</small></div>
      <span style="color:#f38ba8;">→ 실패 →</span>
      <div style="background:#fab387; color:#1e1e2e; padding:6px 12px; border-radius:6px;">orders.retry-2 <small>(5분 후)</small></div>
      <span style="color:#f38ba8;">→ 실패 →</span>
      <div style="background:#f38ba8; color:#1e1e2e; padding:6px 12px; border-radius:6px; font-weight:bold;">orders.DLQ</div>
    </div>
  </div>
  <div style="color:#6c7086; margin-top:12px;">각 retry topic은 별도 Consumer가 지연 시간 후 소비</div>
</div>

**Retry Topic 체인 방식의 장점:**
- 원본 Topic의 소비를 전혀 방해하지 않음
- 단계별로 대기 시간을 늘려 외부 시스템 복구 시간 확보
- 각 단계에서의 실패를 별도로 모니터링 가능

### 4. DLQ 메시지에 포함해야 할 정보

DLQ에 메시지를 보낼 때, 원본 메시지 외에 **실패 컨텍스트**를 함께 저장하면 디버깅이 훨씬 쉬워진다.

```java
// DLQ 메시지 구성
ProducerRecord<String, String> dlqRecord = new ProducerRecord<>("orders.DLQ", record.key(), record.value());

// Headers에 메타 정보 추가
dlqRecord.headers()
    .add("original-topic", "orders".getBytes())
    .add("original-partition", String.valueOf(record.partition()).getBytes())
    .add("original-offset", String.valueOf(record.offset()).getBytes())
    .add("failure-reason", exception.getMessage().getBytes())
    .add("retry-count", String.valueOf(maxRetries).getBytes())
    .add("failed-at", Instant.now().toString().getBytes());
```

포함해야 할 핵심 정보:

| 필드 | 용도 |
|------|------|
| `original-topic` | 어떤 Topic에서 왔는지 |
| `original-partition` | 어떤 Partition이었는지 |
| `original-offset` | 원본 메시지의 위치 |
| `failure-reason` | 실패 이유 (Exception 메시지) |
| `retry-count` | 몇 번 재시도 후 실패했는지 |
| `failed-at` | 실패 시점 |

---

## DLQ 처리 전략

DLQ에 쌓인 메시지는 결국 처리해야 한다. 일반적인 전략 4가지:

### 1. 모니터링 + 알림

DLQ Topic의 메시지 수를 모니터링하고, 메시지가 들어오면 즉시 알림을 보낸다.

```
DLQ Consumer Lag > 0 → Slack/PagerDuty 알림 발송
```

가장 기본적이고 반드시 설정해야 할 전략이다.

### 2. 수동 확인 후 재처리

개발자가 DLQ 메시지를 확인하고, 원인을 파악한 뒤 수동으로 원본 Topic에 다시 발행한다.

```bash
# DLQ 메시지 확인
kafka-console-consumer --bootstrap-server localhost:9092 \
    --topic orders.DLQ --from-beginning

# 원인 파악 후 원본 Topic에 재발행
kafka-console-producer --bootstrap-server localhost:9092 \
    --topic orders
```

### 3. 자동 재처리 (Scheduled Replay)

일정 시간이 지난 DLQ 메시지를 자동으로 원본 Topic에 재발행하는 Consumer를 둔다. 외부 시스템 장애가 복구된 후 자동 재처리에 효과적이다.

```java
// DLQ 재처리 Consumer (스케줄러로 주기적 실행)
@Scheduled(fixedDelay = 300000) // 5분마다
public void replayDlqMessages() {
    ConsumerRecords<String, String> records = dlqConsumer.poll(Duration.ofSeconds(10));
    for (ConsumerRecord<String, String> record : records) {
        String originalTopic = new String(record.headers().lastHeader("original-topic").value());
        producer.send(new ProducerRecord<>(originalTopic, record.key(), record.value()));
    }
    dlqConsumer.commitSync();
}
```

> [!warning] 자동 재처리 주의점
> 재처리해도 계속 실패하는 메시지(데이터 포맷 오류, 비즈니스 규칙 위반 등)는 무한 루프에 빠질 수 있다. **재처리 횟수 제한**을 반드시 설정해야 한다.

### 4. 분석/로깅 용도

DLQ 메시지를 DB나 Elasticsearch에 적재하여 실패 패턴을 분석한다. 반복적으로 실패하는 유형을 찾아 근본 원인을 해결하는 데 활용한다.

---

## Spring Kafka의 DLQ 지원

Spring Kafka는 DLQ 패턴을 프레임워크 레벨에서 지원한다. 직접 구현할 필요 없이 설정으로 처리할 수 있다.

```java
@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        // DLQ로 보내는 recoverer
        DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLQ", record.partition()));

        // 3회 재시도 후 DLQ로 전송, 재시도 간격 1초
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            recoverer,
            new FixedBackOff(1000L, 3L)  // interval 1초, maxAttempts 3회
        );

        // 특정 예외는 재시도하지 않고 바로 DLQ로
        errorHandler.addNotRetryableExceptions(
            DeserializationException.class,    // 역직렬화 오류
            IllegalArgumentException.class     // 잘못된 데이터
        );

        return errorHandler;
    }
}
```

**Spring Kafka가 자동으로 해주는 것:**
- 실패 시 설정된 횟수만큼 재시도
- 재시도 소진 후 DLQ Topic으로 자동 발행
- 원본 메시지의 메타데이터를 Header에 자동 포함
- 특정 예외 유형은 재시도 없이 바로 DLQ로 라우팅

---

## DLQ Topic 네이밍 컨벤션

```
원본 Topic: orders
  → DLQ:     orders.DLQ
  → Retry:   orders.retry-1, orders.retry-2

원본 Topic: payments.processed
  → DLQ:     payments.processed.DLQ
```

원본 Topic 이름에 `.DLQ` 접미사를 붙이는 것이 일반적이다. 어떤 Topic의 실패 메시지인지 바로 파악할 수 있다.

---

## DLQ 설계 시 고려사항

### 1. DLQ에도 보존 기간을 설정한다

DLQ 메시지도 무한정 보관하면 디스크를 차지한다. 보통 원본 Topic보다 긴 보존 기간을 설정한다 (예: 원본 7일 → DLQ 30일).

### 2. DLQ 메시지의 순서는 보장하지 않는다

원본 Topic에서의 순서와 DLQ에서의 순서는 다를 수 있다. 재처리 시 순서에 의존하는 로직이 있다면 별도 처리가 필요하다.

### 3. DLQ를 처리하지 않으면 의미가 없다

DLQ를 만들어놓고 모니터링하지 않으면 메시지가 조용히 쌓이기만 한다. **알림 + 정기 리뷰 프로세스**가 반드시 함께 있어야 한다.

---

## 관련 문서

- [[dev/05-infrastructure/kafka/Kafka 아키텍처와 동작 원리|Kafka 아키텍처와 동작 원리]] — Kafka 전체 구조 및 동작
- [[dev/05-infrastructure/kafka/Kafka Exactly-once Semantics|Kafka Exactly-once Semantics]] — 메시지 전달 보장 수준
- [[dev/05-infrastructure/kafka/Kafka 메타데이터 관리, Zookeeper에서 KRaft로|Zookeeper에서 KRaft로]] — 클러스터 메타데이터 관리
- [[dev/05-infrastructure/monitoring|모니터링]] — DLQ 알림/모니터링 연동
