---
tags: [java, sql, statement]
status: completed
created: 2026-01-20
---

# Statement

## 핵심 개념

Java JDBC에서 SQL을 실행하는 인터페이스는 **Statement**, **PreparedStatement**, **CallableStatement** 세 가지가 있다. Statement는 SQL을 문자열 그대로 실행하고, **PreparedStatement**는 SQL을 미리 컴파일하여 파라미터 바인딩을 지원하며, **CallableStatement**는 Stored Procedure 호출에 사용된다.

> [!warning] Statement 직접 사용 금지
> Statement는 SQL Injection에 취약하고, 매번 SQL을 파싱하므로 성능이 좋지 않다. 실무에서는 반드시 PreparedStatement를 사용해야 한다.

## 동작 원리

### Statement

- SQL을 문자열 그대로 실행
- 매번 SQL 파싱 수행 (성능이 좋지 않음)
- 파라미터 바인딩 불가
- SQL Injection 취약

### PreparedStatement

- SQL을 미리 컴파일하고, 파라미터 바인딩
- SQL Injection 방어
- SQL 재사용으로 좋은 성능
- 파라미터 자동 이스케이프

사용처
- JPA/Hibernate
- MyBatis
- JdbcTemplate

구현체
- HikariCP
- Tomcat JDBC Pool
- MySQL Connector (JDBC Driver)
- MariaDB Connector

### CallableStatement (Stored Procedure용)

- DB의 Stored Procedure 호출
- Oracle, SQL Server의 Stored Procedure
- 복잡한 비즈니스 로직을 DB에서 실행

> [!note] Statement 계층 구조
> `Statement` → `PreparedStatement` → `CallableStatement` 순서로 상속 관계를 이룬다. PreparedStatement는 Statement를 확장하고, CallableStatement는 PreparedStatement를 확장한다. 이 인터페이스들은 [[dev/01-languages/java/JVM|JVM]] 위에서 JDBC Driver를 통해 구현된다.

## 코드 예시

```java
// Statement - SQL 문자열 직접 실행
Statement stmt = connection.createStatement();
stmt.executeQuery("SELECT * FROM orders WHERE id = 123");
stmt.executeUpdate("DELETE FROM orders WHERE id = 456");
```

```java
// PreparedStatement - 파라미터 바인딩
PreparedStatement pstmt = connection.prepareStatement(
    "SELECT * FROM orders WHERE id = ?"
);
pstmt.setLong(1, 123);
pstmt.executeQuery();
```

```java
// CallableStatement - Stored Procedure 호출
CallableStatement cstmt = connection.prepareCall(
    "{call get_order_total(?, ?)}"
);
cstmt.setLong(1, 123);
cstmt.registerOutParameter(2, Types.DECIMAL);
cstmt.execute();
BigDecimal total = cstmt.getBigDecimal(2);
```

> [!tip] Connection Pool과 PreparedStatement 캐싱
> HikariCP 등의 Connection Pool은 PreparedStatement를 캐싱하여 재사용한다. 이를 통해 SQL 파싱 비용을 줄이고, [[dev/03-database/트랜잭션|트랜잭션]] 처리 성능을 향상시킬 수 있다.

## 관련 문서

- [[dev/03-database/트랜잭션|트랜잭션]] — JDBC 트랜잭션 관리
- [[dev/03-database/스토리지엔진|스토리지엔진]] — DB 내부의 SQL 처리 구조
- [[dev/01-languages/java/JVM|JVM]] — JDBC Driver와 JVM 실행 환경
