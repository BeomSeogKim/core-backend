---
tags: [java, exception]
status: completed
created: 2026-04-02
---

# Checked vs Unchecked Exception

## 핵심 개념

Java의 예외는 **Checked Exception**(컴파일러가 처리를 강제)과 **Unchecked Exception**(강제하지 않음)으로 나뉜다. 구분 기준은 `RuntimeException`을 상속하느냐 아니냐다.

## 동작 원리

### 예외 클래스 계층 구조

```
Throwable
├── Error                          ← 시스템 레벨, 복구 불가
│   ├── OutOfMemoryError
│   ├── StackOverflowError
│   └── VirtualMachineError
│
└── Exception
    ├── IOException                ← Checked Exception
    ├── SQLException               ← Checked Exception
    ├── FileNotFoundException      ← Checked Exception
    │
    └── RuntimeException           ← Unchecked Exception
        ├── NullPointerException
        ├── IllegalArgumentException
        ├── IndexOutOfBoundsException
        ├── ClassCastException
        └── ArithmeticException
```

### Checked Exception — 컴파일 타임 강제

`RuntimeException`을 상속하지 **않는** Exception. 컴파일러가 **반드시 처리하도록 강제**한다. 처리하지 않으면 컴파일 에러.

```java
// 반드시 try-catch 또는 throws로 처리해야 함
public void readFile(String path) throws IOException {  // throws 선언
    BufferedReader reader = new BufferedReader(new FileReader(path));
    String line = reader.readLine();
}

// 또는 try-catch로 처리
public void readFile(String path) {
    try {
        BufferedReader reader = new BufferedReader(new FileReader(path));
    } catch (IOException e) {
        // 예외 처리
    }
}
```

**사용 시점**: 호출자가 예외를 **인지하고 적절히 대응해야 하는** 상황
- 파일 I/O (`IOException`)
- DB 접근 (`SQLException`)
- 네트워크 통신

### Unchecked Exception — 런타임 예외

`RuntimeException`을 상속하는 Exception. 컴파일러가 처리를 강제하지 않으며, **런타임에 발생**한다.

```java
// 컴파일 에러 없음 — 처리 안 해도 됨
public void process(String input) {
    int length = input.length();  // input이 null이면 NullPointerException
    int[] arr = new int[10];
    arr[20] = 1;                  // IndexOutOfBoundsException
}
```

**사용 시점**: **프로그래밍 실수**로 발생하는 예외. 호출자가 처리하기보다 코드를 수정해야 하는 상황
- null 체크 미흡 (`NullPointerException`)
- 잘못된 인자 전달 (`IllegalArgumentException`)
- 배열 범위 초과 (`IndexOutOfBoundsException`)

### 비교 요약

| | Checked Exception | Unchecked Exception |
|---|---|---|
| 상속 | Exception (RuntimeException 제외) | **RuntimeException** |
| 처리 강제 | **컴파일러가 강제** (try-catch / throws) | 강제 안 함 |
| 발생 시점 | 주로 외부 환경 (I/O, 네트워크, DB) | **프로그래밍 실수** (null, 범위 초과) |
| 대응 방식 | 호출자가 인지하고 대응 | 코드 수정으로 예방 |
| 대표 예시 | IOException, SQLException | NullPointerException, IllegalArgumentException |

### Error와의 구분

**Error**는 시스템 레벨의 심각한 오류로, 애플리케이션에서 **잡지 않는 것이 원칙**이다.

| | Error | Exception |
|---|---|---|
| 원인 | JVM/시스템 문제 | 애플리케이션 로직 |
| 복구 가능 | **불가능** | 가능 |
| 예시 | OutOfMemoryError, StackOverflowError | IOException, NullPointerException |

## 관련 문서

- [[JVM]] — 예외 발생 시 JVM Stack에서의 처리
- [[OOP-4가지-특징]] — 예외 클래스 계층과 상속
