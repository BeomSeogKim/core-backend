---
tags: [java, collections]
status: completed
created: 2026-04-02
---

# Collections Framework

## 핵심 개념

**Java Collections Framework**는 데이터를 저장하고 조작하기 위한 통합 아키텍처다. `Collection`과 `Map` 두 개의 최상위 인터페이스를 중심으로 List, Set, Queue, Map 계열로 나뉜다.

## 동작 원리

### 인터페이스 계층 구조

```
Iterable
└── Collection
    ├── List      ← 순서 O, 중복 O
    ├── Set       ← 순서 X, 중복 X
    └── Queue     ← FIFO

Map               ← Collection과 별도 계층, Key-Value
```

### 인터페이스별 특징과 구현체

| 인터페이스 | 특징 | 대표 구현체 |
|---|---|---|
| **List** | 순서 보장, 중복 허용, 인덱스 접근 | ArrayList, LinkedList |
| **Set** | 순서 미보장, 중복 불허 | HashSet, TreeSet, LinkedHashSet |
| **Queue** | FIFO (선입선출) | LinkedList, PriorityQueue, ArrayDeque |
| **Map** | Key-Value 쌍, Key 중복 불허 | HashMap, TreeMap, LinkedHashMap |

### ArrayList vs LinkedList

| | ArrayList | LinkedList |
|---|---|---|
| 내부 구조 | **배열** (동적 크기 조정) | **이중 연결 리스트** |
| 인덱스 조회 | **O(1)** — Random Access | O(N) — 순차 탐색 |
| 처음 삽입/삭제 | O(N) — 요소 이동 필요 | **O(1)** |
| 끝 삽입/삭제 | **O(1)** (amortized) | **O(1)** |
| 중간 삽입/삭제 | O(N) | O(N) — 탐색 O(N) + 삽입 O(1) |
| 캐시 성능 | **높음** (연속 메모리 → 공간적 지역성) | 낮음 (노드 흩어짐 → 캐시 미스) |

> [!note] 실제 성능
> 이론적 시간복잡도는 중간 삽입에서 LinkedList가 유리해 보이지만, 실제로는 ArrayList가 대부분의 경우 빠르다. 배열의 연속 메모리 배치가 CPU **캐시의 공간적 지역성**을 활용하기 때문이다. [[캐시의-지역성-원리]] 참고.

#### ArrayList 동적 크기 조정

ArrayList는 내부 배열이 가득 차면 **기존 용량의 1.5배** 크기의 새 배열을 생성하고 기존 요소를 복사한다.

```java
// 내부 동작 (개념적)
if (size == capacity) {
    int newCapacity = capacity + (capacity >> 1);  // 1.5배
    elementData = Arrays.copyOf(elementData, newCapacity);
}
```

### Set 구현체 비교

| | HashSet | LinkedHashSet | TreeSet |
|---|---|---|---|
| 내부 구조 | **HashMap** | LinkedHashMap | **Red-Black Tree** |
| 순서 | 없음 | **삽입 순서 유지** | **정렬 순서 유지** |
| 조회/삽입/삭제 | O(1) | O(1) | **O(log N)** |
| null 허용 | 1개 허용 | 1개 허용 | 불허 (비교 불가) |

HashSet은 내부적으로 HashMap을 사용하며, 값을 Key로, 더미 객체를 Value로 저장한다.

### Map 구현체 비교

| | HashMap | LinkedHashMap | TreeMap |
|---|---|---|---|
| 내부 구조 | 배열 + LinkedList/Red-Black Tree | HashMap + 이중 연결 리스트 | **Red-Black Tree** |
| 순서 | 없음 | **삽입 순서 유지** | **Key 정렬 순서** |
| 조회/삽입/삭제 | O(1) | O(1) | **O(log N)** |
| null Key | 1개 허용 | 1개 허용 | 불허 |

## 관련 문서

- [[HashMap-HashTable-ConcurrentHashMap]]
- [[dev/06-computer-science/os/캐시의-지역성-원리|캐시의-지역성-원리]]
- [[JVM]] — 객체가 저장되는 Heap 영역
