---
id: 20260517-collections-framework
title: "Collections Framework"
tags: [java, collections, data-structures]
status: completed
created: 2026-04-02
updated: 2026-05-18
---

# Collections Framework

## 핵심 개념

**Java Collections Framework**는 데이터를 저장하고 조작하기 위한 통합 아키텍처다. `Collection`과 `Map` 두 개의 최상위 인터페이스를 중심으로 List, Set, Queue/Deque, Map 계열로 나뉜다.

## 인터페이스 계층 구조

```
Iterable
└── Collection
    ├── List      → ArrayList, LinkedList
    ├── Set       → HashSet, LinkedHashSet, TreeSet
    └── Queue
        └── Deque → ArrayDeque, LinkedList

Map (별도 계층)   → HashMap, LinkedHashMap, TreeMap
```

`Map`은 `Collection`과 **별도 계층**이다. Key-Value 쌍을 다루기 때문에 단일 요소 모델과 맞지 않아 독립적으로 설계되었다.

## 구현체 시간 복잡도 요약

| 구현체 | get(i) | add(끝) | add(중간) | remove | contains |
|---|---|---|---|---|---|
| **ArrayList** | O(1) | O(1) amortized | O(N) | O(N) | O(N) |
| **LinkedList** | O(N) | O(1) | O(N)* | O(1)† | O(N) |
| **HashSet** | — | O(1) | — | O(1) | O(1) |
| **TreeSet** | — | O(log N) | — | O(log N) | O(log N) |
| **LinkedHashSet** | — | O(1) | — | O(1) | O(1) |
| **HashMap** | O(1) | O(1) | — | O(1) | — |
| **TreeMap** | O(log N) | O(log N) | — | O(log N) | — |
| **ArrayDeque** | — | O(1) | — | O(1) | O(N) |

\* 중간 삽입: 위치 탐색 O(N) + 포인터 연결 O(1) = 실질적 O(N)
† 노드 참조를 이미 갖고 있을 때만 O(1); 인덱스 기반이면 O(N)

## ArrayList vs LinkedList

| | ArrayList | LinkedList |
|---|---|---|
| 내부 구조 | **동적 배열** | **이중 연결 리스트** |
| 인덱스 조회 | **O(1)** | O(N) — 헤드/테일에서 순차 탐색 |
| 끝 삽입/삭제 | O(1) amortized | O(1) |
| 중간 삽입/삭제 | O(N) | O(N) — 탐색 비용 포함 |
| 캐시 성능 | **높음** — 연속 메모리, 프리페치 효과적 | 낮음 — 노드가 힙에 흩어져 캐시 미스 빈번 |
| 노드 오버헤드 | 없음 | 노드당 prev/next 포인터 2개 + 객체 헤더 |

### ArrayList 동적 크기 조정

내부 배열이 가득 차면 **기존 용량의 1.5배** 크기의 새 배열을 생성하고 기존 요소를 복사한다.

```java
// OpenJDK ArrayList 내부 (개념적 표현, Java 17)
private Object[] grow(int minCapacity) {
    int oldCapacity = elementData.length;
    int newCapacity = oldCapacity + (oldCapacity >> 1); // × 1.5
    return elementData = Arrays.copyOf(elementData, newCapacity);
}
```

대량 삽입이 예상될 때는 `new ArrayList<>(initialCapacity)`로 초기 용량을 지정해 불필요한 배열 복사를 줄일 수 있다.

### LinkedList가 실제로 느린 이유

중간 삽입이 이론상 O(1)이어도 **삽입 위치 탐색 자체가 O(N)**이다. 게다가 각 노드가 힙의 임의 위치에 할당되어 CPU L1/L2 캐시 미스가 매 노드 접근마다 발생한다. 연속 메모리를 쓰는 ArrayList는 프리페치가 효과적으로 동작해 실제 벤치마크에서 대부분 ArrayList가 LinkedList를 능가한다.

**실무에서 LinkedList가 유리한 경우는 거의 없다.** Deque 용도라면 ArrayDeque를, List 용도라면 ArrayList가 기본이다.

## Set 구현체 비교

| | HashSet | LinkedHashSet | TreeSet |
|---|---|---|---|
| 내부 구조 | **HashMap** | LinkedHashMap | **Red-Black Tree** |
| 순서 | 없음 | **삽입 순서 유지** | **자연 정렬 또는 Comparator** |
| 조회/삽입/삭제 | O(1) | O(1) | O(log N) |
| null 허용 | 1개 허용 | 1개 허용 | 불허 (비교 불가) |

