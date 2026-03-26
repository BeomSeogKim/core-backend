---
tags: [infrastructure, kafka, message-queue, distributed-system]
status: in-progress
created: 2026-03-24
---

# Kafka 아키텍처와 동작 원리

## 핵심 개념

**Apache Kafka**는 분산 이벤트 스트리밍 플랫폼이다. 전통적인 메시지 큐(RabbitMQ 등)와 달리, 메시지를 소비해도 삭제하지 않고 **append-only 로그** 형태로 보존하며, 각 Consumer Group이 독립적인 offset으로 같은 데이터를 소비할 수 있다.

### Kafka vs 전통 메시지 큐

| 구분 | 전통 MQ (RabbitMQ 등) | Kafka |
|------|----------------------|-------|
| 메시지 소비 후 | 큐에서 삭제 | 삭제하지 않음 (보존 기간까지 유지) |
| 소비 방식 | Broker가 Consumer에게 push | Consumer가 poll()로 pull |
| 다중 소비자 | 메시지당 하나의 Consumer만 소비 | 여러 Consumer Group이 독립 소비 |
| 순서 보장 | 큐 단위 보장 | Partition 단위 보장 |
| 재처리 | 불가 (삭제됨) | offset 리셋으로 재처리 가능 |

---

## 동작 원리

### 1. 전체 아키텍처

<div style="background:#1e1e2e; border-radius:12px; padding:24px; font-family:monospace; font-size:13px; color:#cdd6f4; overflow-x:auto;">
  <!-- Producers -->
  <div style="display:flex; justify-content:center; gap:16px; margin-bottom:12px;">
    <div style="background:#f38ba8; color:#1e1e2e; padding:8px 16px; border-radius:8px; font-weight:bold;">Producer A</div>
    <div style="background:#f38ba8; color:#1e1e2e; padding:8px 16px; border-radius:8px; font-weight:bold;">Producer B</div>
    <div style="background:#f38ba8; color:#1e1e2e; padding:8px 16px; border-radius:8px; font-weight:bold;">Producer C</div>
  </div>
  <div style="text-align:center; color:#6c7086; font-size:18px;">▼ ▼ ▼</div>
  <!-- Kafka Cluster -->
  <div style="background:#313244; border:2px solid #89b4fa; border-radius:12px; padding:20px; margin:12px 0;">
    <div style="text-align:center; color:#89b4fa; font-weight:bold; font-size:15px; margin-bottom:16px;">Kafka Cluster</div>
    <div style="text-align:center; color:#a6adc8; margin-bottom:8px;">Topic: orders</div>
    <!-- Partitions -->
    <div style="display:flex; flex-direction:column; gap:6px; margin:12px 0;">
      <div style="display:flex; align-items:center; gap:8px;">
        <span style="color:#a6adc8; width:90px;">Partition 0</span>
        <div style="display:flex; gap:2px;">
          <span style="background:#a6e3a1; color:#1e1e2e; padding:2px 8px; border-radius:4px;">0</span>
          <span style="background:#a6e3a1; color:#1e1e2e; padding:2px 8px; border-radius:4px;">1</span>
          <span style="background:#a6e3a1; color:#1e1e2e; padding:2px 8px; border-radius:4px;">2</span>
          <span style="background:#a6e3a1; color:#1e1e2e; padding:2px 8px; border-radius:4px;">3</span>
          <span style="background:#a6e3a1; color:#1e1e2e; padding:2px 8px; border-radius:4px;">4</span>
          <span style="background:#a6e3a1; color:#1e1e2e; padding:2px 8px; border-radius:4px;">5</span>
        </div>
        <span style="color:#6c7086;">← offset</span>
      </div>
      <div style="display:flex; align-items:center; gap:8px;">
        <span style="color:#a6adc8; width:90px;">Partition 1</span>
        <div style="display:flex; gap:2px;">
          <span style="background:#f9e2af; color:#1e1e2e; padding:2px 8px; border-radius:4px;">0</span>
          <span style="background:#f9e2af; color:#1e1e2e; padding:2px 8px; border-radius:4px;">1</span>
          <span style="background:#f9e2af; color:#1e1e2e; padding:2px 8px; border-radius:4px;">2</span>
          <span style="background:#f9e2af; color:#1e1e2e; padding:2px 8px; border-radius:4px;">3</span>
          <span style="background:#f9e2af; color:#1e1e2e; padding:2px 8px; border-radius:4px;">4</span>
        </div>
      </div>
      <div style="display:flex; align-items:center; gap:8px;">
        <span style="color:#a6adc8; width:90px;">Partition 2</span>
        <div style="display:flex; gap:2px;">
          <span style="background:#cba6f7; color:#1e1e2e; padding:2px 8px; border-radius:4px;">0</span>
          <span style="background:#cba6f7; color:#1e1e2e; padding:2px 8px; border-radius:4px;">1</span>
          <span style="background:#cba6f7; color:#1e1e2e; padding:2px 8px; border-radius:4px;">2</span>
          <span style="background:#cba6f7; color:#1e1e2e; padding:2px 8px; border-radius:4px;">3</span>
          <span style="background:#cba6f7; color:#1e1e2e; padding:2px 8px; border-radius:4px;">4</span>
          <span style="background:#cba6f7; color:#1e1e2e; padding:2px 8px; border-radius:4px;">5</span>
          <span style="background:#cba6f7; color:#1e1e2e; padding:2px 8px; border-radius:4px;">6</span>
        </div>
      </div>
    </div>
    <!-- Brokers -->
    <div style="display:flex; justify-content:center; gap:12px; margin-top:16px;">
      <div style="background:#45475a; padding:6px 14px; border-radius:6px; color:#89b4fa;">Broker 1</div>
      <div style="background:#45475a; padding:6px 14px; border-radius:6px; color:#89b4fa;">Broker 2</div>
      <div style="background:#45475a; padding:6px 14px; border-radius:6px; color:#89b4fa;">Broker 3</div>
    </div>
  </div>
  <div style="text-align:center; color:#6c7086; font-size:18px;">▼ ▼ ▼</div>
  <!-- Consumers -->
  <div style="border:2px dashed #a6e3a1; border-radius:12px; padding:16px; margin-top:12px;">
    <div style="text-align:center; color:#a6e3a1; font-weight:bold; margin-bottom:10px;">Consumer Group A</div>
    <div style="display:flex; justify-content:center; gap:16px;">
      <div style="background:#a6e3a1; color:#1e1e2e; padding:8px 12px; border-radius:8px; text-align:center;">Consumer 1<br/><small>P0 담당</small></div>
      <div style="background:#f9e2af; color:#1e1e2e; padding:8px 12px; border-radius:8px; text-align:center;">Consumer 2<br/><small>P1 담당</small></div>
      <div style="background:#cba6f7; color:#1e1e2e; padding:8px 12px; border-radius:8px; text-align:center;">Consumer 3<br/><small>P2 담당</small></div>
    </div>
  </div>
