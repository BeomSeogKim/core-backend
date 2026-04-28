---
tags: [infrastructure, kafka, zookeeper, kraft, metadata]
status: in-progress
created: 2026-03-24
---

# Kafka 메타데이터 관리: Zookeeper에서 KRaft로

## 핵심 개념

Kafka 클러스터를 운영하려면 **메타데이터**(Topic 목록, Partition 정보, Broker 상태, Leader 선출 등)를 관리하는 시스템이 필요하다. 과거에는 **Apache Zookeeper**라는 외부 시스템이 이 역할을 했으나, Kafka 3.x부터 Kafka 자체 내장 합의 프로토콜인 **KRaft (Kafka Raft)**로 대체되었다.

---

## 메타데이터란?

Kafka 클러스터가 정상 동작하려면 다음 정보를 어딘가에 저장하고 관리해야 한다:

| 메타데이터 | 내용 | 예시 |
|-----------|------|------|
| **Broker 등록** | 어떤 Broker가 클러스터에 참여 중인지 | Broker 1, 2, 3 alive |
| **Topic 설정** | Topic 목록, Partition 수, Replication factor | orders: 3 partitions, RF=3 |
| **Partition 배치** | 각 Partition의 Leader/Follower가 어느 Broker에 있는지 | P0: Leader=B1, Followers=B2,B3 |
| **ISR 목록** | 각 Partition의 In-Sync Replicas | P0 ISR: {B1, B2, B3} |
| **ACL** | 접근 제어 목록 | user-A: read orders |
| **Consumer Group** | 각 Group의 offset 정보 | group-A: P0=offset 5 |

---

## Zookeeper 방식 (기존)

### Zookeeper란?

**Apache Zookeeper**는 분산 시스템을 위한 **중앙화된 코디네이션 서비스**다. Kafka 전용이 아니라 Hadoop, HBase 등 여러 분산 시스템에서 사용된다.

### Kafka에서의 Zookeeper 역할

<div style="background:#1e1e2e; border-radius:12px; padding:24px; font-family:monospace; font-size:13px; color:#cdd6f4; overflow-x:auto;">
  <div style="display:flex; gap:20px; align-items:stretch; flex-wrap:wrap; justify-content:center;">
    <!-- Kafka Cluster -->
    <div style="background:#313244; border:2px solid #89b4fa; border-radius:12px; padding:16px; flex:1; min-width:200px;">
      <div style="color:#89b4fa; font-weight:bold; text-align:center; margin-bottom:12px;">Kafka Cluster</div>
      <div style="display:flex; flex-direction:column; gap:6px;">
        <div style="background:#45475a; padding:6px 12px; border-radius:6px; text-align:center;">Broker 1</div>
        <div style="background:#45475a; padding:6px 12px; border-radius:6px; text-align:center;">Broker 2</div>
        <div style="background:#45475a; padding:6px 12px; border-radius:6px; text-align:center;">Broker 3</div>
      </div>
    </div>
    <!-- 화살표 -->
    <div style="display:flex; align-items:center; color:#f9e2af; font-size:20px;">⟷</div>
    <!-- Zookeeper -->
    <div style="background:#313244; border:2px solid #f9e2af; border-radius:12px; padding:16px; flex:1; min-width:200px;">
      <div style="color:#f9e2af; font-weight:bold; text-align:center; margin-bottom:12px;">Zookeeper Cluster (별도 운영)</div>
      <div style="display:flex; flex-direction:column; gap:6px;">
        <div style="background:#45475a; padding:6px 12px; border-radius:6px; text-align:center;">ZK Node 1</div>
        <div style="background:#45475a; padding:6px 12px; border-radius:6px; text-align:center;">ZK Node 2</div>
        <div style="background:#45475a; padding:6px 12px; border-radius:6px; text-align:center;">ZK Node 3</div>
      </div>
      <div style="color:#6c7086; text-align:center; margin-top:10px; font-size:11px;">메타데이터 저장<br/>Leader 선출<br/>Broker 감시</div>
    </div>
  </div>
</div>

**1. Broker 등록 및 감시**

