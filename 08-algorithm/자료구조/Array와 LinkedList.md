---
tags: [algorithm, list]
status: completed
created: 2025-12-09
---
# List

**순서를 가진 원소들을 저장**하고 **동적으로 크기가 조절**되는 특징을 지닌 [[자료구조]]

구현 방식으로는 **ArrayList**, **LinkedList**가 있다.

## 핵심 개념

> [!note] 핵심 요약
> List는 순서가 있는 데이터를 저장하는 선형 자료구조로, 정적 배열의 한계를 보완한 동적 배열과 포인터 기반의 연결 리스트로 구현된다.

### 정적 배열 (Static Array)
고정된 크기를 가지는 배열로, 생성 시 크기가 정해지며 이후 변경할 수 없다. 메모리 관리가 효율적이지만, 크기를 미리 예측해야 하는 단점이 존재한다.

**배열의 특성**

고정된 저장 공간
- 배열 생성 시 크기를 명시적으로 지정하며, 이후 크기 변경이 불가능하다.

순차적인 데이터 저장
- 크기가 고정되어있어, JVM은 배열이 사용할 메모리를 런타임 시점에 결정한다.
- 추가적은 메모리 할당이나 해제가 필요 없어 메모리 관리가 단순하다.

메모리 연속적 할당

`int[] arr = {1, 2, 3, 4, 5}`
- 위 예시의 경우 int형 데이터(4 byte) 크기를 5로 정했기 때문에 20byte를 미리 할당 받는다.
- 또한 배열의 데이터는 연속적이고 순차적으로 메모리에 저장이 된다.

*Random Access*

메모리에 저장된 데이터를 접근하기 위해서는 주소값을 알아야 한다. 배열 변수는 할당받은 메모리의 첫 번째 주소값을 가리킨다.

데이터가 연속적으로 저장되어 있기 때문에, 첫 번째의 주소값 그리고 인덱스만 알면 간단한 연산 (시작 주소값 + (인덱스 * type 크기))만으로 원하는 데이터에 O(1)으로 접근 할 수 있다. 이를 **Random Access**라고 한다.

<div style="background:#1e1e2e; border-radius:8px; padding:16px; font-family:monospace; color:#cdd6f4; margin:12px 0;">
<div style="color:#89b4fa; font-weight:bold; margin-bottom:8px;">Random Access 계산 과정</div>
<div style="display:flex; gap:2px; margin-bottom:8px;">
<div style="background:#45475a; padding:8px 12px; border-radius:4px; text-align:center; border:2px solid #f38ba8;">
<div style="color:#f38ba8; font-size:10px;">0x100</div>
<div style="color:#a6e3a1; font-weight:bold;">10</div>
<div style="color:#6c7086; font-size:10px;">arr[0]</div>
</div>
<div style="background:#45475a; padding:8px 12px; border-radius:4px; text-align:center;">
<div style="color:#f38ba8; font-size:10px;">0x104</div>
<div style="color:#a6e3a1; font-weight:bold;">20</div>
<div style="color:#6c7086; font-size:10px;">arr[1]</div>
</div>
<div style="background:#45475a; padding:8px 12px; border-radius:4px; text-align:center;">
<div style="color:#f38ba8; font-size:10px;">0x108</div>
<div style="color:#a6e3a1; font-weight:bold;">30</div>
<div style="color:#6c7086; font-size:10px;">arr[2]</div>
</div>
<div style="background:#45475a; padding:8px 12px; border-radius:4px; text-align:center; border:2px solid #89b4fa;">
<div style="color:#f38ba8; font-size:10px;">0x10C</div>
<div style="color:#a6e3a1; font-weight:bold;">40</div>
<div style="color:#6c7086; font-size:10px;">arr[3]</div>
</div>
<div style="background:#45475a; padding:8px 12px; border-radius:4px; text-align:center;">
<div style="color:#f38ba8; font-size:10px;">0x110</div>
<div style="color:#a6e3a1; font-weight:bold;">50</div>
<div style="color:#6c7086; font-size:10px;">arr[4]</div>
</div>
</div>
<div style="color:#f9e2af; font-size:12px;">arr[3] = 0x100 + (4 × 3) = 0x10C → <span style="color:#a6e3a1;">O(1)</span></div>
</div>

## 동작 원리

### 동적 배열 (Dynamic Array)
크기를 실행 중에 동적으로 변경할 수 있는 배열로, 정적 배열의 한계를 보완하기 위해 설계되었다. Java에서는 `ArrayList`가 대표적인 동적 배열 구현체