</div>

### 2. 핵심 구성 요소

#### Broker

Kafka 서버 인스턴스. 클러스터는 여러 Broker로 구성되며, 각 Broker가 Partition의 일부를 저장하고 관리한다. Broker 하나가 죽어도 다른 Broker가 복제본(Replica)을 갖고 있어 데이터 유실을 방지한다.

#### Topic & Partition

```
Topic: "orders"
├── Partition 0  →  [msg0, msg1, msg2, msg3, ...]   ← append-only log
├── Partition 1  →  [msg0, msg1, msg2, ...]
└── Partition 2  →  [msg0, msg1, msg2, msg3, ...]
```

- **Topic**: 메시지의 논리적 카테고리 (예: `orders`, `payments`)
- **Partition**: Topic을 쪼갠 물리적 단위. 병렬 처리의 핵심
- 각 Partition 내에서 메시지 순서가 보장됨 (Topic 전체에서는 보장되지 않음)

#### Consumer Group & Offset

<div style="background:#1e1e2e; border-radius:12px; padding:20px; font-family:monospace; font-size:13px; color:#cdd6f4;">
  <!-- Group A -->
  <div style="border:2px solid #a6e3a1; border-radius:10px; padding:14px; margin-bottom:14px;">
    <div style="color:#a6e3a1; font-weight:bold; margin-bottom:8px;">Consumer Group A (주문 처리 서비스)</div>
    <div style="display:flex; flex-direction:column; gap:4px; padding-left:12px;">
      <span>Consumer 1 → Partition 0 <span style="color:#6c7086;">(offset: 5까지 읽음)</span></span>
      <span>Consumer 2 → Partition 1 <span style="color:#6c7086;">(offset: 3까지 읽음)</span></span>
      <span>Consumer 3 → Partition 2 <span style="color:#6c7086;">(offset: 6까지 읽음)</span></span>
    </div>
  </div>
  <!-- Group B -->
  <div style="border:2px solid #f9e2af; border-radius:10px; padding:14px;">
    <div style="color:#f9e2af; font-weight:bold; margin-bottom:8px;">Consumer Group B (분석 서비스) ← 같은 Topic을 독립적으로 소비</div>
    <div style="display:flex; flex-direction:column; gap:4px; padding-left:12px;">
      <span>Consumer 4 → Partition 0, 1 <span style="color:#6c7086;">(offset: 각각 2, 1)</span></span>
      <span>Consumer 5 → Partition 2 <span style="color:#6c7086;">(offset: 4)</span></span>
    </div>
  </div>
