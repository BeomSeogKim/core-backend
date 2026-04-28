---
tags: [computer-science, os, cpu, cache, register]
status: completed
created: 2026-03-13
---

# CPU 구조

## 핵심 개념

**CPU (Central Processing Unit)**는 명령어를 읽고, 해석하고, 실행하는 연산 장치다. 내부에 **코어(Core)**, **레지스터(Register)**, **캐시(Cache)**로 구성되며, 이들의 계층 구조가 성능을 결정한다.

## 동작 원리

### CPU 내부 구성

<div style="font-family: -apple-system, sans-serif; padding: 16px; max-width: 640px;">
<div style="background: #2C3E50; color: white; padding: 12px 24px; border-radius: 8px; text-align: center; font-size: 18px; font-weight: bold; margin-bottom: 12px;">CPU</div>
<div style="display: flex; gap: 12px;">
<div style="flex: 1; border: 2px solid #27AE60; border-radius: 8px; padding: 12px;">
<div style="background: #27AE60; color: white; padding: 8px; border-radius: 4px; text-align: center; font-weight: bold; margin-bottom: 10px;">Core (코어)</div>
<div style="background: #E8F8F0; padding: 8px; border-radius: 4px; margin-bottom: 4px; font-size: 13px;"><b>ALU</b> — 산술/논리 연산</div>
<div style="background: #E8F8F0; padding: 8px; border-radius: 4px; margin-bottom: 4px; font-size: 13px;"><b>CU</b> — 명령어 해석/제어</div>
<div style="border: 1px dashed #27AE60; border-radius: 4px; padding: 8px; margin-top: 6px;">
<div style="font-size: 12px; font-weight: bold; color: #27AE60; margin-bottom: 4px;">Registers</div>
<div style="font-size: 12px; color: #555;">PC — 다음 명령어 주소</div>
<div style="font-size: 12px; color: #555;">SP — 스택 위치</div>
<div style="font-size: 12px; color: #555;">IR — 현재 명령어</div>
<div style="font-size: 12px; color: #555;">범용 — 연산 중간값</div>
</div>
</div>
<div style="flex: 1; border: 2px solid #E67E22; border-radius: 8px; padding: 12px;">
<div style="background: #E67E22; color: white; padding: 8px; border-radius: 4px; text-align: center; font-weight: bold; margin-bottom: 10px;">Cache (캐시)</div>
<div style="background: #FDF2E9; padding: 8px; border-radius: 4px; margin-bottom: 4px; font-size: 13px;"><b>L1</b> ~1ns — 코어 전용, 가장 빠름</div>
<div style="background: #FDF2E9; padding: 8px; border-radius: 4px; margin-bottom: 4px; font-size: 13px;"><b>L2</b> ~3-10ns — 코어 전용</div>
<div style="background: #FDF2E9; padding: 8px; border-radius: 4px; font-size: 13px;"><b>L3</b> ~10-30ns — 코어 간 공유</div>
<div style="margin-top: 8px; padding: 6px; font-size: 11px; color: #888; text-align: center;">CPU ↔ RAM 속도 차이를 줄이는 고속 버퍼</div>
</div>
</div>
</div>

### 메모리 계층 구조

빠를수록 비싸고 작고, 느릴수록 싸고 크다. 계층 구조로 쌓아서 비용 대비 성능을 최적화한다.