각 Broker는 시작할 때 Zookeeper에 **Ephemeral Node**를 생성한다. Broker가 죽으면 이 노드가 자동으로 삭제되어 Zookeeper가 장애를 감지한다.

```
/brokers/ids/
├── 1   ← Broker 1 살아있음 (ephemeral)
├── 2   ← Broker 2 살아있음 (ephemeral)
└── 3   ← Broker 3 살아있음 (ephemeral)
```

**2. Controller 선출**

Broker 중 하나가 **Controller** 역할을 맡는다. Controller는 Partition Leader 선출, Broker 장애 대응 등 클러스터 관리를 담당한다. Controller가 죽으면 Zookeeper가 남은 Broker 중에서 새 Controller를 선출한다.

```
/controller → {"brokerId": 1}   ← Broker 1이 현재 Controller
```

**3. Partition Leader 선출**

Controller가 Zookeeper의 메타데이터를 보고 각 Partition의 Leader를 결정한다. Leader Broker가 죽으면 ISR 목록에서 새 Leader를 선출한다.

**4. 메타데이터 저장**

Topic, Partition, 설정 정보 등을 Zookeeper의 ZNode 트리 구조에 저장한다.

```
/brokers/
├── ids/           ← 살아있는 Broker 목록
├── topics/        ← Topic별 Partition 정보
│   ├── orders/
│   │   └── partitions/
│   │       ├── 0/state  → {"leader":1, "isr":[1,2,3]}
│   │       ├── 1/state  → {"leader":2, "isr":[2,3,1]}
│   │       └── 2/state  → {"leader":3, "isr":[3,1,2]}
│   └── payments/
└── ...
```

### Zookeeper의 문제점

#### 1. 운영 복잡도 증가

Kafka Cluster와 Zookeeper Cluster를 **별도로 배포, 설정, 모니터링, 업그레이드**해야 한다. 사실상 두 개의 분산 시스템을 운영하는 것이다.

```
운영해야 할 것:
├── Kafka Cluster (3+ Brokers)
│   ├── 설정 관리
│   ├── 모니터링
│   └── 버전 업그레이드
└── Zookeeper Cluster (3+ Nodes)   ← 이것도 별도로!
    ├── 설정 관리
    ├── 모니터링
    └── 버전 업그레이드
```

#### 2. SPOF (Single Point of Failure) 위험

Zookeeper 클러스터가 다운되면 Kafka의 메타데이터 관리가 불가능해진다:
- 새로운 Topic 생성 불가
- Partition Leader 선출 불가 (기존 Leader가 죽으면 복구 불가)
- 새 Broker 합류 불가

기존 동작 중인 메시지 읽기/쓰기는 영향이 적지만, **장애 복구 능력을 상실**한다.

#### 3. 확장성 한계

Zookeeper는 모든 메타데이터를 **메모리에 로드**하고 ZNode 변경을 **모든 Follower에게 동기화**한다. Broker 수, Topic 수, Partition 수가 늘어나면:

- 메타데이터 동기화 트래픽 증가
- Controller failover 시간 증가 (수십만 Partition이면 수분 소요)
- Zookeeper 자체의 쓰기 성능이 병목

#### 4. Kafka와 Zookeeper 간 메타데이터 불일치

Kafka Broker와 Zookeeper 각각이 메타데이터를 캐싱하기 때문에 일시적으로 **불일치(split-brain)**가 발생할 수 있다.

---

## KRaft 방식 (신규)

### KRaft란?

**KRaft (Kafka Raft)**는 Kafka 자체에 **Raft 합의 알고리즘**을 내장하여, Zookeeper 없이 메타데이터를 관리하는 방식이다. Kafka 2.8에서 Early Access로 도입, Kafka 3.3부터 Production Ready, Kafka 4.0에서 Zookeeper 완전 제거 예정이다.

### 구조