</div>

- **Offset**은 **Consumer Group 단위**로 관리된다
- 같은 Group 내에서는 하나의 Partition을 한 Consumer만 읽음 → 병렬 처리
- 다른 Group끼리는 같은 메시지를 독립적으로 소비 가능
- **Consumer 수 ≤ Partition 수**로 설계해야 한다. 초과하는 Consumer는 유휴 상태

---

### 3. 메시지 흐름

<div style="background:#1e1e2e; border-radius:12px; padding:20px; font-family:monospace; font-size:13px; color:#cdd6f4;">
  <div style="display:flex; justify-content:space-between; margin-bottom:16px; font-weight:bold;">
    <span style="color:#f38ba8;">Producer</span>
    <span style="color:#89b4fa;">Kafka</span>
    <span style="color:#a6e3a1;">Consumer</span>
  </div>
  <div style="display:flex; flex-direction:column; gap:8px;">
    <div style="background:#313244; padding:8px 14px; border-radius:8px; border-left:3px solid #f38ba8;">
      <span style="color:#f9e2af;">①</span> Producer → send(topic, key, value)
    </div>
    <div style="background:#313244; padding:8px 14px; border-radius:8px; border-left:3px solid #89b4fa;">
      <span style="color:#f9e2af;">②</span> key로 Partition 결정 — <code>hash(key) % partition 수</code>
    </div>
    <div style="background:#313244; padding:8px 14px; border-radius:8px; border-left:3px solid #89b4fa;">
      <span style="color:#f9e2af;">③</span> Leader Partition에 append
    </div>
    <div style="background:#313244; padding:8px 14px; border-radius:8px; border-left:3px solid #89b4fa;">
      <span style="color:#f9e2af;">④</span> Follower가 복제
    </div>
    <div style="background:#313244; padding:8px 14px; border-radius:8px; border-left:3px solid #f38ba8;">
      <span style="color:#f9e2af;">⑤</span> Producer ← ack 반환
    </div>
    <div style="background:#313244; padding:8px 14px; border-radius:8px; border-left:3px solid #a6e3a1;">
      <span style="color:#f9e2af;">⑥</span> Consumer → poll()로 메시지 가져감
    </div>
    <div style="background:#313244; padding:8px 14px; border-radius:8px; border-left:3px solid #a6e3a1;">
      <span style="color:#f9e2af;">⑦</span> 처리 후 offset commit
    </div>
  </div>
</div>

### 4. Partition 라우팅 (Key 기반)

- **key 있음**: `hash(key) % partition 수` → 같은 key는 항상 같은 Partition → **순서 보장**
- **key 없음 (null)**: Round-Robin으로 Partition에 골고루 분배

> [!warning] Partition 수 변경 주의
> Partition 수를 변경하면 `hash(key) % partition 수` 결과가 달라져 기존 key의 라우팅이 전면 재분배된다. **순서 보장이 깨질 수 있으므로** Partition 수는 처음에 넉넉하게 잡고 가급적 변경하지 않는다.

---

## Kafka가 빠른 이유

### 1. Sequential I/O (순차 쓰기)

디스크에 랜덤으로 쓰면 느리지만, 순차적으로 append만 하면 메모리에 근접하는 속도가 나온다. Kafka는 메시지를 로그 끝에 이어붙이기만 한다 (append-only log).

### 2. Zero-Copy

일반적인 데이터 전송은 4번의 복사가 발생한다:

