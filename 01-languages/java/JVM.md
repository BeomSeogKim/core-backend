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

### 클래스 로더 (Class Loader)

바이트코드(.class)를 JVM 메모리에 로드하는 서브시스템. 세 가지 로더가 **위임 모델(Delegation Model)**로 동작한다:

| 로더 | 역할 | 로드 대상 |
|------|------|----------|
| **Bootstrap** | 최상위 로더 (네이티브 코드) | `java.lang.*` 등 핵심 클래스 (`rt.jar`) |
| **Extension (Platform)** | Bootstrap의 자식 | `javax.*`, 확장 라이브러리 (`ext/`) |
| **Application (System)** | Extension의 자식 | 개발자가 작성한 클래스 (`classpath`) |

클래스 로딩 요청이 오면 **부모에게 먼저 위임**하고, 부모가 못 찾으면 자식이 로드한다 (Parent Delegation).

#### 클래스 로딩 3단계

```
Loading → Linking → Initialization
```

**1. Loading** — .class 파일을 읽어 바이너리 데이터를 Method Area에 로드. 해당 클래스의 `Class` 객체를 Heap에 생성.

**2. Linking** — 세 단계로 구성:
- **Verify** — 바이트코드가 JVM 명세에 맞는지 검증 (매직 넘버, 구조 등)
- **Prepare** — static 변수에 **기본값** 할당 (`int → 0`, `Object → null`)
- **Resolve** — 심볼릭 레퍼런스(클래스/메서드 이름)를 실제 메모리 레퍼런스로 교체

**3. Initialization** — static 변수에 **실제 값** 할당 + `static {}` 블록 실행. Prepare에서 `0`이었던 값이 여기서 코드에 정의된 값으로 초기화된다.

```java
public class Example {
    static int count = 10;  // Prepare: count = 0 → Initialization: count = 10
    static {
        System.out.println("클래스 초기화");  // Initialization 단계에서 실행
    }
}
```

### Runtime Data Area (JVM 메모리 구조)

JVM이 프로그램을 실행하면서 사용하는 메모리 영역. **스레드 공유 영역**과 **스레드 독립 영역**으로 나뉜다.

```
┌─────────────────────────────────────────────────────┐
│                   JVM Runtime Data Area              │
├─────────────────────┬───────────────────────────────┤
│   스레드 공유 영역     │        스레드 독립 영역          │
│                     │                               │
│  ┌───────────────┐  │  ┌──────────┐ ┌──────────┐   │
│  │  Method Area   │  │  │JVM Stack │ │JVM Stack │   │
│  │ 클래스 메타데이터│  │  │ Thread-1 │ │ Thread-2 │   │
│  │ static 변수    │  │  └──────────┘ └──────────┘   │
│  │ 상수 풀        │  │  ┌──────────┐ ┌──────────┐   │
│  │ 메서드 코드    │  │  │PC Register│ │PC Register│   │
│  └───────────────┘  │  │ Thread-1 │ │ Thread-2 │   │
│  ┌───────────────┐  │  └──────────┘ └──────────┘   │
│  │     Heap       │  │  ┌──────────┐ ┌──────────┐   │
│  │ 객체 인스턴스   │  │  │  Native  │ │  Native  │   │
│  │ 배열           │  │  │  Method  │ │  Method  │   │
│  │ (GC 대상)      │  │  │  Stack   │ │  Stack   │   │
│  └───────────────┘  │  └──────────┘ └──────────┘   │
└─────────────────────┴───────────────────────────────┘
```

| 영역 | 공유 | 저장 내용 |
|------|------|----------|
| **Method Area** | 공유 | 클래스 메타데이터, static 변수, **Constant Pool**(상수 풀), 메서드 바이트코드 |
| **Heap** | 공유 | `new`로 생성된 객체 인스턴스, 배열. **GC의 주요 대상** |
| **JVM Stack** | 독립 | 스레드별 메서드 호출 시 생성되는 **스택 프레임** (지역변수, 매개변수, 리턴 주소) |
| **PC Register** | 독립 | 현재 실행 중인 바이트코드의 주소 |
| **Native Method Stack** | 독립 | JNI로 호출되는 C/C++ 네이티브 코드용 스택 |

### Execution Engine (실행 엔진)

Method Area에 로드된 바이트코드를 실제로 실행하는 컴포넌트.

```
바이트코드
    ↓
┌─────────────────────────────────────────┐
│           Execution Engine               │
│                                         │
│  ┌─────────────┐   ┌─────────────────┐  │
│  │ Interpreter  │   │  JIT Compiler   │  │
│  │ 한 줄씩 해석  │   │ 핫 코드를 네이티브│  │
│  │ (느리지만     │   │ 코드로 컴파일    │  │
│  │  즉시 실행)   │   │ (빠르지만       │  │
│  │              │   │  컴파일 비용)    │  │
│  └─────────────┘   └─────────────────┘  │
│                                         │
│  ┌─────────────────────────────────────┐ │
│  │     Garbage Collector (GC)          │ │
│  │     Heap 영역의 미참조 객체 수거      │ │
│  └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

- **Interpreter** — 바이트코드를 한 줄씩 읽어 해석·실행. 시작은 빠르지만 반복 실행 시 비효율적.
- **JIT (Just-In-Time) Compiler** — 자주 실행되는 코드(**핫 코드**, Hot Code)를 감지하여 네이티브 머신 코드로 컴파일. 이후 같은 코드는 컴파일된 네이티브 코드를 직접 실행하여 성능 향상.
- **GC (Garbage Collector)** — Heap 영역에서 더 이상 참조되지 않는 객체를 자동 수거. [[GC|GC 동작 원리]] 참고.

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
- [[GC]] — Heap 구조와 GC 알고리즘
- [[2-Areas/backend/06-computer-science/os/프로세스,스레드|프로세스,스레드]] — OS 수준의 프로세스/스레드 개념
- [[2-Areas/backend/06-computer-science/os/스레드-모델|스레드-모델]] — JVM 스레드와 OS 스레드 매핑
- [[2-Areas/backend/06-computer-science/os/가상-메모리|가상-메모리]] — JVM Heap이 올라가는 OS 가상 메모리