<div style="background:#1e1e2e; border-radius:12px; padding:24px; font-family:monospace; font-size:13px; color:#cdd6f4; overflow-x:auto;">
  <div style="background:#313244; border:2px solid #a6e3a1; border-radius:12px; padding:20px;">
    <div style="color:#a6e3a1; font-weight:bold; text-align:center; font-size:15px; margin-bottom:16px;">Kafka Cluster (KRaft 모드) — Zookeeper 없음!</div>
    <!-- Controller 노드 -->
    <div style="border:2px dashed #f9e2af; border-radius:10px; padding:14px; margin-bottom:14px;">
      <div style="color:#f9e2af; font-weight:bold; margin-bottom:10px;">Controller 노드 (Raft Quorum)</div>
      <div style="display:flex; justify-content:center; gap:12px;">
        <div style="background:#f9e2af; color:#1e1e2e; padding:8px 14px; border-radius:8px; font-weight:bold; text-align:center;">Controller 1<br/><small>Active</small></div>
        <div style="background:#45475a; padding:8px 14px; border-radius:8px; text-align:center;">Controller 2<br/><small>Standby</small></div>
        <div style="background:#45475a; padding:8px 14px; border-radius:8px; text-align:center;">Controller 3<br/><small>Standby</small></div>
      </div>
      <div style="color:#6c7086; text-align:center; margin-top:8px; font-size:11px;">메타데이터를 Raft 프로토콜로 복제/합의</div>
    </div>
    <!-- Broker 노드 -->
    <div style="border:2px dashed #89b4fa; border-radius:10px; padding:14px;">
      <div style="color:#89b4fa; font-weight:bold; margin-bottom:10px;">Broker 노드</div>
      <div style="display:flex; justify-content:center; gap:12px;">
        <div style="background:#89b4fa; color:#1e1e2e; padding:8px 14px; border-radius:8px;">Broker 1</div>
        <div style="background:#89b4fa; color:#1e1e2e; padding:8px 14px; border-radius:8px;">Broker 2</div>
        <div style="background:#89b4fa; color:#1e1e2e; padding:8px 14px; border-radius:8px;">Broker 3</div>
      </div>
      <div style="color:#6c7086; text-align:center; margin-top:8px; font-size:11px;">메시지 저장 및 처리</div>
    </div>
  </div>
</div>

### 핵심 변경: Controller Quorum

KRaft에서는 **Controller 노드들이 Raft 프로토콜로 Quorum을 형성**하여 메타데이터를 관리한다.

- **Active Controller**: 1개. 메타데이터 변경 요청을 처리
- **Standby Controller**: N개. Active의 메타데이터를 실시간 복제. Active가 죽으면 즉시 승격
- Controller는 전용 노드일 수도 있고, **Broker가 Controller 역할을 겸할 수도** 있다

### 배포 모드

#### Combined 모드 (소규모)

Broker가 Controller 역할을 겸한다. 별도 노드가 필요 없어 자원을 절약한다.

```properties
# 하나의 노드가 broker + controller 역할을 모두 수행
process.roles=broker,controller
```

```
Node 1: Broker + Controller
Node 2: Broker + Controller
Node 3: Broker + Controller
```

#### Isolated 모드 (대규모, 권장)

Controller와 Broker를 분리한다. Controller는 메타데이터만 관리하고, Broker는 메시지 처리에 집중한다.

```properties
# Controller 전용 노드
process.roles=controller

# Broker 전용 노드
process.roles=broker
```

```
Controller 1, 2, 3  ← 메타데이터 관리 전용
Broker 1, 2, 3, ... ← 메시지 처리 전용
```

### 메타데이터 저장: __cluster_metadata

Zookeeper는 ZNode 트리에 메타데이터를 저장했지만, KRaft는 **`__cluster_metadata`라는 내부 Kafka Topic**에 저장한다. 이 Topic은 Kafka의 로그 구조(append-only)를 그대로 활용한다.

