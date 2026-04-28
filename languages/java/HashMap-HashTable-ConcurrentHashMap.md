---
tags: [java, collections, hashmap, concurrency]
status: completed
created: 2026-04-02
---

# HashMap vs HashTable vs ConcurrentHashMap

## 핵심 개념

세 가지 모두 Key-Value 기반의 Map 구현체지만, **동기화 방식**과 **null 허용 여부**에서 근본적으로 다르다.

## 동작 원리

### 비교 요약

| | HashMap | HashTable | ConcurrentHashMap |
|---|---|---|---|
| 동기화 | **없음** | 모든 메서드 **synchronized** | **버킷 단위 락** |
| Thread Safety | X | O (전체 락) | **O (세분화 락)** |
| null Key | 1개 허용 | **불허** | **불허** |
| null Value | 허용 | **불허** | **불허** |
| 성능 (멀티스레드) | — | 느림 (전체 락) | **빠름** (부분 락) |
| 사용 환경 | **싱글 스레드** | 레거시 (사용 지양) | **멀티 스레드** |

### HashMap 내부 구조

```
HashMap 내부 (Java 8+):

버킷 배열 (Node[])
┌───┬───┬───┬───┬───┬───┬───┬───┐
│ 0 │ 1 │ 2 │ 3 │ 4 │ 5 │ 6 │ 7 │  ← index = hash(key) & (n-1)
└─┬─┴───┴─┬─┴───┴───┴─┬─┴───┴───┘
  │       │           │
  ▼       ▼           ▼
[A:1]   [C:3]       [E:5]         ← 충돌 없으면 단일 노드
  │
  ▼
[B:2]                              ← 충돌 시 LinkedList로 체이닝
  │
  ▼
[D:4]
```

#### 해시 충돌 해결 — Separate Chaining

1. `hashCode()`로 버킷 인덱스 계산
2. 해당 버킷에 이미 노드가 있으면 **LinkedList로 연결**
3. 하나의 버킷에 노드가 **8개 이상** → **Red-Black Tree로 변환** (탐색 O(N) → O(log N))
4. **6개 이하**로 줄면 다시 LinkedList로 복원

```
버킷 내부 변화:

노드 < 8개:  [A] → [B] → [C] → [D]         (LinkedList)

노드 >= 8개:        [D]                      (Red-Black Tree)
                   /   \
                 [B]   [F]
                / \    / \
              [A] [C] [E] [G]
```

#### 리사이징 (Rehashing)

버킷 배열의 사용률이 **load factor(기본 0.75)**를 초과하면 배열 크기를 **2배**로 확장하고, 모든 엔트리의 해시를 다시 계산하여 재배치한다.

```
초기 용량 16, load factor 0.75 → 12개 초과 시 32로 확장
```

### HashTable의 문제점

```java
// HashTable — 메서드 전체에 synchronized
public synchronized V get(Object key) { ... }
public synchronized V put(K key, V value) { ... }
```

읽기 작업조차 전체 테이블에 락이 걸려서, 여러 스레드가 동시에 **읽기조차 불가능** → 심각한 병목.

### ConcurrentHashMap의 해결

```
HashTable:                    ConcurrentHashMap:
┌─────────────────────┐      ┌──────┬──────┬──────┬──────┐
│ 전체 테이블에 하나의 락 │      │ 락1  │ 락2  │ 락3  │ 락4  │
│ put/get 모두 대기     │      │버킷0-3│버킷4-7│버킷8-B│버킷C-F│
└─────────────────────┘      └──────┴──────┴──────┴──────┘
                              → 서로 다른 버킷은 동시 접근 가능
```

- **Java 8+**: 버킷(노드) 단위로 `synchronized` + CAS(Compare-And-Swap) 연산
- 읽기 작업은 **락 없이** 수행 (volatile 활용)
- 서로 다른 버킷에 대한 쓰기는 **동시 수행** 가능
- 같은 버킷에 대한 쓰기만 직렬화

## 코드 예시

```java
// 싱글 스레드 — HashMap
Map<String, Integer> map = new HashMap<>();
map.put("key", 1);
map.put(null, 100);  // null key 허용

// 멀티 스레드 — ConcurrentHashMap
Map<String, Integer> concMap = new ConcurrentHashMap<>();
concMap.put("key", 1);
// concMap.put(null, 100);  // NullPointerException!

// HashTable — 레거시, 사용 지양
Map<String, Integer> table = new Hashtable<>();  // ConcurrentHashMap 사용 권장
```

## 관련 문서

- [[Collections-Framework]]
- [[equals와-hashCode]]
- [[dev/06-computer-science/os/스핀락-뮤텍스-세마포어|스핀락-뮤텍스-세마포어]]
