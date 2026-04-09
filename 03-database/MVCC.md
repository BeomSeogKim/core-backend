---
tags: [database, mvcc, innodb, transaction]
status: completed
created: 2026-04-04
---

# MVCC (Multi-Version Concurrency Control)

## 핵심 개념

**MVCC**는 데이터의 여러 버전을 유지하여 **읽기 작업에 락을 걸지 않고도** 일관된 데이터를 보장하는 동시성 제어 기법이다. 읽기와 쓰기가 서로를 방해하지 않아 높은 동시성을 달성한다. MySQL InnoDB의 핵심 메커니즘이다.

> [!note] MVCC의 본질
> 락 기반 동시성 제어에서는 읽기도 락이 필요해 쓰기와 충돌한다. MVCC는 **변경 전 데이터를 별도로 보관**하여 읽기 트랜잭션이 락 없이 과거 버전을 읽을 수 있게 한다.

## 동작 원리

### InnoDB의 Row 숨겨진 필드

InnoDB의 모든 row에는 사용자가 보지 못하는 **숨겨진 필드**가 존재한다:

<div style="font-family: -apple-system, monospace; max-width: 560px; margin: 12px auto; padding: 16px; font-size: 12px;">
<div style="color: #888; font-size: 11px; margin-bottom: 6px;">InnoDB Row 내부 구조</div>
<div style="display: flex; gap: 2px;">
<div style="background: #8E44AD; color: white; padding: 10px 12px; border-radius: 4px 0 0 4px; text-align: center; flex: 1;">
<div style="font-weight: bold;">trx_id</div>
<div style="font-size: 10px; color: #D2B4DE;">마지막 수정한<br/>트랜잭션 ID</div>
</div>
<div style="background: #2980B9; color: white; padding: 10px 12px; text-align: center; flex: 1.2;">
<div style="font-weight: bold;">roll_pointer</div>
<div style="font-size: 10px; color: #AED6F1;">Undo Log의<br/>이전 버전 포인터</div>
</div>
<div style="background: #27AE60; color: white; padding: 10px 8px; text-align: center; flex: 0.6;">
<div style="font-weight: bold;">id</div>
</div>
<div style="background: #27AE60; color: white; padding: 10px 8px; text-align: center; flex: 0.8;">
<div style="font-weight: bold;">name</div>
</div>
<div style="background: #27AE60; color: white; padding: 10px 8px; border-radius: 0 4px 4px 0; text-align: center; flex: 0.6;">
<div style="font-weight: bold;">age</div>
</div>
</div>
<div style="display: flex; gap: 2px; margin-top: 2px;">
<div style="flex: 1; text-align: center; font-size: 10px; color: #8E44AD;">숨겨진 필드</div>
<div style="flex: 1.2; text-align: center; font-size: 10px; color: #2980B9;">숨겨진 필드</div>
<div style="flex: 2; text-align: center; font-size: 10px; color: #27AE60;">사용자 정의 컬럼들</div>
</div>
</div>

| 필드 | 역할 |
|------|------|
| **trx_id** | 이 row를 마지막으로 수정한 트랜잭션 ID |
| **roll_pointer** | Undo Log에서 이 row의 이전 버전을 가리키는 포인터 |

### Undo Log — 버전 체인

UPDATE가 발생하면 변경 전 데이터를 **Undo Log**에 보관한다. roll_pointer로 연결되어 **버전 체인**을 형성한다.