<div style="background:#1e1e2e; border-radius:12px; padding:20px; font-family:monospace; font-size:13px; color:#cdd6f4;">
  <div style="color:#89b4fa; font-weight:bold; margin-bottom:12px;">__cluster_metadata Topic</div>
  <div style="display:flex; flex-direction:column; gap:4px;">
    <div style="display:flex; align-items:center; gap:8px;">
      <span style="background:#a6e3a1; color:#1e1e2e; padding:2px 8px; border-radius:4px;">0</span>
      <span>Topic "orders" 생성 (partitions=3, RF=3)</span>
    </div>
    <div style="display:flex; align-items:center; gap:8px;">
      <span style="background:#a6e3a1; color:#1e1e2e; padding:2px 8px; border-radius:4px;">1</span>
      <span>Broker 2 등록</span>
    </div>
    <div style="display:flex; align-items:center; gap:8px;">
      <span style="background:#a6e3a1; color:#1e1e2e; padding:2px 8px; border-radius:4px;">2</span>
      <span>Partition 0 Leader 변경: Broker 1 → Broker 3</span>
    </div>
    <div style="display:flex; align-items:center; gap:8px;">
      <span style="background:#a6e3a1; color:#1e1e2e; padding:2px 8px; border-radius:4px;">3</span>
      <span>Topic "payments" 생성 (partitions=5, RF=2)</span>
    </div>
    <div style="display:flex; align-items:center; gap:8px;">
      <span style="color:#6c7086;">...</span>
      <span style="color:#6c7086;">이벤트 로그 형태로 계속 append</span>
    </div>
  </div>
</div>

**이벤트 소싱 방식**: 현재 상태를 "스냅샷"으로 저장하는 것이 아니라, **변경 이벤트를 순서대로 기록**한다. 현재 상태가 필요하면 이벤트를 처음부터 재생(replay)하면 된다. 주기적으로 **스냅샷**을 찍어 복구 시간을 단축한다.

---

## Zookeeper vs KRaft 비교

| 비교 항목 | Zookeeper 방식 | KRaft 방식 |
|----------|--------------|-----------|
| **외부 의존성** | Zookeeper Cluster 별도 운영 필요 | 없음. Kafka만으로 완결 |
| **메타데이터 저장** | Zookeeper ZNode 트리 | `__cluster_metadata` 내부 Topic |
| **Controller 선출** | Zookeeper를 통해 선출 | Raft 합의 알고리즘으로 자체 선출 |
| **Controller failover** | 수초~수분 (Partition 수에 비례) | 수초 이내 (Standby가 즉시 승격) |
| **확장성** | 수만 Partition에서 병목 | 수백만 Partition 지원 가능 |
| **메타데이터 일관성** | Kafka-Zookeeper 간 불일치 가능 | 단일 소스 → 불일치 없음 |
| **운영 복잡도** | 2개 시스템 관리 | 1개 시스템 관리 |
| **모니터링** | Kafka + Zookeeper 각각 모니터링 | Kafka만 모니터링 |

### Controller Failover 성능 차이

이것이 실무에서 가장 큰 체감 차이다:

<div style="background:#1e1e2e; border-radius:12px; padding:20px; font-family:monospace; font-size:13px; color:#cdd6f4;">
  <!-- Zookeeper -->
  <div style="border:2px solid #f38ba8; border-radius:10px; padding:14px; margin-bottom:14px;">
    <div style="color:#f38ba8; font-weight:bold; margin-bottom:8px;">Zookeeper 방식 — Controller Failover</div>
    <div style="display:flex; flex-direction:column; gap:4px; padding-left:8px;">
      <span><span style="color:#f9e2af;">①</span> Controller 죽음 감지 (Zookeeper session timeout)</span>
      <span><span style="color:#f9e2af;">②</span> Zookeeper에서 새 Controller 선출</span>
      <span><span style="color:#f9e2af;">③</span> 새 Controller가 Zookeeper에서 <strong>모든 메타데이터를 다시 읽음</strong></span>
      <span><span style="color:#f38ba8;">④</span> Partition 수가 많을수록 ③이 오래 걸림 (10만 Partition → 수분)</span>
      <span style="color:#6c7086; margin-top:4px;">→ 이 동안 Leader 선출 불가, 장애 복구 지연</span>
    </div>
  </div>
  <!-- KRaft -->
  <div style="border:2px solid #a6e3a1; border-radius:10px; padding:14px;">
    <div style="color:#a6e3a1; font-weight:bold; margin-bottom:8px;">KRaft 방식 — Controller Failover</div>
    <div style="display:flex; flex-direction:column; gap:4px; padding-left:8px;">
      <span><span style="color:#f9e2af;">①</span> Active Controller 죽음</span>
      <span><span style="color:#a6e3a1;">②</span> Standby Controller가 <strong>이미 모든 메타데이터를 갖고 있음</strong></span>
      <span><span style="color:#a6e3a1;">③</span> Raft 프로토콜로 즉시 승격 → <strong>수초 이내 완료</strong></span>
      <span style="color:#6c7086; margin-top:4px;">→ Partition 수와 무관하게 일정한 failover 시간</span>
    </div>
  </div>