> [!tip] Amortized O(1)
> 배열에 데이터를 추가하다 기존 할당된 내부 배열의 크기를 초과하면, 더 큰 크기의 배열을 새로 생성하고 기존 데이터를 모두 복사하는 과정을 가진다 O(n).
>
> 하지만 대부분의 추가 연산은 O(1) 시간 복잡도를 가진다. Worst 시간복잡도를 대부분의 연산에서 분할 상환을 하기 때문에 결과적으로 **Amortized O(1)**이라는 시간 복잡도를 지닌다.

**Growth Factor (확장 비율)**

배열이 가득 찼을 때 **배수(multiplicative)**로 크기를 늘린다. 고정값(예: +10)으로 늘리면 삽입이 빈번할수록 재할당 빈도가 높아져 amortized O(1)이 깨진다.

<div style="background:#1e1e2e; border-radius:8px; padding:16px; font-family:monospace; color:#cdd6f4; margin:12px 0;">
<div style="color:#89b4fa; font-weight:bold; margin-bottom:8px;">Dynamic Array 리사이징 (×1.5)</div>
<div style="margin-bottom:6px;">
<span style="color:#6c7086;">Step 1:</span>
<span style="background:#45475a; padding:2px 8px; border-radius:3px; color:#a6e3a1;">1</span>
<span style="background:#45475a; padding:2px 8px; border-radius:3px; color:#a6e3a1;">2</span>
<span style="background:#45475a; padding:2px 8px; border-radius:3px; color:#a6e3a1;">3</span>
<span style="background:#45475a; padding:2px 8px; border-radius:3px; color:#a6e3a1;">4</span>
<span style="color:#f38ba8;"> ← FULL</span>
</div>
<div style="margin-bottom:6px;">
<span style="color:#6c7086;">Step 2:</span>
<span style="background:#45475a; padding:2px 8px; border-radius:3px; color:#a6e3a1;">1</span>
<span style="background:#45475a; padding:2px 8px; border-radius:3px; color:#a6e3a1;">2</span>
<span style="background:#45475a; padding:2px 8px; border-radius:3px; color:#a6e3a1;">3</span>
<span style="background:#45475a; padding:2px 8px; border-radius:3px; color:#a6e3a1;">4</span>
<span style="background:#45475a; padding:2px 8px; border-radius:3px; color:#f9e2af;">5</span>
<span style="background:#313244; padding:2px 8px; border-radius:3px; color:#6c7086;">_</span>
<span style="color:#a6e3a1;"> ← 새 배열 (size 6)</span>
</div>
<div style="color:#f9e2af; font-size:11px; margin-top:4px;">기존 4개 복사 O(n) + 삽입 O(1) → 대부분은 O(1)이므로 Amortized O(1)</div>
</div>

| 구현체 | Growth Factor |
|--------|--------------|
| Java `ArrayList` | **1.5배** (`oldCapacity + (oldCapacity >> 1)`) |
| C++ `std::vector` | 보통 **2배** |
| Python `list` | 약 **1.125배** |

### 연결 리스트 (Linked List)
여러 개의 독립된 노드가 포인터를 통해 연결되어, 논리적인 순서를 형성하는 자료구조

배열과 달리 Linked List의 노드들은 메모리 곳곳에 흩어져 있을 수 있다. 이러한 특성 때문에 메모리 효율적으로 동작할 수 있으며,데이터 삽입 및 삭제에 뛰어난 성능을 발휘한다.

> [!warning] 물리적 비연속적, 논리적 연속적
> LinkedList는 메모리상에서 비연속적으로 저장되지만, 각각의 노드가 다음 메모리 주소를 가리키면서 논리적으로 연속성을 지닌다.

장단점
- 장점
    - 빠른 삽입/삭제 : 특정 위치의 노드만 알고 있다면, 해당 노드의 앞 / 뒤 포인터만 수정하면 되므로 삽입 / 삭제가 빠르다 O(1)
- 단점
    - 느린 조회 : 특정 인덱스에 조회하기 위해서는 `head`부터 차례대로 따라가야 하므로 O(n)의 시간복잡도를 지닌다.

Linked List의 종류

