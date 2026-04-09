---
tags: [database, rdbms, nosql]
status: completed
created: 2026-04-04
---

# RDBMS vs NoSQL

## 핵심 개념

**RDBMS**는 고정 스키마 기반의 관계형 데이터베이스로 ACID 트랜잭션을 보장하고, **NoSQL**은 유연한 스키마로 수평 확장에 유리한 비관계형 데이터베이스다. 데이터 특성과 확장 요구에 따라 선택한다.

## 동작 원리

### 비교

| | RDBMS | NoSQL |
|---|---|---|
| 스키마 | **고정** (테이블, 컬럼 사전 정의) | **유연** (스키마리스 또는 동적) |
| 데이터 모델 | 테이블 + 행 + 열 | Key-Value, Document, Column-Family, Graph |
| 관계 | **JOIN**으로 관계 표현 | 관계 약함 (비정규화/중복 허용) |
| 확장 | **수직 확장** (Scale-up) 중심 | **수평 확장** (Scale-out) 유리 |
| 트랜잭션 | **ACID** 보장 | BASE (Eventually Consistent) |
| 일관성 | **강한 일관성** | 최종 일관성 (Eventual Consistency) |
| 대표 | MySQL, PostgreSQL, Oracle | Redis, MongoDB, Cassandra, DynamoDB |

### NoSQL 유형

| 유형 | 특징 | 대표 | 사용 사례 |
|------|------|------|----------|
| **Key-Value** | 단순 키-값 쌍, 가장 빠름 | Redis, Memcached | 캐시, 세션 저장 |
| **Document** | JSON/BSON 문서 단위 저장 | MongoDB | 유연한 스키마, CMS |
| **Column-Family** | 컬럼 기반 저장, 대용량 분석 | Cassandra, HBase | 시계열, 로그 데이터 |
| **Graph** | 노드-엣지 관계 표현 | Neo4j | 소셜 네트워크, 추천 |

### RDBMS 선택 시점

- 데이터 구조가 명확하고 **관계가 복잡**한 경우
- **ACID 트랜잭션**이 필수인 경우 (금융, 결제)
- 데이터 일관성이 최우선인 경우

### NoSQL 선택 시점

- 스키마가 자주 변경되는 경우
- **대량 데이터 + 수평 확장**이 필요한 경우
- 읽기/쓰기 속도가 중요한 **캐시 용도** (Redis)
- 비정형 데이터 (로그, JSON 문서, IoT 데이터)
- 최종 일관성으로 충분한 경우

### CAP 정리

분산 시스템에서 Consistency, Availability, Partition Tolerance 세 가지를 동시에 만족할 수 없다.

```
        Consistency
          /    \
         /      \
    RDBMS       ─── Partition Tolerance
         \      /
          \    /
       Availability
```

- **CP** (Consistency + Partition Tolerance) — MongoDB, HBase
- **AP** (Availability + Partition Tolerance) — Cassandra, DynamoDB
- **CA** (Consistency + Availability) — 전통적 RDBMS (단일 노드)

## 관련 문서

- [[트랜잭션]] — ACID 속성
- [[Replication-샤딩-파티셔닝]] — DB 확장 전략
- [[Redis 클러스터]]