<div style="font-family: -apple-system, monospace; max-width: 480px; margin: 12px auto; padding: 16px; font-size: 12px;">
<div style="color: #888; font-size: 11px; margin-bottom: 8px;">시나리오: name을 "민수" → "영희" → "철수"로 순차 변경</div>
<div style="background: #1e1e2e; border-radius: 8px; padding: 12px;">
<div style="color: #f9e2af; font-size: 11px; margin-bottom: 6px;">실제 테이블 (최신 데이터)</div>
<div style="background: #45475a; padding: 8px 12px; border-radius: 4px; color: #cdd6f4;">
<span style="color: #a6e3a1;">id=1</span> name=<b style="color: #89b4fa;">"철수"</b> <span style="color: #8E44AD;">trx_id=100</span>
</div>
<div style="text-align: center; color: #f9e2af; padding: 4px;">↓ roll_pointer</div>
<div style="color: #f9e2af; font-size: 11px; margin-bottom: 6px;">Undo Log (이전 버전 체인)</div>
<div style="background: #313244; padding: 8px 12px; border-radius: 4px; color: #cdd6f4; border-left: 3px solid #f9e2af;">
<span style="color: #a6e3a1;">id=1</span> name=<b style="color: #89b4fa;">"영희"</b> <span style="color: #8E44AD;">trx_id=50</span>
<span style="color: #6c7086; font-size: 10px;"> ← 100이 변경하기 전</span>
</div>
<div style="text-align: center; color: #f9e2af; padding: 4px;">↓ roll_pointer</div>
<div style="background: #313244; padding: 8px 12px; border-radius: 4px; color: #cdd6f4; border-left: 3px solid #f9e2af;">
<span style="color: #a6e3a1;">id=1</span> name=<b style="color: #89b4fa;">"민수"</b> <span style="color: #8E44AD;">trx_id=10</span>
<span style="color: #6c7086; font-size: 10px;"> ← 최초 버전 (NULL)</span>
</div>
</div>
</div>

트랜잭션이 과거 데이터를 읽어야 할 때, 이 체인을 따라가며 자기가 "볼 수 있는" 버전을 찾는다.

### Read View — 가시성 판단 기준

트랜잭션이 SELECT를 실행할 때 **Read View**가 생성된다. Read View는 "어느 트랜잭션의 변경까지 볼 수 있는가"를 결정하는 스냅샷이다.

<div style="font-family: -apple-system, monospace; max-width: 420px; margin: 12px auto; padding: 16px; font-size: 12px;">
<div style="background: #1e1e2e; border-radius: 8px; padding: 12px;">
<div style="color: #89b4fa; font-weight: bold; margin-bottom: 8px; text-align: center;">Read View (trx_id=75가 생성)</div>
<div style="display: flex; flex-direction: column; gap: 6px;">
<div style="background: #45475a; padding: 8px 12px; border-radius: 4px; color: #cdd6f4;">
<span style="color: #f9e2af;">creator_trx_id</span> = 75 <span style="color: #6c7086;">← 이 Read View를 만든 트랜잭션</span>
</div>
<div style="background: #45475a; padding: 8px 12px; border-radius: 4px; color: #cdd6f4;">
<span style="color: #f38ba8;">m_ids</span> = [50, 75] <span style="color: #6c7086;">← 아직 커밋 안 된 활성 트랜잭션들</span>
</div>
<div style="background: #45475a; padding: 8px 12px; border-radius: 4px; color: #cdd6f4;">
<span style="color: #a6e3a1;">min_trx_id</span> = 50 <span style="color: #6c7086;">← m_ids 중 최솟값</span>
</div>
<div style="background: #45475a; padding: 8px 12px; border-radius: 4px; color: #cdd6f4;">
<span style="color: #fab387;">max_trx_id</span> = 100 <span style="color: #6c7086;">← 다음에 생성될 트랜잭션 ID</span>
</div>
</div>
</div>
</div>

### 가시성 판단 알고리즘

row의 trx_id를 Read View와 비교하여 **보일지 말지** 결정한다:

<div style="font-family: -apple-system, sans-serif; max-width: 520px; margin: 12px auto; padding: 16px; font-size: 12px;">
<div style="background: #2C3E50; color: white; padding: 8px 12px; border-radius: 6px 6px 0 0; font-weight: bold; text-align: center;">row의 trx_id를 확인</div>
<div style="display: flex; flex-direction: column; gap: 4px; margin-top: 4px;">
<div style="display: flex; gap: 4px;">
<div style="flex: 1; background: #27AE60; color: white; padding: 8px; border-radius: 4px;">
<div style="font-weight: bold;">trx_id < min_trx_id</div>
<div style="font-size: 11px; margin-top: 4px;">Read View 생성 전에 이미 커밋됨</div>
<div style="font-size: 13px; margin-top: 4px;">→ ✅ <b>보임</b></div>
</div>
<div style="flex: 1; background: #E74C3C; color: white; padding: 8px; border-radius: 4px;">
<div style="font-weight: bold;">trx_id >= max_trx_id</div>
<div style="font-size: 11px; margin-top: 4px;">Read View 생성 후에 시작된 트랜잭션</div>
<div style="font-size: 13px; margin-top: 4px;">→ ❌ <b>안 보임</b> → Undo Log 탐색</div>
</div>
</div>
<div style="display: flex; gap: 4px;">
<div style="flex: 1; background: #E74C3C; color: white; padding: 8px; border-radius: 4px;">
<div style="font-weight: bold;">trx_id가 m_ids에 있음</div>
<div style="font-size: 11px; margin-top: 4px;">아직 커밋 안 된 트랜잭션의 변경</div>
<div style="font-size: 13px; margin-top: 4px;">→ ❌ <b>안 보임</b> → Undo Log 탐색</div>
</div>
<div style="flex: 1; background: #27AE60; color: white; padding: 8px; border-radius: 4px;">
<div style="font-weight: bold;">min~max 사이 + m_ids에 없음</div>
<div style="font-size: 11px; margin-top: 4px;">Read View 생성 전에 커밋 완료됨</div>
<div style="font-size: 13px; margin-top: 4px;">→ ✅ <b>보임</b></div>
</div>
</div>
</div>
</div>

### 전체 동작 예시

<div style="font-family: -apple-system, monospace; max-width: 520px; margin: 12px auto; padding: 16px; font-size: 12px;">
<div style="background: #1e1e2e; border-radius: 8px; padding: 12px;">
<div style="color: #89b4fa; font-weight: bold; margin-bottom: 8px;">시간 흐름 →</div>
<div style="display: flex; flex-direction: column; gap: 4px;">
<div style="background: #313244; padding: 6px 10px; border-radius: 4px; color: #cdd6f4;">
<span style="color: #a6e3a1;">trx_id=10</span> INSERT name="민수" → <span style="color: #27AE60; font-weight: bold;">COMMIT ✓</span>
</div>
<div style="background: #313244; padding: 6px 10px; border-radius: 4px; color: #cdd6f4;">
<span style="color: #f9e2af;">trx_id=50</span> UPDATE name="영희" → <span style="color: #f38ba8; font-weight: bold;">미커밋 ✗</span>
</div>
<div style="background: #45475a; padding: 6px 10px; border-radius: 4px; color: #cdd6f4; border-left: 3px solid #89b4fa;">
<span style="color: #89b4fa;">trx_id=75</span> SELECT * WHERE id=1 ← <span style="color: #89b4fa;">Read View 생성</span>
</div>
</div>
<div style="margin-top: 10px; color: #cdd6f4;">
<div style="color: #f9e2af; font-weight: bold; margin-bottom: 4px;">Read View: m_ids=[50], min=50, max=76</div>
<div style="margin-top: 6px;">
<div style="color: #6c7086;">1단계: 최신 row</div>
<div style="margin-left: 12px;">name="영희", trx_id=<span style="color: #f9e2af;">50</span> → <span style="color: #f38ba8;">m_ids에 있음 → ❌ 안 보임</span></div>
</div>
<div style="margin-top: 4px;">
<div style="color: #6c7086;">2단계: Undo Log 이전 버전</div>
<div style="margin-left: 12px;">name="민수", trx_id=<span style="color: #a6e3a1;">10</span> → <span style="color: #27AE60;">10 < 50(min) → ✅ 보임</span></div>
</div>
<div style="margin-top: 8px; background: #45475a; padding: 6px 10px; border-radius: 4px; text-align: center;">
결과: <b style="color: #89b4fa;">name="민수"</b> <span style="color: #6c7086;">(50번의 미커밋 변경은 무시)</span>
</div>
</div>
</div>
</div>

### READ COMMITTED vs REPEATABLE READ

같은 MVCC인데 **Read View 생성 시점**만 다르다:

<div style="font-family: -apple-system, sans-serif; max-width: 560px; margin: 12px auto; padding: 16px; font-size: 12px;">
<div style="display: flex; gap: 12px;">
<div style="flex: 1; border: 2px solid #E74C3C; border-radius: 8px; padding: 12px;">
<div style="background: #E74C3C; color: white; padding: 6px; border-radius: 4px; text-align: center; font-weight: bold; margin-bottom: 8px;">READ COMMITTED</div>
<div style="font-size: 11px; line-height: 1.8; color: #333;">
Read View: <b>매 SELECT마다</b> 새로 생성<br/><br/>
<span style="color: #888;">trx A: UPDATE → COMMIT</span><br/>
<span style="color: #2980B9;">trx B: 1st SELECT</span> → Read View①<br/>
결과: name=<b>"민수"</b><br/><br/>
<span style="color: #2980B9;">trx B: 2nd SELECT</span> → Read View② (새로 생성)<br/>
결과: name=<b style="color: #E74C3C;">"영희"</b> ← A 커밋 반영!<br/><br/>
<span style="color: #E74C3C;">⚠ Non-Repeatable Read 발생</span>
</div>
</div>
<div style="flex: 1; border: 2px solid #27AE60; border-radius: 8px; padding: 12px;">
<div style="background: #27AE60; color: white; padding: 6px; border-radius: 4px; text-align: center; font-weight: bold; margin-bottom: 8px;">REPEATABLE READ</div>
<div style="font-size: 11px; line-height: 1.8; color: #333;">
Read View: <b>첫 SELECT에서 1번</b>만 생성<br/><br/>
<span style="color: #888;">trx A: UPDATE → COMMIT</span><br/>
<span style="color: #2980B9;">trx B: 1st SELECT</span> → Read View①<br/>
결과: name=<b>"민수"</b><br/><br/>
<span style="color: #2980B9;">trx B: 2nd SELECT</span> → Read View① 재사용<br/>
결과: name=<b>"민수"</b> ← 동일 스냅샷<br/><br/>
<span style="color: #27AE60;">✓ 일관된 읽기 보장</span>
</div>
</div>
</div>
</div>

| 격리 수준 | Read View 생성 | 결과 |
|---|---|---|
| **READ COMMITTED** | **매 SELECT마다** 새로 생성 | 다른 트랜잭션 커밋이 즉시 반영 |
| **REPEATABLE READ** | **트랜잭션 첫 SELECT에서 1번** | 동일 스냅샷 유지 → 일관된 읽기 |

### MVCC와 락의 관계

MVCC는 **읽기 작업에만** 적용된다. 쓰기 작업은 여전히 락이 필요하다.

<div style="font-family: -apple-system, sans-serif; max-width: 420px; margin: 12px auto; padding: 16px; font-size: 12px;">
<div style="display: flex; flex-direction: column; gap: 4px;">
<div style="display: flex; gap: 4px;">
<div style="flex: 1; background: #27AE60; color: white; padding: 8px; border-radius: 4px; text-align: center;">
<div style="font-weight: bold;">읽기 vs 읽기</div>
<div style="font-size: 11px;">MVCC — 락 없음, 동시 가능</div>
</div>
<div style="flex: 1; background: #27AE60; color: white; padding: 8px; border-radius: 4px; text-align: center;">
<div style="font-weight: bold;">읽기 vs 쓰기</div>
<div style="font-size: 11px;">MVCC — 락 없음, 동시 가능</div>
</div>
</div>
<div style="background: #E74C3C; color: white; padding: 8px; border-radius: 4px; text-align: center;">
<div style="font-weight: bold;">쓰기 vs 쓰기</div>
<div style="font-size: 11px;">락 필요 (Row Lock) — 동시 불가</div>
</div>
</div>
<div style="text-align: center; margin-top: 6px; font-size: 11px; color: #888;">대부분의 워크로드는 읽기가 많으므로, MVCC만으로 동시성이 크게 향상된다</div>
</div>

## 관련 문서

- [[트랜잭션]] — 격리 수준과 이상 현상
- [[데이터베이스-락]] — 쓰기 작업의 락
- [[스토리지엔진]] — InnoDB의 MVCC 지원
- [[인덱스]] — Undo Log와 인덱스의 관계
