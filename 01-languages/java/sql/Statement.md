# Statement
> 역할: SQL을 문자열 그대로 실행

```java
Statement stmt = connection.createStatement();

// SQL을 직접 문자열로 실행
stmt.executeQuery("SELECT * FROM orders WHERE id = 123");
stmt.executeUpdate("DELETE FROM orders WHERE id = 456");
```

단점
- SQL Injection 취약
- 매번 SQL 파싱 (성능이 좋지 않음)
- 파라미터 바인딩 불가

# PreparedStatement
> 역할: SQL을 미리 컴파일하고, 파라미터 바인딩

```java
PreparedStatement pstmt = connection.prepareStatement(
    "SELECT * FROM orders WHERE id = ?"
);

// 파라미터 바인딩
pstmt.setLong(1, 123);
pstmt.executeQuery();
```

장점
- SQL Injection 방어
- SQL 재사용 (좋은 성능)
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

# CallableStatement (Stored Procedure용)
> 역할: DB의 Stored Procedure 호출

```java
PreparedStatement pstmt = connection.prepareStatement(
    "SELECT * FROM orders WHERE id = ?"
);

// 파라미터 바인딩
pstmt.setLong(1, 123);
pstmt.executeQuery();
```

용도
- Oracle, SQL Server의 Stored Procedure
- 복잡한 비즈니스 로직을 DB에서 실