<div style="background:#1e1e2e; border-radius:8px; padding:16px; font-family:monospace; color:#cdd6f4; margin:12px 0;">
<div style="color:#89b4fa; font-weight:bold; margin-bottom:12px;">Singly Linked List</div>
<div style="display:flex; align-items:center; gap:4px; margin-bottom:16px;">
<div style="background:#45475a; padding:6px 10px; border-radius:4px; text-align:center;">
<span style="color:#a6e3a1;">A</span> <span style="color:#6c7086;">|</span> <span style="color:#f38ba8;">→</span>
</div>
<span style="color:#f38ba8;">→</span>
<div style="background:#45475a; padding:6px 10px; border-radius:4px; text-align:center;">
<span style="color:#a6e3a1;">B</span> <span style="color:#6c7086;">|</span> <span style="color:#f38ba8;">→</span>
</div>
<span style="color:#f38ba8;">→</span>
<div style="background:#45475a; padding:6px 10px; border-radius:4px; text-align:center;">
<span style="color:#a6e3a1;">C</span> <span style="color:#6c7086;">|</span> <span style="color:#6c7086;">null</span>
</div>
</div>
<div style="color:#89b4fa; font-weight:bold; margin-bottom:12px;">Doubly Linked List</div>
<div style="display:flex; align-items:center; gap:4px; margin-bottom:16px;">
<span style="color:#6c7086;">null</span>
<div style="background:#45475a; padding:6px 10px; border-radius:4px; text-align:center;">
<span style="color:#f38ba8;">←</span> <span style="color:#6c7086;">|</span> <span style="color:#a6e3a1;">A</span> <span style="color:#6c7086;">|</span> <span style="color:#f38ba8;">→</span>
</div>
<span style="color:#f9e2af;">⇄</span>
<div style="background:#45475a; padding:6px 10px; border-radius:4px; text-align:center;">
<span style="color:#f38ba8;">←</span> <span style="color:#6c7086;">|</span> <span style="color:#a6e3a1;">B</span> <span style="color:#6c7086;">|</span> <span style="color:#f38ba8;">→</span>
</div>
<span style="color:#f9e2af;">⇄</span>
<div style="background:#45475a; padding:6px 10px; border-radius:4px; text-align:center;">
<span style="color:#f38ba8;">←</span> <span style="color:#6c7086;">|</span> <span style="color:#a6e3a1;">C</span> <span style="color:#6c7086;">|</span> <span style="color:#f38ba8;">→</span>
</div>
<span style="color:#6c7086;">null</span>
</div>
<div style="color:#89b4fa; font-weight:bold; margin-bottom:12px;">Circular Linked List</div>
<div style="display:flex; align-items:center; gap:4px;">
<span style="color:#f9e2af;">↻</span>
<div style="background:#45475a; padding:6px 10px; border-radius:4px; text-align:center;">
<span style="color:#a6e3a1;">A</span> <span style="color:#6c7086;">|</span> <span style="color:#f38ba8;">→</span>
</div>
<span style="color:#f38ba8;">→</span>
<div style="background:#45475a; padding:6px 10px; border-radius:4px; text-align:center;">
<span style="color:#a6e3a1;">B</span> <span style="color:#6c7086;">|</span> <span style="color:#f38ba8;">→</span>
</div>
<span style="color:#f38ba8;">→</span>
<div style="background:#45475a; padding:6px 10px; border-radius:4px; text-align:center;">
<span style="color:#a6e3a1;">C</span> <span style="color:#6c7086;">|</span> <span style="color:#f38ba8;">→ A</span>
</div>
</div>
</div>

1. **Singly Linked List**
    - 각 노드는 다음 노드에 대한 pointer만 지닌다.
    - 단방향으로만 이동이 가능하며, 구조가 단순하다.
2. **Doubly Linked List**
    - 각 노드는 이전, 다음 노드에 대한 pointer를 지닌다.
    - 양방향으로 이동이 가능해 탐색 및 삽입 / 삭제가 유연하다.
3. **Circular Linked List**
    - 마지막 노드가 첫번째 노드를 가리키는 구조로, 순환형 형태를 지닌다.

### Deque vs List
Linked List는 Deque, List 인터페이스를 모두 구현하고 있다.

List
- 일반적인 리스트 기능 (`get`, `set`, `add`)을 사용할 때 선언
- get(index)의 성능이 O(n) 으로 좋지 않은 편이라 이 경우 -> `ArrayList`를 사용하는 것이 낫다.

Deque
- 양쪽 끝에서의 빠른 데이터 처리를 위한 메서드들을 제공한다. O(1)
- [[큐]] 혹은 [[스택]]이 필요할 때 좋은 선택지가 될 수 있다.

## 코드 예시

```java
// ArrayList - 동적 배열
List<Integer> arrayList = new ArrayList<>();
arrayList.add(1);        // O(1) amortized
arrayList.get(0);        // O(1) random access

// LinkedList - 이중 연결 리스트
List<Integer> linkedList = new LinkedList<>();
linkedList.addFirst(1);  // O(1)
linkedList.get(0);       // O(n) sequential access
```

## ArrayList vs LinkedList

> [!note] 비교 요약
> 두 클래스는 모두 데이터를 순차적으로 관리하는 `List` 인터페이스를 구현하고 있지만, 내부 구조의 특성 차이로 인한 성능 특성이 다르다.

