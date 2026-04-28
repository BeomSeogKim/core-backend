---
tags: [computer-science, network, tcp, udp]
status: completed
created: 2026-04-06
---

# TCP vs UDP

## 핵심 개념

**Transport 계층(4계층)** 의 두 가지 프로토콜. 둘 다 포트 번호를 사용해 프로세스를 식별한다.

| | TCP | UDP |
|---|---|---|
| 연결 방식 | **연결 지향** (3-way handshake) | **비연결** |
| 신뢰성 | 순서 보장 + 재전송 | 보장 없음 (유실 허용) |
| 속도 | 느림 | **빠름** |
| 헤더 크기 | 20byte | 8byte |
| 사용 예 | HTTP, FTP, 채팅, 파일 전송 | DNS, 영상통화, 스트리밍, 온라인 게임 |

## 동작 원리

### TCP 3-way Handshake (연결 수립)

```
Client                    Server
  |                          |
  |-------- SYN ----------→  |   클라이언트: "연결 요청"
  |                          |
  |  ←------ SYN + ACK ----- |   서버: "OK, 나도 연결할게"
  |                          |
  |-------- ACK ----------→  |   클라이언트: "확인"
  |                          |
  ↓ 데이터 전송 시작           ↓
```

- **SYN** (Synchronize): 연결 시작 의사 표현
- **ACK** (Acknowledge): 확인 응답

### TCP 4-way Handshake (연결 종료)

```
Client                    Server
  |-------- FIN ----------→  |   클라이언트: "나 끊을게"
  |  ←------ ACK ---------   |   서버: "OK 확인"
  |  ←------ FIN ---------   |   서버: "나도 끊을게"
  |-------- ACK ----------→  |   클라이언트: "OK 확인"
```

> [!note]
> **TIME_WAIT** — 클라이언트는 마지막 ACK 전송 후 일정 시간 대기한다. 서버의 FIN이 재전송될 경우를 대비해 연결 상태를 유지한다.

### TCP가 느린 이유

신뢰성 보장을 위해 추가 동작이 많다:

1. **연결 수립** — 데이터 전송 전 3-way handshake 비용
2. **순서 보장** — Sequence Number를 헤더에 포함, 수신 측에서 재조립
3. **재전송** — ACK를 받지 못한 세그먼트 재전송
4. **흐름 제어** — 수신자 버퍼가 넘치지 않도록 전송 속도 조절 (Window Size)
5. **혼잡 제어** — 네트워크 혼잡 감지 시 전송 속도 감소 (Slow Start)

### UDP 사용 시나리오

유실보다 **실시간성**이 중요하거나, 애플리케이션 레벨에서 재전송을 제어할 때 사용한다:

- 영상통화/스트리밍: 프레임 일부 유실보다 끊김 없는 재생이 중요
- DNS: 단순 요청/응답, 실패 시 애플리케이션이 재시도
- 온라인 게임: 위치 정보 등 최신 값만 의미 있음

## 코드 예시

```java
// Java TCP 소켓 서버
ServerSocket serverSocket = new ServerSocket(8080);
Socket socket = serverSocket.accept();          // 연결 대기 (블로킹)
InputStream in = socket.getInputStream();       // 데이터 수신

// Java UDP 소켓
DatagramSocket udpSocket = new DatagramSocket(9090);
byte[] buffer = new byte[1024];
DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
udpSocket.receive(packet);                      // 연결 없이 수신
```

## 관련 문서

- [[dev/06-computer-science/network/OSI-7-Layer]]
- [[dev/06-computer-science/network/HTTP]]
- [[dev/06-computer-science/os/프로세스,스레드]]
