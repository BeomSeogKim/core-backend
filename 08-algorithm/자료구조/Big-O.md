---
tags: [algorithm, big-o, time-complexity]
status: completed
created: 2026-04-04
---

# Big O 표기법

## 핵심 개념

**Big O 표기법**은 알고리즘의 **최악의 경우(Worst Case)** 시간/공간 복잡도를 나타내는 표기법이다. 입력 크기 N이 커질 때 실행 시간이 어떻게 증가하는지를 표현한다. 상수와 낮은 차수 항은 무시한다.

## 동작 원리

### 주요 시간복잡도 (빠른 순)

```
O(1) < O(log N) < O(N) < O(N log N) < O(N²) < O(2ᴺ) < O(N!)
```

<div style="font-family: -apple-system, sans-serif; max-width: 520px; margin: 12px auto; padding: 16px; font-size: 12px;">
<div style="display: flex; flex-direction: column; gap: 3px;">
<div style="display: flex; align-items: center; gap: 8px;">
<div style="background: #27AE60; color: white; padding: 6px 12px; border-radius: 4px; width: 120px; text-align: center; font-weight: bold;">O(1)</div>
<div style="color: #333;">상수 — HashMap 조회, 배열 인덱스 접근</div>
</div>
<div style="display: flex; align-items: center; gap: 8px;">
<div style="background: #2ECC71; color: white; padding: 6px 12px; border-radius: 4px; width: 120px; text-align: center; font-weight: bold;">O(log N)</div>
<div style="color: #333;">로그 — 이진 탐색, B+Tree 탐색</div>
</div>
<div style="display: flex; align-items: center; gap: 8px;">
<div style="background: #F39C12; color: white; padding: 6px 12px; border-radius: 4px; width: 120px; text-align: center; font-weight: bold;">O(N)</div>
<div style="color: #333;">선형 — 배열 순회, 선형 탐색</div>
</div>
<div style="display: flex; align-items: center; gap: 8px;">
<div style="background: #E67E22; color: white; padding: 6px 12px; border-radius: 4px; width: 120px; text-align: center; font-weight: bold;">O(N log N)</div>
<div style="color: #333;">선형 로그 — Merge Sort, Quick Sort(평균)</div>
</div>
<div style="display: flex; align-items: center; gap: 8px;">
<div style="background: #E74C3C; color: white; padding: 6px 12px; border-radius: 4px; width: 120px; text-align: center; font-weight: bold;">O(N²)</div>
<div style="color: #333;">이차 — Bubble Sort, 이중 for문</div>
</div>
<div style="display: flex; align-items: center; gap: 8px;">
<div style="background: #8E44AD; color: white; padding: 6px 12px; border-radius: 4px; width: 120px; text-align: center; font-weight: bold;">O(2ᴺ)</div>
<div style="color: #333;">지수 — 피보나치(재귀), 부분집합</div>
</div>
<div style="display: flex; align-items: center; gap: 8px;">
<div style="background: #2C3E50; color: white; padding: 6px 12px; border-radius: 4px; width: 120px; text-align: center; font-weight: bold;">O(N!)</div>
<div style="color: #333;">팩토리얼 — 순열, TSP(브루트포스)</div>
</div>
</div>
</div>

### N에 따른 연산 횟수 비교

| N | O(1) | O(log N) | O(N) | O(N log N) | O(N²) |
|---|---|---|---|---|---|
| 10 | 1 | 3 | 10 | 30 | 100 |
| 100 | 1 | 7 | 100 | 700 | 10,000 |
| 1,000 | 1 | 10 | 1,000 | 10,000 | 1,000,000 |
| 10,000 | 1 | 13 | 10,000 | 130,000 | 100,000,000 |

### 자주 쓰는 자료구조별 시간복잡도

| 자료구조 | 조회 | 삽입 | 삭제 | 탐색 |
|----------|------|------|------|------|
| **Array** | O(1) | O(N) | O(N) | O(N) |
| **LinkedList** | O(N) | O(1)* | O(1)* | O(N) |
| **Hash Table** | O(1) | O(1) | O(1) | — |
| **BST (균형)** | O(log N) | O(log N) | O(log N) | O(log N) |
| **Stack/Queue** | — | O(1) | O(1) | — |

*노드 위치를 알고 있는 경우

### Big O 표기 규칙

1. **상수 제거** — O(2N) → O(N)
2. **낮은 차수 무시** — O(N² + N) → O(N²)
3. **최고 차수만** — O(3N³ + 2N² + N + 5) → O(N³)

```java
// O(1) — 입력 크기와 무관
int first = arr[0];

// O(N) — 단일 루프
for (int i = 0; i < n; i++) { ... }

// O(N²) — 이중 루프
for (int i = 0; i < n; i++) {
    for (int j = 0; j < n; j++) { ... }
}

// O(log N) — 반씩 줄어드는 루프
while (n > 1) { n = n / 2; }
```

### Big O vs Big Omega vs Big Theta

| 표기 | 의미 | 실무 사용 |
|------|------|----------|
| **Big O (O)** | **최악의 경우** 상한 | 가장 많이 사용 |
| Big Omega (Ω) | 최선의 경우 하한 | 거의 사용 안 함 |
| Big Theta (Θ) | 평균적인 경우 | 학술적 사용 |

## 관련 문서

- [[자료구조]]
- [[Array와 LinkedList]]
- [[해시테이블]]
- [[정렬]]
