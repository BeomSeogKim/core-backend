---
tags: [computer-science, network, http]
status: completed
created: 2026-04-06
---

# HTTP

## 핵심 개념

**HyperText Transfer Protocol** — 클라이언트-서버 간 데이터를 주고받는 Application 계층(7계층) 프로토콜. **Stateless** 기반이며 요청/응답 구조로 동작한다.

## 동작 원리

### HTTP/1.1

```
연결1: 요청1 → 응답1 → 요청2 → 응답2 → ...  (순차 처리)
연결2: 요청3 → 응답3 → ...
연결3: 요청4 → 응답4 → ...
(브라우저가 도메인당 최대 6개 병렬 연결로 우회)
```

**HOL Blocking (Head-of-Line Blocking)** — 앞 요청이 막히면 뒤 요청 전부 대기한다. 하나의 TCP 연결에서 요청을 순서대로 처리하기 때문에 발생한다.

**특징:**
- 텍스트 기반 프로토콜
- KeepAlive 기본 활성화 (TCP 연결 재사용)
- 헤더 중복 전송 (매 요청마다 전체 헤더 포함)

### HTTP/2

```
하나의 TCP 연결:
  스트림1: 요청1 ──→  응답1
  스트림2: 요청2 ──→  응답2   (동시 처리 — 멀티플렉싱)
  스트림3: 요청3 ──→  응답3
```

**멀티플렉싱 (Multiplexing)** — 하나의 TCP 연결에서 여러 요청/응답을 동시에 처리. HOL Blocking 해결.

**주요 개선점:**

| | HTTP/1.1 | HTTP/2 |
|---|---|---|
| 전송 형식 | 텍스트 | **바이너리 프레임** |
| 동시 처리 | 순차 (HOL Blocking) | **멀티플렉싱** |
| 헤더 | 매 요청 전체 | **HPACK 압축** (중복 제거) |
| 서버 푸시 | 없음 | 클라이언트 요청 전에 리소스 선제 전송 |

### HTTP/3

TCP의 HOL Blocking까지 근본적으로 해결한다. TCP 대신 **QUIC** 프로토콜(UDP 기반)을 사용한다.

```
HTTP/2:  TCP HOL Blocking 발생 가능 (패킷 유실 시 모든 스트림 대기)
HTTP/3:  QUIC — 스트림별 독립 전송, 하나의 패킷 유실이 다른 스트림에 영향 없음
```

**QUIC 특징:**
- UDP 기반이지만 신뢰성/순서 보장을 애플리케이션 레벨에서 구현
- 연결 수립 **1-RTT** (TCP + TLS는 2~3 RTT)
- 연결 ID 기반으로 IP 변경(Wi-Fi → LTE) 시에도 연결 유지

### HTTPS

HTTP + TLS(Transport Layer Security). 6계층(Presentation)에서 암호화를 추가한다.

```
HTTP:   평문 전송
HTTPS:  TLS Handshake → 대칭키 교환 → 암호화된 데이터 전송
```

## 코드 예시

```java
// Spring RestTemplate — HTTP 요청
RestTemplate restTemplate = new RestTemplate();
String response = restTemplate.getForObject("https://api.example.com/users", String.class);

// WebClient (비동기, HTTP/2 지원)
WebClient client = WebClient.create("https://api.example.com");
Mono<String> response = client.get()
    .uri("/users")
    .retrieve()
    .bodyToMono(String.class);
```

## 관련 문서

- [[2-Areas/backend/06-computer-science/network/TCP-UDP]]
- [[2-Areas/backend/06-computer-science/network/OSI-7-Layer]]
- [[2-Areas/backend/06-computer-science/network/REST-API]]
- [[2-Areas/backend/06-computer-science/network/KeepAlive]]
