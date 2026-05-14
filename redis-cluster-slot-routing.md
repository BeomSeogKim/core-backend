---
id: 20260430-1000-redis-cluster-slot-routing
title: "Redis Cluster 슬롯 라우팅"
type: atomic
status: seedling
domain: career
subdomain: dev
tags: [redis, database, distributed-system, cs]
created: 2026-04-30
updated: 2026-04-30
---

# Redis Cluster 슬롯 라우팅

Redis Cluster는 데이터를 **16384개의 해시 슬롯(Hash Slot)**으로 분산 저장한다. 클라이언트가 키에 접근할 때, 어느 노드로 라우팅할지를 슬롯 번호로 결정한다.

## 핵심 개념

### 슬롯 배분 원리

```
슬롯 번호 = CRC16(key) % 16384
```

16384개 슬롯이 마스터 노드들에 균등하게 배분된다. 예: 노드 3개면 각각 ~5461개 슬롯.

```
Node A: slot 0     ~ 5460
Node B: slot 5461  ~ 10922
Node C: slot 10923 ~ 16383
```

클라이언트가 `CRC16("user:1") % 16384 = 7000`을 계산하면 → Node B로 라우팅.

### 해시 태그 (`{}`)

`{}`를 쓰면 중괄호 안의 값만으로 슬롯을 계산한다.

```
{EBAY}_US → hash("EBAY") → 같은 슬롯
{EBAY}_JP → hash("EBAY") → 같은 슬롯
```

**목적:** 여러 키를 같은 슬롯에 강제 배치 → Multi-key 연산(MGET, 트랜잭션) 가능  
**위험:** 특정 슬롯에 트래픽 집중 → **핫스팟(Hotspot)** 발생 가능

### Replica Failover

1. Master 노드 장애 발생
2. 클러스터 내 과반 Master가 해당 노드를 FAIL로 판정
3. 해당 Master의 Replica 중 하나가 Master로 자동 승격
4. 슬롯 소유권 재할당 및 클러스터 전파

### 키의 슬롯 확인

```bash
CLUSTER KEYSLOT user:1        # → 해당 키의 슬롯 번호 반환
CLUSTER INFO                  # → 클러스터 상태 전체
```

## 면접 포인트

- "왜 16384인가?" → 2^14 = 16384. gossip 메시지 크기를 2KB 이하로 유지하기 위한 트레이드오프.
- "해시 태그의 위험성" → 핫스팟 + 특정 슬롯 과부하 → 클러스터 이점 상실.
- "MOVED 리다이렉션" → 클라이언트가 잘못된 노드로 요청 시 `MOVED <slot> <ip>:<port>` 응답 → 클라이언트가 재요청.

## 관련 문서

- [[system-design-scale-estimation-metrics]] — Redis 클러스터는 scale-out 설계의 핵심 구성요소
- [[30-Career/mentoring-2026-03-21]] — 토스플레이스 면접 회고 맥락, 취약 개념으로 확인
