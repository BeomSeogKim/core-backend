---
tags: [computer-science, network, osi]
status: completed
created: 2026-04-06
---

# OSI 7 Layer

## 핵심 개념

네트워크 통신을 **7개의 계층으로 분리**한 표준 참조 모델. 각 계층은 자신의 역할만 담당하고 인접 계층에 위임하는 방식으로 **관심사 분리**를 실현한다.

| 계층 | 이름 | 주요 프로토콜/장비 | 역할 |
|------|------|-----------------|------|
| 7 | **Application** | HTTP, FTP, DNS, SMTP | 사용자/앱이 직접 사용하는 프로토콜 |
| 6 | **Presentation** | SSL/TLS, JPEG, MPEG | 암호화, 압축, 인코딩 변환 |
| 5 | **Session** | — | 세션 수립/유지/종료 |
| 4 | **Transport** | TCP, UDP | 포트 번호, 신뢰성/흐름 제어 |
| 3 | **Network** | IP, ICMP, 라우터 | IP 주소, 라우팅 |
| 2 | **Data Link** | MAC, 이더넷, 스위치 | 같은 네트워크 내 프레임 전송 |
| 1 | **Physical** | 케이블, 허브 | 비트를 전기/광 신호로 변환 |

> [!note]
> **TCP/IP 4계층**은 OSI를 실용적으로 압축한 모델이다.
> Application(5+6+7) / Transport(4) / Internet(3) / Network Access(1+2)

## 동작 원리

### 캡슐화 (Encapsulation)

송신 측에서 데이터가 상위 계층에서 하위 계층으로 내려가면서 각 계층의 헤더가 추가된다.

```
7 Application  [HTTP 데이터              ]
               ↓ TCP 헤더(포트 번호) 추가
4 Transport    [TCP Header | HTTP        ]  → Segment
               ↓ IP 헤더(IP 주소) 추가
3 Network      [IP Header | TCP | HTTP   ]  → Packet
               ↓ MAC 헤더 추가
2 Data Link    [MAC Header | IP | TCP | HTTP | FCS ]  → Frame
               ↓ 전기신호로 변환
1 Physical     ~~~~~~~~~ Bit 전송 ~~~~~~~~~
```

### 역캡슐화 (Decapsulation)

수신 측에서 하위 계층에서 상위 계층으로 올라가면서 각 계층의 헤더를 제거하고 상위 계층으로 전달한다.

### PDU (Protocol Data Unit)

각 계층에서 데이터 단위를 부르는 명칭:

| 계층 | PDU 이름 |
|------|---------|
| 4 Transport | **Segment** |
| 3 Network | **Packet** |
| 2 Data Link | **Frame** |
| 1 Physical | **Bit** |

## 관련 문서

- [[dev/06-computer-science/network/TCP-UDP]]
- [[dev/06-computer-science/network/HTTP]]
- [[dev/06-computer-science/network/CORS]]