<div style="font-family: -apple-system, sans-serif; max-width: 480px; margin: 12px auto; padding: 16px;">
<div style="background: #C0392B; color: white; padding: 6px; text-align: center; border-radius: 4px 4px 0 0; font-weight: bold; font-size: 13px;">Register ~0.3ns — SRAM</div>
<div style="background: #E74C3C; color: white; padding: 6px; text-align: center; margin: 2px 24px 0; font-size: 13px;">L1 Cache ~1ns — SRAM</div>
<div style="background: #E67E22; color: white; padding: 6px; text-align: center; margin: 2px 48px 0; font-size: 13px;">L2 Cache ~3-10ns — SRAM</div>
<div style="background: #F39C12; color: white; padding: 6px; text-align: center; margin: 2px 72px 0; font-size: 13px;">L3 Cache ~10-30ns — SRAM</div>
<div style="background: #27AE60; color: white; padding: 6px; text-align: center; margin: 2px 96px 0; font-size: 13px;">RAM ~50-100ns — DRAM</div>
<div style="background: #2980B9; color: white; padding: 6px; text-align: center; margin: 2px 120px 0; font-size: 13px;">SSD ~100μs</div>
<div style="background: #8E44AD; color: white; padding: 6px; text-align: center; margin: 2px 144px 0; border-radius: 0 0 4px 4px; font-size: 13px;">HDD ~10ms</div>
<div style="display: flex; justify-content: space-between; margin-top: 8px; font-size: 11px; color: #888;">
<span>⬆ 빠르고 · 작고 · 비쌈</span>
<span>느리고 · 크고 · 저렴 ⬇</span>
</div>
</div>

#### SRAM vs DRAM

| | SRAM (캐시, 레지스터) | DRAM (RAM) |
|---|---|---|
| 구조 | 트랜지스터 6개/비트 | 트랜지스터 1개 + 커패시터/비트 |
| 속도 | 빠름 | 느림 |
| refresh | 불필요 | 필요 (전하가 빠져나감) |
| 가격 | 비쌈 | 저렴 |
| 집적도 | 낮음 (크기 큼) | 높음 (크기 작음) |

### Fetch-Decode-Execute Cycle

CPU가 프로그램을 실행하는 핵심 사이클. 전원이 켜져 있는 한 무한 반복한다.

<div style="font-family: -apple-system, sans-serif; max-width: 560px; margin: 12px auto; padding: 16px;">
<div style="display: flex; align-items: center; gap: 4px; flex-wrap: wrap; justify-content: center;">
<div style="background: #2980B9; color: white; padding: 10px 16px; border-radius: 8px; text-align: center; min-width: 100px;">
<div style="font-weight: bold; font-size: 14px;">1. Fetch</div>
<div style="font-size: 11px; margin-top: 4px;">PC → 명령어 → IR</div>
</div>
<div style="font-size: 20px; color: #999;">→</div>
<div style="background: #E67E22; color: white; padding: 10px 16px; border-radius: 8px; text-align: center; min-width: 100px;">
<div style="font-weight: bold; font-size: 14px;">2. Decode</div>
<div style="font-size: 11px; margin-top: 4px;">CU가 명령어 해석</div>
</div>
<div style="font-size: 20px; color: #999;">→</div>
<div style="background: #C0392B; color: white; padding: 10px 16px; border-radius: 8px; text-align: center; min-width: 100px;">
<div style="font-weight: bold; font-size: 14px;">3. Execute</div>
<div style="font-size: 11px; margin-top: 4px;">ALU가 연산 수행</div>
</div>
<div style="font-size: 20px; color: #999;">→</div>
<div style="background: #27AE60; color: white; padding: 10px 16px; border-radius: 8px; text-align: center; min-width: 100px;">
<div style="font-weight: bold; font-size: 14px;">4. PC 증가</div>
<div style="font-size: 11px; margin-top: 4px;">다음 명령어로</div>
</div>
<div style="font-size: 20px; color: #999;">↩</div>
</div>
<div style="text-align: center; margin-top: 8px; font-size: 11px; color: #888;">무한 반복 — 전원이 켜져 있는 한 계속 순환</div>
</div>

고수준 코드 한 줄이 CPU 입장에서는 여러 번의 사이클이다:

```java
count++;  // Java 한 줄
```

```
→ CPU 명령어 3개:
  1. LOAD  count → 레지스터  (Fetch-Decode-Execute)
  2. ADD   레지스터 + 1       (Fetch-Decode-Execute)
  3. STORE 레지스터 → count   (Fetch-Decode-Execute)
```

이 사이 사이에 [[컨텍스트-스위칭]]이 끼어들 수 있기 때문에 [[동기화|Race Condition]]이 발생한다.

## 관련 문서

- [[커널]]
- [[가상-메모리]]
- [[컨텍스트-스위칭]]
- [[프로세스,스레드]]
