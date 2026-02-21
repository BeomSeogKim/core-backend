# JVM

## JVM의 탄생 배경 및 설계 철학

### 크로스 플랫폼 문제의 본질

#### 플랫폼 간 차이점
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

#### 전통적 해결 방법의 한계
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

Write Once, Run Anywhere
- 바이트코드: 플랫폼에 독립적인 중간 형태
- JVM: 플랫폼별로 구현된 실행 엔진

구현 원리

JVM은 플랫폼별로 다르게 구현됨