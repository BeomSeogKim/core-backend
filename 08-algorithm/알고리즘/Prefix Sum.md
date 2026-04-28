---
tags: [algorithm, prefix-sum]
status: completed
created: 2026-04-01
---
# Prefix Sum (부분합)

## 핵심 개념

> [!note] 핵심 요약
> **Prefix Sum**은 배열의 누적합을 미리 계산해두어, 임의 구간의 합을 O(1)에 구하는 기법이다. 구간 합 쿼리가 반복되는 문제에서 O(N²) → O(N)으로 최적화할 수 있다.

## 동작 원리

### 기본 Prefix Sum 배열

```
원본:      [3, 1, 2, 4, 3]
Prefix:    [0, 3, 4, 6, 10, 13]
            ↑
            P[0] = 0 (빈 구간)

P[i] = A[0] + A[1] + ... + A[i-1]
```

구간 합: `A[i] + ... + A[j] = P[j+1] - P[i]`

```java
// Prefix Sum 배열 생성
int[] P = new int[A.length + 1];
for (int i = 0; i < A.length; i++) {
    P[i + 1] = P[i] + A[i];
}

// 구간 [i, j] 합 → O(1)
int rangeSum = P[j + 1] - P[i];
```

### Prefix Sum 없이 활용하는 패턴

전체 합을 먼저 구하고, 순회하면서 한쪽에 더하고 다른 쪽에서 빼는 방식:

```java
// TapeEquilibrium — 배열을 둘로 나눌 때 차이 최소화
int total = 0;
for (int a : A) total += a;

int minDiff = Integer.MAX_VALUE;
int leftSum = 0;
int rightSum = total;

for (int i = 1; i < A.length; i++) {
    leftSum += A[i - 1];
    rightSum -= A[i - 1];
    minDiff = Math.min(minDiff, Math.abs(leftSum - rightSum));
}
```

## 코드 예시

### 빠진 숫자 찾기 (수학 공식 + 합)

1부터 N+1까지 중 하나가 빠진 배열에서 빠진 수를 찾는다:

```java
public int findMissing(int[] A) {
    int n = A.length;
    int expected = (n + 1) * (n + 2) / 2;  // 1~(N+1) 합 공식
    int actual = 0;
    for (int a : A) actual += a;
    return expected - actual;
}
```

> [!tip] 면접 팁
> 등차수열 합 공식 `N * (N + 1) / 2`는 자주 쓰인다. 정수 나눗셈 주의 — `(N + 1) * (N / 2)`와 `N * (N + 1) / 2`는 N이 홀수일 때 결과가 다르다.

### 구간 평균 최솟값 (응용)

```java
// Prefix Sum으로 구간 합 → 평균 계산
// MinAvgTwoSlice 등에서 활용
double minAvg = Double.MAX_VALUE;
int minIdx = 0;
for (int i = 0; i < A.length - 1; i++) {
    // 길이 2 구간
    double avg2 = (A[i] + A[i + 1]) / 2.0;
    if (avg2 < minAvg) {
        minAvg = avg2;
        minIdx = i;
    }
    // 길이 3 구간
    if (i < A.length - 2) {
        double avg3 = (A[i] + A[i + 1] + A[i + 2]) / 3.0;
        if (avg3 < minAvg) {
            minAvg = avg3;
            minIdx = i;
        }
    }
}
```

## 관련 문서

- [[비트 연산]]
- [[정렬]]
- [[dev/08-algorithm/자료구조/배열]]