### HashSet 내부 구조 — HashMap + dummy value

```java
// HashSet 내부 (실제 OpenJDK 구조)
public class HashSet<E> {
    private transient HashMap<E, Object> map;
    private static final Object PRESENT = new Object(); // dummy value

    public boolean add(E e) {
        return map.put(e, PRESENT) == null;
    }
    public boolean contains(Object o) {
        return map.containsKey(o);
    }
}
```

HashSet의 O(1) contains는 [[HashMap-HashTable-ConcurrentHashMap]]의 버킷 탐색 덕분이다.
HashSet/HashMap의 정확성은 [[equals와-hashCode]] 구현에 완전히 의존한다 — 미구현 시 중복 제거가 작동하지 않는다.

## Map 구현체 비교

| | HashMap | LinkedHashMap | TreeMap |
|---|---|---|---|
| 내부 구조 | 배열 + LinkedList/Red-Black Tree | HashMap + 이중 연결 리스트 | Red-Black Tree |
| 순서 | 없음 | **삽입 순서 유지** | **Key 자연 정렬** |
| 조회/삽입/삭제 | O(1) | O(1) | O(log N) |
| null Key | 1개 허용 | 1개 허용 | 불허 |

## fail-fast Iterator

대부분의 Java 컬렉션 iterator는 **fail-fast** 방식이다. iterator 생성 시점의 `modCount`를 기억했다가 `next()` 호출마다 비교하여, 구조 변경이 감지되면 즉시 `ConcurrentModificationException`을 던진다.

```java
List<String> list = new ArrayList<>(List.of("A", "B", "C"));

// 잘못된 코드 — 반복 중 직접 remove → ConcurrentModificationException
for (String s : list) {
    if (s.equals("B")) list.remove(s);
}

// 올바른 방법 1: Iterator.remove()
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    if (it.next().equals("B")) it.remove();
}

// 올바른 방법 2: removeIf (Java 8+, 권장)
list.removeIf(s -> s.equals("B"));
```

fail-fast는 멀티스레드 안전장치가 아니다. 단일 스레드에서의 논리 오류를 빠르게 발견하기 위한 메커니즘이다.

## Thread-safe 컬렉션 선택

| 방법 | 내부 동작 | 적합한 상황 | 주의사항 |
|---|---|---|---|
| `Collections.synchronizedList()` | 모든 메서드에 synchronized | 간단한 동기화 래핑 | iteration 시 외부 동기화 필수 |
| `CopyOnWriteArrayList` | 쓰기 시 배열 전체 복사 | 읽기 많고 쓰기 드물 때 | 쓰기 비용 O(N), 메모리 증가 |
| `ConcurrentHashMap` | 버킷 단위 락 + CAS | 멀티스레드 Map 표준 | null key/value 불허 |

```java
// synchronizedList — iteration 시 외부 lock 필수
List<String> syncList = Collections.synchronizedList(new ArrayList<>());
synchronized (syncList) {
    for (String s : syncList) { process(s); }
}

// CopyOnWriteArrayList — 스냅샷 기반 iteration, 동기화 불필요
List<String> cowList = new CopyOnWriteArrayList<>();
for (String s : cowList) { process(s); } // safe
```

## 구현체 선택 가이드

```
데이터 저장 목적
│
├── Key-Value? → Map
│   ├── 정렬 필요?          → TreeMap
│   ├── 삽입 순서 유지?      → LinkedHashMap
│   ├── 멀티스레드?          → ConcurrentHashMap
│   └── 기본               → HashMap
│
├── 중복 불허? → Set
│   ├── 정렬 필요?          → TreeSet
│   ├── 삽입 순서 유지?      → LinkedHashSet
│   └── 기본               → HashSet
│
└── 순서 있는 목록? → List / Queue
    ├── 인덱스 접근?         → ArrayList
    ├── Queue / Stack?      → ArrayDeque
    └── 멀티스레드 읽기 많음? → CopyOnWriteArrayList
```

## 관련 문서

- [[HashMap-HashTable-ConcurrentHashMap]] — HashMap 버킷 구조, ConcurrentHashMap 상세
- [[equals와-hashCode]] — HashSet/HashMap 정확성의 전제 조건
- [[JVM]] — ArrayList capacity growth 시 배열 복사와 Heap GC 영향
- [[캐시의-지역성-원리]] — LinkedList 캐시 미스의 하드웨어 원리