| 구분 | ArrayList | Linked List |
| -- | --- | --- |
| 데이터 조회 | O(1) | O(n) |
| 맨 앞/끝 추가/삭제 | O(n)/O(1) | O(1) |
| 중간 추가/삭제 | O(n) | O(n) |
| 메모리 사용| 효율적 | 비효율적 |

*ArrayList는 데이터만 저장하지만, LinkedList는 데이터와 포인터를 위한 추가 공간이 필요하기 때문에 비효율적이다.*

> [!warning] Linked List 중간 삽입/삭제의 함정
> Linked List의 삽입/삭제 자체는 O(1)이지만, 해당 위치까지 **탐색하는 과정이 O(n)**이므로 탐색을 포함하면 결국 O(n)이다. O(1)이 유의미한 경우는 **이미 위치를 알고 있을 때**(Iterator 순회 중 삽입/삭제) 또는 **맨 앞/뒤에 삽입/삭제**하는 경우뿐이다.

### CPU 캐시 친화성 (Cache Locality)

실무에서 대부분 ArrayList를 사용하는 이유 중 하나.

CPU가 메모리를 읽을 때 요청한 주소 하나만 읽는 것이 아니라 **주변 메모리 블록(Cache Line, 보통 64 bytes)**을 통째로 캐시에 올린다.

<div style="background:#1e1e2e; border-radius:8px; padding:16px; font-family:monospace; color:#cdd6f4; margin:12px 0;">
<div style="color:#a6e3a1; font-weight:bold; margin-bottom:8px;">Array — Cache Hit (연속 메모리)</div>
<div style="background:#313244; border-radius:4px; padding:8px; margin-bottom:4px; border:1px dashed #a6e3a1;">
<span style="color:#6c7086; font-size:10px;">Cache Line (64 bytes)</span>
<div style="display:flex; gap:2px; margin-top:4px;">
<span style="background:#45475a; padding:4px 8px; border-radius:3px; color:#f9e2af; border:2px solid #f9e2af;">1</span>
<span style="background:#45475a; padding:4px 8px; border-radius:3px; color:#a6e3a1;">2</span>
<span style="background:#45475a; padding:4px 8px; border-radius:3px; color:#a6e3a1;">3</span>
<span style="background:#45475a; padding:4px 8px; border-radius:3px; color:#a6e3a1;">4</span>
<span style="background:#45475a; padding:4px 8px; border-radius:3px; color:#a6e3a1;">5</span>
<span style="background:#45475a; padding:4px 8px; border-radius:3px; color:#a6e3a1;">6</span>
<span style="background:#45475a; padding:4px 8px; border-radius:3px; color:#a6e3a1;">7</span>
<span style="background:#45475a; padding:4px 8px; border-radius:3px; color:#a6e3a1;">8</span>
</div>
</div>
<div style="color:#a6e3a1; font-size:11px; margin-bottom:16px;">↑ arr[0] 읽으면 arr[1]~arr[7]도 이미 캐시에 → 전부 Cache Hit</div>
<div style="color:#f38ba8; font-weight:bold; margin-bottom:8px;">Linked List — Cache Miss (비연속 메모리)</div>
<div style="display:flex; align-items:center; gap:8px; flex-wrap:wrap;">
<div style="background:#313244; border-radius:4px; padding:6px; border:1px dashed #f38ba8;">
<span style="color:#6c7086; font-size:9px;">0x100</span><br>
<span style="background:#45475a; padding:2px 6px; border-radius:3px; color:#f9e2af;">1|→</span>
</div>
<span style="color:#6c7086;">···</span>
<div style="background:#313244; border-radius:4px; padding:6px; border:1px dashed #f38ba8;">
<span style="color:#6c7086; font-size:9px;">0x5F0</span><br>
<span style="background:#45475a; padding:2px 6px; border-radius:3px; color:#f9e2af;">2|→</span>
</div>
<span style="color:#6c7086;">···</span>
<div style="background:#313244; border-radius:4px; padding:6px; border:1px dashed #f38ba8;">
<span style="color:#6c7086; font-size:9px;">0x2A0</span><br>
<span style="background:#45475a; padding:2px 6px; border-radius:3px; color:#f9e2af;">3|→</span>
</div>
</div>
<div style="color:#f38ba8; font-size:11px; margin-top:4px;">↑ 노드마다 다른 메모리 주소 → 매번 Cache Miss</div>
</div>

이는 **Spatial Locality(공간 지역성)** 원리 — "한 메모리를 읽으면 근처 메모리도 곧 읽을 가능성이 높다"에 의한 것으로, 이론적 시간복잡도가 같아도 실측 성능은 ArrayList가 압도적인 경우가 많다.

## 관련 문서

- [[자료구조]]
- [[스택]]
- [[큐]]
