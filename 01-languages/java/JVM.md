---
tags: [java, jvm]
status: completed
created: 2026-02-21
---

# JVM

## 핵심 개념

**JVM(Java Virtual Machine)**은 Java 바이트코드를 실행하는 가상 머신으로, 플랫폼 독립성을 실현하는 핵심 컴포넌트다. "Write Once, Run Anywhere" 철학을 구현하여 하나의 바이트코드(.class)가 어떤 OS에서든 동일하게 실행된다.

> [!note] JVM의 본질
> JVM은 하드웨어가 아니라 소프트웨어로 구현된 가상 컴퓨터다. 플랫폼별로 다르게 구현되지만, 바이트코드 명세는 동일하므로 개발자는 플랫폼 차이를 신경 쓸 필요가 없다.

## 동작 원리

### 크로스 플랫폼 문제의 본질

**플랫폼 간 차이점**

하드웨어 아키텍처
- Intel x86-64: `MOV rax, 10` / `ADD rax, 7`
- ARM64: `MOV X0, #10` / `ADD X0, X0, #7`
- 동일 로직, 완전히 다른 기계어

운영체제 API
- Linux: `open()` 시스템 콜
- Windows: `CreateFileW()` API
- 파일 열기 같은 기본 동작도 OS마다 다른 방식

표준 라이브러리
- Linux: `libc.so` (POSIX)
- Windows: `msvcrt.dll` (Windows API)

### 전통적 해결 방법의 한계

C/C++ 방식
> 하나의 소스코드 -> 4개 플랫폼 x 각각 컴파일 = 4개 바이너리
> - 새 기능 추가 시 4번 빌드
> - 플랫폼별 테스트 필요
> - 4개 배포 파일 관리

### JVM의 해결 전략: 중간 언어 (Intermediate Language)

핵심 아이디어
```text
소스 코드 (.java)
    ↓ javac 컴파일 (1번만)
바이트 코드 (.class) <- 플랫폼 독립적
    ↓ 각 플랫폼의 JVM이 실행
기계어 (플랫폼)
```

> [!tip] Write Once, Run Anywhere
> 바이트코드는 플랫폼에 독립적인 중간 형태이고, JVM이 플랫폼별로 구현된 실행 엔진 역할을 한다. [[Class]] 파일은 JVM 위에서 로드되어 실행된다.

JVM은 플랫폼별로 다르게 구현됨

> [!warning] JVM과 OS의 관계
> JVM 자체는 플랫폼 종속적이다. 각 OS(Linux, Windows, macOS)마다 별도의 JVM 구현체가 필요하며, [[2-Areas/backend/06-computer-science/os/프로세스,스레드|프로세스와 스레드]] 모델도 OS에 따라 JVM이 다르게 매핑한다. [[2-Areas/backend/06-computer-science/os/스레드-모델|스레드 모델]]을 참고하면 JVM의 스레드 매핑 전략을 이해할 수 있다.

## 코드 예시

```java
// Java 소스 코드 → javac → 바이트코드 → JVM 실행
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
```

```text
// javap -c HelloWorld.class로 확인하는 바이트코드
public static void main(java.lang.String[]);
  Code:
    0: getstatic     #2  // System.out
    3: ldc           #3  // "Hello, World!"
    5: invokevirtual #4  // println
    8: return
```

## 관련 문서

- [[Class]] — 클래스 로딩과 JVM 메모리 구조
- [[2-Areas/backend/06-computer-science/os/프로세스,스레드|프로세스,스레드]] — OS 수준의 프로세스/스레드 개념
- [[2-Areas/backend/06-computer-science/os/스레드-모델|스레드-모델]] — JVM 스레드와 OS 스레드 매핑