</div>

---

## Raft 합의 알고리즘 핵심

KRaft의 기반이 되는 **Raft** 알고리즘의 핵심만 간단히 정리한다:

### Leader 선출

1. 모든 Controller 노드는 **Follower** 상태로 시작
2. Leader로부터 heartbeat가 오지 않으면 **Candidate**로 전환
3. Candidate는 다른 노드에게 투표 요청
4. **과반수**의 투표를 받으면 **Leader**로 승격
5. Leader는 주기적으로 heartbeat를 보냄

### 로그 복제

1. Leader가 메타데이터 변경 요청을 받음
2. 변경 내용을 자신의 로그에 기록
3. 모든 Follower에게 복제 요청
4. **과반수**가 기록 완료하면 → commit 처리
5. commit된 내용을 Follower에게 알림

이 과정 덕분에 Leader가 죽어도 과반수의 Follower가 최신 데이터를 갖고 있어 즉시 새 Leader를 선출할 수 있다.

> [!note] Quorum 크기
> Controller 노드는 보통 3개 또는 5개로 구성한다.
> - 3개: 1개 장애 허용 (과반수 = 2)
> - 5개: 2개 장애 허용 (과반수 = 3)

---

## 마이그레이션: Zookeeper → KRaft

기존 Zookeeper 기반 클러스터를 KRaft로 마이그레이션하는 과정:

<div style="background:#1e1e2e; border-radius:12px; padding:20px; font-family:monospace; font-size:13px; color:#cdd6f4;">
  <div style="display:flex; flex-direction:column; gap:8px;">
    <div style="background:#313244; padding:10px 14px; border-radius:8px; border-left:3px solid #f9e2af;">
      <span style="color:#f9e2af;">Phase 1</span> — KRaft Controller 노드 추가 (Zookeeper와 병행 운영)
    </div>
    <div style="background:#313244; padding:10px 14px; border-radius:8px; border-left:3px solid #f9e2af;">
      <span style="color:#f9e2af;">Phase 2</span> — Zookeeper의 메타데이터를 KRaft로 마이그레이션
    </div>
    <div style="background:#313244; padding:10px 14px; border-radius:8px; border-left:3px solid #f9e2af;">
      <span style="color:#f9e2af;">Phase 3</span> — Broker를 KRaft 모드로 순차 재시작 (Rolling Restart)
    </div>
    <div style="background:#313244; padding:10px 14px; border-radius:8px; border-left:3px solid #a6e3a1;">
      <span style="color:#a6e3a1;">Phase 4</span> — Zookeeper Cluster 제거
    </div>
  </div>
  <div style="color:#6c7086; margin-top:12px;">다운타임 없이 마이그레이션 가능 (Rolling 방식)</div>
</div>

Kafka 공식 도구인 `kafka-metadata.sh`를 사용하여 마이그레이션할 수 있다.

---

## 관련 문서

- [[dev/05-infrastructure/kafka/Kafka 아키텍처와 동작 원리|Kafka 아키텍처와 동작 원리]] — Kafka 전체 구조 및 동작
- [[dev/05-infrastructure/kafka/Kafka Exactly-once Semantics|Kafka Exactly-once Semantics]] — Transaction Coordinator와 메타데이터 관리
- [[dev/05-infrastructure/kafka/Kafka DLQ와 에러 처리|Kafka DLQ와 에러 처리]] — 에러 처리 전략
- [[dev/04-architecture/마이크로서비스|마이크로서비스]] — 분산 시스템 코디네이션
