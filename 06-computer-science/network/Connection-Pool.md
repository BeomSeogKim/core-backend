---
tags: [computer-science, network, database, performance]
status: completed
created: 2026-04-21
---

# Connection Pool

## 핵심 개념

**Connection Pool** — 미리 연결을 맺어 Pool에 보관해두고, 요청마다 재사용하는 방식. 매번 연결 생성 비용을 제거해 성능을 높인다.

## 동작 원리

### 연결 생성 비용 (왜 필요한가)

```
매번 신규 연결 시:
  TCP 3-way Handshake
  → DB 세션 생성
  → DB 인증(사용자/비밀번호 확인)
  → 쿼리 실행
  → 연결 종료

  ⇒ 쿼리 자체보다 연결 수립 비용이 더 클 수 있음
```

### Connection Pool 흐름

```
애플리케이션 시작 시:
  ┌─────────────────────────────────┐
  │         Connection Pool         │
  │  [Con1] [Con2] [Con3] [Con4]   │  ← 미리 연결 생성
  └─────────────────────────────────┘

요청 들어왔을 때:
  Thread1 → [Con1] 획득 → 쿼리 → 반납
  Thread2 → [Con2] 획득 → 쿼리 → 반납
  Thread3 → [Con3] 획득 → 쿼리 → 반납

모든 Connection 사용 중일 때:
  Thread4 → 대기 (connectionTimeout 설정값만큼)
           → 시간 초과 시 SQLTimeoutException 발생
```

### Timeout 종류

```
Connection Timeout  — 연결을 맺는 데 걸리는 시간 제한
                      TCP Handshake 단계에서 서버 응답 없을 때

Socket Timeout      — 연결은 됐는데 데이터 응답이 안 올 때의 시간 제한
                      DB에 연결됐지만 쿼리 응답이 너무 오래 걸릴 때

Connection Timeout: [연결 맺기] ─── 타임아웃
Socket Timeout:     [연결됨] ── 데이터 기다리는 중 ─── 타임아웃
```

### Pool 사이즈 최적화

```
너무 크면:
  - 메모리 과점유 → OOM 위험
  - Thread 경쟁 증가 → Context Switching 오버헤드

너무 작으면:
  - 대기 큐 적체 → Timeout 빈발
  - Throughput 저하 → 사용자 응답 지연

HikariCP 권장 공식:
  pool size = CPU 코어 수 × 2 + 유효 스핀들 수
  (실무에서는 CPU 코어 수 × 2 로 시작)

이유:
  DB I/O 대기 시간 동안 CPU가 유휴 상태
  → 다른 Connection이 CPU를 활용할 수 있음
  → 코어 수보다 조금 더 잡는 것이 효율적
```

## HikariCP 설정 (Spring Boot 기본)

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10        # 최대 Connection 수
      minimum-idle: 5              # 최소 유휴 Connection 수
      connection-timeout: 30000    # 30초 — Connection 획득 대기 시간
      idle-timeout: 600000         # 10분 — 유휴 Connection 유지 시간
      max-lifetime: 1800000        # 30분 — Connection 최대 수명
```

## Pool 사이즈 트레이드오프

| | 너무 큼 | 적정 | 너무 작음 |
|---|---|---|---|
| 메모리 | OOM 위험 | 적정 | 여유 |
| Throughput | Context Switching 증가 | **최대** | 병목 |
| 대기 | 없음 | 없음 | Timeout 빈발 |

## 코드 예시

```java
// HikariCP 직접 설정
HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:mysql://localhost:3306/mydb");
config.setMaximumPoolSize(10);
config.setConnectionTimeout(30_000);
config.setIdleTimeout(600_000);

HikariDataSource dataSource = new HikariDataSource(config);
```

```java
// Spring — DataSource 주입 후 Connection 사용
@Repository
public class UserRepository {
    private final DataSource dataSource;

    public User findById(Long id) throws SQLException {
        try (Connection conn = dataSource.getConnection();  // Pool에서 획득
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE id = ?")) {
            ps.setLong(1, id);
            // try-with-resources → 자동 반납
        }
    }
}
```

## 관련 문서

- [[2-Areas/backend/06-computer-science/network/TCP-UDP]]
- [[2-Areas/backend/03-database/DB-Connection]]
- [[2-Areas/backend/02-frameworks/spring/data/JPA-기초]]
