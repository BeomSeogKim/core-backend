---
tags: [computer-science, os, cpu-bound, io-bound, thread]
status: completed
created: 2026-03-13
---

# CPU Bound vs IO Bound

## 핵심 개념

프로세스/스레드의 작업 특성에 따라 **CPU Bound**와 **IO Bound**로 구분한다. 이 구분은 스레드 풀 크기를 결정하는 핵심 기준이 된다.

- **CPU Bound** — CPU 연산이 대부분인 작업. CPU 성능이 처리 속도를 결정.
- **IO Bound** — I/O 대기가 대부분인 작업. 외부 시스템과의 병목이 처리 속도를 결정.

## 동작 원리

### CPU Burst vs IO Burst

프로세스가 실행되는 동안 CPU 연산과 I/O 대기를 반복한다:

<div style="font-family: -apple-system, sans-serif; max-width: 560px; margin: 12px auto; padding: 16px;">
<div style="font-size: 13px; font-weight: bold; margin-bottom: 8px;">CPU Bound 작업 — CPU burst가 지배적</div>
<div style="display: flex; height: 28px; border-radius: 4px; overflow: hidden; margin-bottom: 12px;">
<div style="background: #E74C3C; flex: 8; display: flex; align-items: center; justify-content: center; color: white; font-size: 11px;">CPU</div>
<div style="background: #BDC3C7; flex: 1; display: flex; align-items: center; justify-content: center; font-size: 10px;">IO</div>
<div style="background: #E74C3C; flex: 7; display: flex; align-items: center; justify-content: center; color: white; font-size: 11px;">CPU</div>
<div style="background: #BDC3C7; flex: 1; display: flex; align-items: center; justify-content: center; font-size: 10px;">IO</div>
<div style="background: #E74C3C; flex: 6; display: flex; align-items: center; justify-content: center; color: white; font-size: 11px;">CPU</div>
</div>
<div style="font-size: 13px; font-weight: bold; margin-bottom: 8px;">IO Bound 작업 — IO burst가 지배적</div>
<div style="display: flex; height: 28px; border-radius: 4px; overflow: hidden; margin-bottom: 4px;">
<div style="background: #3498DB; flex: 2; display: flex; align-items: center; justify-content: center; color: white; font-size: 11px;">CPU</div>
<div style="background: #BDC3C7; flex: 6; display: flex; align-items: center; justify-content: center; font-size: 10px;">IO 대기</div>
<div style="background: #3498DB; flex: 1; display: flex; align-items: center; justify-content: center; color: white; font-size: 11px;">CPU</div>
<div style="background: #BDC3C7; flex: 8; display: flex; align-items: center; justify-content: center; font-size: 10px;">IO 대기</div>
<div style="background: #3498DB; flex: 2; display: flex; align-items: center; justify-content: center; color: white; font-size: 11px;">CPU</div>
</div>
</div>

- **CPU Burst** — CPU가 연산을 수행하는 구간
- **IO Burst** — I/O 완료를 기다리는 구간

CPU burst가 긴 작업이 CPU bound, IO burst가 긴 작업이 IO bound다.

### CPU Bound 작업

CPU 연산 시간이 지배적. I/O 대기가 거의 없다.

| 예시 | 설명 |
|------|------|
| 영상 인코딩 | 프레임별 픽셀 연산 |
| 행렬 곱셈 | 대규모 수학 연산 |
| 머신러닝 학습 | 가중치 계산 반복 |
| 암호화/복호화 | 알고리즘 연산 집약 |

### IO Bound 작업

I/O 대기 시간이 지배적. CPU는 대부분 idle 상태.

| 예시 | 설명 |
|------|------|
| DB 쿼리 | 쿼리 전송 후 결과 대기 |
| API 호출 | HTTP 요청 후 응답 대기 |
| 파일 읽기/쓰기 | 디스크 I/O 대기 |
| Redis 조회 | 네트워크 왕복 대기 |

### 적정 스레드 수

#### CPU Bound: 코어 수 + 1

```
스레드 수 = CPU 코어 수 + 1
```

- CPU를 계속 사용하므로, 코어 수 이상의 스레드는 **불필요한 컨텍스트 스위칭**만 유발
- +1은 특정 스레드가 page fault 등으로 잠시 멈출 때를 대비한 여유분

```
4코어 CPU, CPU bound 작업:
  스레드 5개 → 각 코어에 1:1 매핑 + 여유 1개
  스레드 20개 → 컨텍스트 스위칭 오버헤드만 증가, 처리량 오히려 감소
```

#### IO Bound: 코어 × (1 + W/C)

```
스레드 수 = N × (1 + W / C)

N = CPU 코어 수
W = 대기 시간 (IO waiting)
C = 연산 시간 (CPU computing)
```

I/O 대기 동안 CPU가 놀기 때문에, 그 시간에 다른 스레드가 CPU를 활용하도록 스레드를 더 많이 잡는다.

```
4코어 CPU, IO 대기가 연산의 4배인 경우:
  스레드 수 = 4 × (1 + 4/1) = 20개
  → IO 대기 중인 스레드 16개 + CPU 연산 중인 스레드 4개
```

> [!warning] 공식은 출발점일 뿐
> 실제 서비스에서는 I/O 대기 시간이 일정하지 않고, 외부 시스템 상태에 따라 달라진다. 공식으로 초기값을 잡되, **부하 테스트(load test)**로 최적값을 찾아야 한다.

### 성능 향상 전략

| | CPU Bound | IO Bound |
|---|---|---|
| 병목 | CPU 연산 속도 | I/O 대기 시간 |
| 스레드 수 | 코어 수 + 1 | 코어 × (1 + W/C) |
| 스케일링 | **Scale-up** (CPU 성능 강화) | **Scale-out** (스레드/인스턴스 증가) |
| 병렬 처리 | Multiprocessing | Multithreading / Async IO |

## 관련 문서

- [[컨텍스트-스위칭]]
- [[프로세스,스레드]]
- [[스레드-모델]]