<div style="background:#1e1e2e; border-radius:12px; padding:20px; font-family:monospace; font-size:13px; color:#cdd6f4;">
  <!-- 일반 전송 -->
  <div style="margin-bottom:20px;">
    <div style="color:#f38ba8; font-weight:bold; margin-bottom:12px;">일반적인 데이터 전송 — 4번 복사</div>
    <div style="display:flex; align-items:center; gap:6px; flex-wrap:wrap;">
      <div style="background:#45475a; padding:8px 12px; border-radius:6px;">디스크</div>
      <span style="color:#f38ba8;">→①→</span>
      <div style="background:#45475a; padding:8px 12px; border-radius:6px;">커널 Read Buffer</div>
      <span style="color:#f38ba8;">→②→</span>
      <div style="background:#f38ba8; color:#1e1e2e; padding:8px 12px; border-radius:6px; font-weight:bold;">JVM App Buffer</div>
      <span style="color:#f38ba8;">→③→</span>
      <div style="background:#45475a; padding:8px 12px; border-radius:6px;">커널 Socket Buffer</div>
      <span style="color:#f38ba8;">→④→</span>
      <div style="background:#45475a; padding:8px 12px; border-radius:6px;">NIC (네트워크)</div>
    </div>
  </div>
  <!-- Zero-Copy -->
  <div>
    <div style="color:#a6e3a1; font-weight:bold; margin-bottom:12px;">Kafka Zero-Copy — sendfile()</div>
    <div style="display:flex; align-items:center; gap:6px; flex-wrap:wrap;">
      <div style="background:#45475a; padding:8px 12px; border-radius:6px;">디스크</div>
      <span style="color:#a6e3a1;">→①→</span>
      <div style="background:#45475a; padding:8px 12px; border-radius:6px;">커널 Read Buffer</div>
      <span style="color:#a6e3a1;">— — — 직접 전송 — — —→②→</span>
      <div style="background:#45475a; padding:8px 12px; border-radius:6px;">NIC (네트워크)</div>
    </div>
    <div style="color:#6c7086; margin-top:8px; padding-left:4px;">유저 영역(JVM)을 완전히 건너뜀 → CPU/메모리 오버헤드 최소화</div>
  </div>
</div>

- **일반**: 디스크 → 커널 → JVM → 커널 → 네트워크 (4회 복사, CPU 개입)
- **Kafka**: 디스크 → 커널 → 네트워크 (2회 복사, CPU 최소)

### 3. Batching

메시지를 하나씩 보내지 않고 묶어서 한 번에 전송하며, 압축까지 적용해 네트워크 오버헤드를 줄인다.

---

## Replication & 데이터 유실 방지

### Replication 구조

<div style="background:#1e1e2e; border-radius:12px; padding:20px; font-family:monospace; font-size:13px; color:#cdd6f4;">
  <div style="color:#89b4fa; font-weight:bold; margin-bottom:12px;">Partition 0 — Replication Factor = 3</div>
  <div style="display:flex; flex-direction:column; gap:8px;">
    <div style="display:flex; align-items:center; gap:10px;">
      <div style="background:#f9e2af; color:#1e1e2e; padding:8px 14px; border-radius:8px; font-weight:bold; min-width:90px; text-align:center;">Broker 1</div>
      <span style="background:#a6e3a1; color:#1e1e2e; padding:4px 10px; border-radius:6px; font-weight:bold;">Leader</span>
      <span style="color:#6c7086;">← Producer/Consumer는 여기에 읽기/쓰기</span>
    </div>
    <div style="display:flex; align-items:center; gap:10px;">
      <div style="background:#45475a; padding:8px 14px; border-radius:8px; min-width:90px; text-align:center;">Broker 2</div>
      <span style="background:#6c7086; color:#cdd6f4; padding:4px 10px; border-radius:6px;">Follower</span>
      <span style="color:#6c7086;">← Leader를 복제</span>
    </div>
    <div style="display:flex; align-items:center; gap:10px;">
      <div style="background:#45475a; padding:8px 14px; border-radius:8px; min-width:90px; text-align:center;">Broker 3</div>
      <span style="background:#6c7086; color:#cdd6f4; padding:4px 10px; border-radius:6px;">Follower</span>
      <span style="color:#6c7086;">← Leader를 복제</span>
    </div>
  </div>
</div>

- 각 Partition에는 **Leader** 1개 + **Follower** N개
- Producer/Consumer는 Leader와만 통신
- Leader가 죽으면 Follower 중 하나가 새 Leader로 승격

### ISR (In-Sync Replicas)

Leader와 동기화가 잘 되고 있는 Follower들의 집합이다. Follower가 너무 뒤처지면 ISR에서 제외된다.

### acks 설정

| acks | 동작 | 특징 |
|------|------|------|
| `0` | 보내고 확인 안 함 | 가장 빠름, 유실 가능 |
| `1` | Leader가 저장하면 확인 | Leader 죽으면 유실 가능 |
| `all` | Leader + 모든 ISR이 저장하면 확인 | 가장 안전, 상대적으로 느림 |

### 안전한 운영 설정 조합

