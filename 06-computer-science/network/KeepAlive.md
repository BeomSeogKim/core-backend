---
tags: [computer-science, network, http, keepalive]
status: completed
created: 2026-04-06
---

# KeepAlive

## 핵심 개념

**HTTP KeepAlive** — 하나의 TCP 연결을 여러 HTTP 요청/응답에 **재사용**하는 기능. Persistent Connection이라고도 한다.

HTTP/1.0은 요청마다 TCP 연결을 새로 맺고 끊었다. HTTP/1.1부터 **기본값이 KeepAlive on**이다.

## 동작 원리

### KeepAlive 없음 (HTTP/1.0 기본)

```
요청1: TCP 연결(3-way) → HTTP 요청 → HTTP 응답 → TCP 종료(4-way)
요청2: TCP 연결(3-way) → HTTP 요청 → HTTP 응답 → TCP 종료(4-way)
요청3: TCP 연결(3-way) → HTTP 요청 → HTTP 응답 → TCP 종료(4-way)

→ 매 요청마다 핸드셰이크 비용 발생
```

### KeepAlive 있음 (HTTP/1.1 기본)

```
TCP 연결(3-way) ─────────────────────────────────────────→
  요청1 → 응답1
  요청2 → 응답2
  요청3 → 응답3
                           (타임아웃 후) TCP 종료(4-way)

→ 핸드셰이크 1번으로 여러 요청 처리
```

### HTTP 헤더

```http
Connection: keep-alive          -- 연결 유지 요청
Keep-Alive: timeout=60, max=100 -- 60초 유지, 최대 100개 요청
Connection: close               -- 응답 후 연결 종료
```

### KeepAlive vs HTTP/2 멀티플렉싱

| | KeepAlive | HTTP/2 멀티플렉싱 |
|---|---|---|
| 연결 재사용 | O | O |
| 동시 요청 | X (순차) | **O (병렬)** |
| HOL Blocking | **있음** | 없음 |

KeepAlive는 연결을 재사용하지만 여전히 요청을 순차 처리한다. HTTP/2 멀티플렉싱이 더 근본적인 해결책이다.

### 서버 설정 고려사항

KeepAlive 타임아웃 동안 연결을 유지하므로 **서버 리소스(소켓, 파일 디스크립터)를 점유**한다. 대규모 트래픽 환경에서는 타임아웃 값을 적절히 설정해야 한다.

- Nginx 기본: `keepalive_timeout 65`
- Tomcat 기본: `connectionTimeout 20000ms`

## 관련 문서

- [[dev/06-computer-science/network/HTTP]]
- [[dev/06-computer-science/network/TCP-UDP]]
- [[dev/06-computer-science/network/OSI-7-Layer]]
