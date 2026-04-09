---
tags: [database, replication, sharding, partitioning, scaling]
status: completed
created: 2026-04-04
---

# Replication · 샤딩 · 파티셔닝

## 핵심 개념

DB 확장 전략 세 가지. **Replication**은 데이터를 복제하여 읽기 분산 + 가용성 확보, **샤딩**은 데이터를 여러 서버에 나눠 저장하여 쓰기 분산 + 용량 확장, **파티셔닝**은 하나의 서버 내에서 테이블을 분할하여 쿼리 성능을 향상시킨다.

## 동작 원리

### Replication — 데이터 복제

DB를 **복제**하여 Master(쓰기)와 Replica(읽기)로 역할을 분리한다.

```
Client ─── 쓰기(INSERT/UPDATE/DELETE) ──→ [Master]
Client ─── 읽기(SELECT) ──→ [Replica 1]
                             [Replica 2]

Master ──── 비동기 복제 ──→ Replica 1
                       └──→ Replica 2
```

| 항목 | 설명 |
|------|------|
| 목적 | **읽기 분산** + 장애 시 **고가용성** (Failover) |
| 데이터 | 모든 서버에 **전체 데이터 복사** |
| 쓰기 성능 | 향상 안 됨 (Master 하나에만 쓰기) |
| 읽기 성능 | **향상** (Replica 수만큼 분산) |
| 주의 | 복제 지연(Replication Lag) — 쓰기 직후 Replica에서 읽으면 구 데이터 가능 |

### 샤딩 (Sharding) — 수평 분산

데이터를 **수평 분할**하여 여러 DB 서버(Shard)에 나눠 저장한다. 각 Shard는 데이터의 일부만 담당한다.

```
Shard Key: user_id

user_id 1~1000      → [Shard 1]
user_id 1001~2000   → [Shard 2]
user_id 2001~3000   → [Shard 3]
```

| 항목 | 설명 |
|------|------|
| 목적 | **쓰기 분산** + 저장 용량 확장 |
| 데이터 | 각 서버에 **일부 데이터** |
| Shard Key | 데이터를 어느 Shard에 넣을지 결정하는 기준 컬럼 |
| 장점 | 수평 확장(Scale-out) 가능 |
| 단점 | **Cross-shard JOIN 어려움**, Shard Key 변경 어려움, 데이터 재분배 복잡 |

#### Shard Key 전략

| 전략 | 방식 | 특징 |
|------|------|------|
| **Range** | 값 범위로 분배 (1~1000, 1001~2000) | 단순하지만 핫스팟 가능 |
| **Hash** | 해시값으로 분배 (hash(id) % N) | 균등 분배, 범위 검색 어려움 |
| **Directory** | 매핑 테이블로 관리 | 유연하지만 매핑 테이블이 병목 |

### 파티셔닝 (Partitioning) — 단일 서버 내 분할

**하나의 DB 서버 내에서** 하나의 큰 테이블을 여러 파티션으로 분할한다.

```
orders 테이블 (하나의 DB 서버 내부):
  ├── partition_2024  (2024년 주문)
  ├── partition_2025  (2025년 주문)
  └── partition_2026  (2026년 주문)

SELECT * FROM orders WHERE created_at = '2026-03-01'
→ partition_2026만 스캔 (Partition Pruning)
```

| 종류 | 방식 |
|------|------|
| **Range** | 날짜, 숫자 범위로 분할 (가장 많이 사용) |
| **List** | 특정 값 목록으로 분할 (지역, 카테고리) |
| **Hash** | 해시값으로 균등 분할 |

### 세 가지 비교

| | Replication | 샤딩 | 파티셔닝 |
|---|---|---|---|
| 목적 | 읽기 분산 + 가용성 | **쓰기 분산** + 용량 확장 | 쿼리 성능 + 관리 편의 |
| 데이터 | **전체 복사** | **나눠서 저장** | **나눠서 저장** |
| 서버 수 | 여러 대 | **여러 대** | **한 대** |
| 복잡도 | 낮음 | 높음 | 중간 |
| 쓰기 성능 | 향상 안 됨 | **향상** | 향상 안 됨 |

### 조합 사용

실무에서는 이 세 가지를 조합해서 사용한다:

```
[Shard 1]                    [Shard 2]
├── Master ──→ Replica 1     ├── Master ──→ Replica 1
│             Replica 2      │             Replica 2
│                            │
├── partition_2025           ├── partition_2025
└── partition_2026           └── partition_2026

샤딩 + Replication + 파티셔닝 조합
```

## 관련 문서

- [[RDBMS-vs-NoSQL]] — 확장 전략과 DB 선택
- [[Connection-Pool]] — 여러 서버 연결 관리
- [[트랜잭션]] — 분산 트랜잭션의 어려움
- [[Redis 클러스터]] — NoSQL의 클러스터링