```properties
replication.factor=3          # Leader 1 + Follower 2 = 총 3개 복제본
acks=all                      # 모든 ISR이 저장 완료해야 ack
min.insync.replicas=2         # ISR이 최소 2개 이상이어야 쓰기 허용
```

> [!warning] acks=all + ISR이 Leader만 남은 경우
> `acks=all`이라도 ISR에 Leader만 남아있으면 사실상 `acks=1`과 동일해진다. `min.insync.replicas=2`를 설정하면 ISR이 2개 미만일 때 **쓰기를 거부**(에러 반환)하여 데이터 유실을 방지한다.

---

## Rebalancing

### 발생 조건

- Consumer가 죽거나 새로 추가될 때
- Consumer Group 내 Partition이 재분배됨

### 문제점

**1. Stop-the-World**

Rebalancing 중에는 해당 Consumer Group **전체가 메시지 소비를 멈춘다**.

**2. 중복 소비 (Duplicate Processing)**

<div style="background:#1e1e2e; border-radius:12px; padding:20px; font-family:monospace; font-size:13px; color:#cdd6f4;">
  <div style="display:flex; flex-direction:column; gap:6px;">
    <div style="display:flex; align-items:center; gap:8px;">
      <span style="color:#a6e3a1;">①</span>
      <span>Consumer A — Partition 0에서 offset 5까지 처리</span>
    </div>
    <div style="display:flex; align-items:center; gap:8px;">
      <span style="color:#f38ba8;">②</span>
      <span style="color:#f38ba8;">offset commit 전에 Consumer A 죽음</span>
    </div>
    <div style="display:flex; align-items:center; gap:8px;">
      <span style="color:#f9e2af;">③</span>
      <span>Rebalancing → Consumer B가 Partition 0을 맡음</span>
    </div>
    <div style="display:flex; align-items:center; gap:8px;">
      <span style="color:#f9e2af;">④</span>
      <span>Consumer B는 마지막 commit된 offset <strong>3</strong>부터 다시 읽음</span>
    </div>
    <div style="display:flex; align-items:center; gap:8px;">
      <span style="color:#f38ba8;">⑤</span>
      <span style="color:#f38ba8;">offset 3, 4, 5 메시지가 <strong>중복 처리</strong>됨</span>
    </div>
  </div>
</div>

### 중복 소비 해결: 멱등성 (Idempotency)

같은 메시지를 여러 번 처리해도 결과가 동일하게 만든다.

**Consumer 측:**
- DB에 **unique key** (예: 주문 ID)로 저장 → 중복 insert 시 무시
- 별도 **처리 완료 테이블**에 메시지 ID 기록 → 이미 존재하면 skip

**Producer 측:**
- `enable.idempotence=true` 설정 → 네트워크 재전송으로 인한 중복 메시지 발행 방지

---

## 데이터 보존

- 메시지는 소비 여부와 관계없이 **설정된 기간**(기본 7일) 또는 **용량**까지 보존
- Consumer가 죽었다 살아나도 마지막 commit offset부터 다시 읽기 가능
- offset을 리셋하면 과거 메시지 재처리도 가능

---

## 관련 문서

- [[2-Areas/backend/05-infrastructure/kafka/Kafka DLQ와 에러 처리|Kafka DLQ와 에러 처리]] — DLQ 패턴, Retry 전략, Spring Kafka 지원
- [[2-Areas/backend/05-infrastructure/kafka/Kafka Exactly-once Semantics|Kafka Exactly-once Semantics]] — 메시지 전달 보장, Idempotent Producer, Transaction
- [[2-Areas/backend/05-infrastructure/kafka/Kafka 메타데이터 관리, Zookeeper에서 KRaft로|Zookeeper에서 KRaft로]] — 클러스터 메타데이터 관리 방식 변화
- [[2-Areas/backend/04-architecture/마이크로서비스|마이크로서비스]] — Kafka가 주로 사용되는 MSA 환경
- [[2-Areas/backend/04-architecture/이벤트 드리븐 아키텍처|이벤트 드리븐 아키텍처]] — Kafka 기반 EDA 패턴
- [[2-Areas/backend/03-database/트랜잭션|트랜잭션]] — 분산 트랜잭션과 Kafka의 관계
- [[2-Areas/backend/06-computer-science/네트워크|네트워크]] — Zero-Copy, sendfile() 등 OS 레벨 최적화
- [[2-Areas/backend/09-system-design/building-blocks/Message Queue|Message Queue]] — 시스템 설계에서의 메시지 큐 활용
