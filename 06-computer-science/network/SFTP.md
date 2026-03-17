---
tags: [computer-science, network, sftp]
status: completed
created: 2026-03-03
---

# SFTP (SSH File Transfer Protocol)

## 핵심 개념

**SSH** 프로토콜 기반의 보안 파일 전송 프로토콜. Port 22를 사용한다.

```
FTP  → 파일 전송 (평문, 보안 취약)
FTPS → FTP + SSL/TLS 암호화
SFTP → SSH 기반 파일 전송 (FTP와 무관한 별개 프로토콜)
```

> [!warning]
> FTPS와 SFTP는 다르다. SFTP는 FTP에 SSL을 얹은 것이 아니라 **SSH 기반의 완전히 다른 프로토콜**이다.

## 동작 원리

### 일반적인 SFTP 배치 처리 흐름 (eBay LMS 예시)

대량 상품 등록 플랫폼(eBay Large Merchant Services)은 SFTP를 통한 배치 처리를 제공한다.

```
판매자 → eBay SFTP 서버에 XML/CSV 업로드
                ↓
        eBay가 파일을 비동기로 처리
                ↓
        결과 파일을 SFTP 서버에 다시 올려둠
판매자 → 결과 파일 다운로드 → 성공/실패 확인
```

### SFTP 특성

| 장점 | 단점 |
|------|------|
| 구현 단순 | **비동기** - 즉시 결과 확인 불가 |
| 대량 배치 처리에 유리 | 실시간 동기화 불가 |
| API 없이도 연동 가능 | 에러 파악이 느림 |
| 보안 (SSH 암호화) | 상태 추적이 어려움 |

### SFTP 방식의 한계

SFTP 기반 연동은 **외부 서비스의 현재 상태를 코드로 알 수 없다**.

- 현재 등록된 상품 목록 확인 → CSV 직접 다운로드 또는 어드민 화면 수동 확인 필요
- 등록/수정/삭제를 프로그래밍적으로 추적하기 어려움
- 상품 수가 증가할수록 수동 관리 비용이 **선형으로 증가**

```
내부 DB 상태  ≠  외부 서비스 실제 상태
      ↑
  이 gap을 메우려면 사람이 수동으로 확인/처리
```

> [!note]
> API 전환을 고려해야 하는 시점: 수동 상태 관리 부담 증가, 상품 수 확장으로 운영 비용 폭증, 가격/재고 변경의 실시간 반영 요구.

### SFTP vs API 연동 비교

| | SFTP | API |
|--|------|-----|
| 적합한 규모 | 소규모, MVP | 중대규모, 자동화 |
| 실시간성 | 배치 처리 | 이벤트 기반 즉시 반영 |
| 상태 추적 | 수동 | 프로그래밍적 추적 가능 |
| 운영 비용 | 상품 수 비례 증가 | 자동화로 고정 비용 |
| 에러 처리 | 결과 파일 다운로드 후 확인 | 응답 코드 즉시 확인 |

## 코드 예시

```java
// JSch 라이브러리를 사용한 SFTP 파일 업로드
import com.jcraft.jsch.*;

public class SftpUploader {
    public void upload(String host, String username, String privateKeyPath,
                       String localFile, String remotePath) throws Exception {
        JSch jsch = new JSch();
        jsch.addIdentity(privateKeyPath);

        Session session = jsch.getSession(username, host, 22);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
        channel.put(localFile, remotePath);

        channel.disconnect();
        session.disconnect();
    }
}
```

### 마이그레이션 시 주의사항

> [!warning]
> SFTP와 API로 등록된 상품은 플랫폼 내부적으로 관리 주체가 다를 수 있다. eBay의 경우 SFTP 등록 상품은 API로 관리 불가하므로, 전환 시 기존 SFTP 등록 상품을 별도 처리(수기 내림 등)해야 한다.

## 관련 문서
- [[CORS]]
- [[2-Areas/backend/06-computer-science/network/CORS|CORS]]
