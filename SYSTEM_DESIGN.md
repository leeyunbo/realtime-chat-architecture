# Realtime Chat - System Design Document

## 1. Functional Requirements

### Phase 1 (Core)
- 1:1 채팅 (텍스트)
- 온라인/오프라인 상태
- 읽음/안읽음 표시

### Phase 2 (확장)
- 그룹 채팅
- 메시지 수정/삭제
- 채팅방 퇴장

### Phase 3 (미디어)
- 이미지/파일 전송 (3MB 제한, 이미지/PDF/문서)
- 비동기 썸네일 생성
- S3 저장 + Presigned URL 다운로드

---

## 2. Non-Functional Requirements

| 항목 | 목표 | 근거 |
|------|------|------|
| 동시 접속자 수 | 1,000명 | 단일 서버 한계 경계선, 분산 설계 학습에 적합 |
| 메시지 전달 지연 | 500ms 이내 | 채팅 앱 사용자 기대치 |
| 메시지 유실 | 불허 | DB 저장 후 전달 방식으로 보장 |
| 메시지 보관 | 1개월 | TTL 기반 자동 삭제 |
| CAP 선택 | AP (가용성 우선) | 서비스 중단보다 Eventually Consistent 허용 |

---

## 3. Core Entities

### User
| 필드 | 타입 | 설명 |
|------|------|------|
| id | BIGINT (PK) | 식별자 |
| username | VARCHAR | 아이디 |
| password | VARCHAR | 비밀번호 |
| created_at | TIMESTAMP | 생성일 |
| updated_at | TIMESTAMP | 수정일 |

### ChatRoom
| 필드 | 타입 | 설명 |
|------|------|------|
| id | BIGINT (PK) | 식별자 |
| type | VARCHAR | DIRECT / GROUP |
| created_at | TIMESTAMP | 생성일 |
| updated_at | TIMESTAMP | 수정일 |

### ChatRoomUser
| 필드 | 타입 | 설명 |
|------|------|------|
| chatroom_id | BIGINT (FK) | 채팅방 ID |
| user_id | BIGINT (FK) | 사용자 ID |
| status | VARCHAR | 상태 (ACTIVE / LEFT) |
| joined_at | TIMESTAMP | 입장 시점 (이 시점 이후 메시지만 조회) **(Phase 2)** |
| last_read_message_id | BIGINT | 마지막 읽은 메시지 ID |
| created_at | TIMESTAMP | 생성일 |
| updated_at | TIMESTAMP | 수정일 |

### Message
| 필드 | 타입 | 설명 |
|------|------|------|
| id | BIGINT (PK) | 식별자 |
| chatroom_id | BIGINT (FK) | 채팅방 ID |
| sender_id | BIGINT (FK) | 발신자 ID (SYSTEM 타입이면 null) |
| type | VARCHAR | CHAT / SYSTEM **(Phase 2)** / FILE **(Phase 3)** |
| content | TEXT | 메시지 내용 |
| file_id | BIGINT (FK, nullable) | FileAttachment 참조 **(Phase 3)** |
| unread_count | INT | 안 읽은 사람 수 |
| edited | BOOLEAN | 수정 여부 **(Phase 2)** |
| deleted | BOOLEAN | 삭제 여부 (soft delete, content 보존) **(Phase 2)** |
| created_at | TIMESTAMP | 생성일 |
| updated_at | TIMESTAMP | 수정일 |

### FileAttachment **(Phase 3)**
| 필드 | 타입 | 설명 |
|------|------|------|
| id | BIGINT (PK) | fileId |
| uploader_id | BIGINT (FK) | 업로드한 유저 |
| original_filename | VARCHAR | 원본 파일명 (report.pdf) |
| stored_path | VARCHAR | S3 경로 (files/{id}/original.pdf) |
| thumbnail_path | VARCHAR (nullable) | 썸네일 S3 경로 (이미지만) |
| content_type | VARCHAR | MIME 타입 (image/jpeg, application/pdf) |
| file_size | BIGINT | 바이트 단위 크기 |
| thumbnail_status | VARCHAR | PENDING / COMPLETED / FAILED / NONE |
| created_at | TIMESTAMP | 업로드 시각 |

---

## 4. API Design

### REST API

| Method | Path | 설명 |
|--------|------|------|
| POST | /users/register | 회원가입 |
| POST | /users/login | 로그인 |
| GET | /chatrooms | 내 채팅방 목록 조회 |
| POST | /chatrooms | 채팅방 생성 (userIds 2명=DIRECT, 3명+=GROUP) |
| GET | /chatrooms/{roomId}/messages | 과거 메시지 조회 (joined_at 이후만) |
| GET | /friends | 친구 목록 조회 |
| POST | /friends | 친구 추가 |
| POST | /files | 파일 업로드 (multipart, 3MB 제한) **(Phase 3)** |
| GET | /files/{fileId}/url | Presigned URL 발급 (원본 + 썸네일) **(Phase 3)** |

### WebSocket Events

**클라이언트 → 서버**
| 이벤트 | 필드 | 설명 |
|--------|------|------|
| message.send | chatRoomId, content, fileId(optional) | 메시지 전송 (파일 메시지 시 fileId 포함) |
| message.read | chatRoomId | 메시지 읽음 처리 |
| message.update | messageId, content | 메시지 수정 (본인만) **(Phase 2)** |
| message.delete | messageId | 메시지 삭제 (본인만) **(Phase 2)** |
| room.leave | chatRoomId | 채팅방 퇴장 **(Phase 2)** |
| room.invite | chatRoomId, userIds | 멤버 초대 (친구만) **(Phase 2)** |
| heartbeat | (없음) | 연결 유지 |

**서버 → 클라이언트**
| 이벤트 | 필드 | 설명 |
|--------|------|------|
| message.received | chatRoomId, senderId, senderName, content, messageId, unreadCount, type, fileId, originalFilename, contentType, fileSize | 메시지 수신 (CHAT/SYSTEM/FILE) |
| message.updated | chatRoomId, messageId, content, edited, deleted, unreadCount | 메시지 상태 변경 (수정/삭제/읽음) |
| messages.read | chatRoomId, senderId, messageId | 일괄 읽음 처리 알림 |
| user.status | senderId, senderName, online | 온라인/오프라인 상태 |

**연결/해제**
| 이벤트 | 설명 |
|--------|------|
| connect | WebSocket 연결 → 온라인 상태 전환 |
| disconnect | WebSocket 해제 → 오프라인 상태 전환 |

---

## 5. High-Level Architecture

```
Client (React)
    │
    ▼
Load Balancer
    │
    ├──────────────────┐
    ▼                  ▼
REST API Server    WebSocket Server (N대)
    │                  │
    ▼                  ▼
PostgreSQL         Redis
                   ├── 온라인 상태 (Key-Value + TTL)
                   ├── 유저-서버 매핑 (user:B → server:2)
                   └── 서버 간 메시지 중계 (Pub/Sub, 서버당 1채널)

S3 (MinIO)  ◄── 파일 원본 + 썸네일 저장 (Phase 3)
    │
    └── Presigned URL로 클라이언트 직접 다운로드
```

---

## 6. Message Flow

### B가 온라인인 경우
```
1. A → WebSocket 서버1: 메시지 전송
2. 서버1 → DB: 메시지 저장 (status: unread)
3. 서버1 → Redis: B가 어느 서버에 있는지 조회 (user:B → server:2)
4. 서버1 → Redis Pub/Sub: server:2 채널에 publish
5. 서버2 → Redis Pub/Sub: subscribe로 메시지 수신
6. 서버2 → B: WebSocket으로 메시지 전달
7. B → 서버2: 읽음 확인 → DB 업데이트 (unreadCount - 1)
```

### B가 오프라인인 경우
```
1. A → WebSocket 서버1: 메시지 전송
2. 서버1 → DB: 메시지 저장 (status: unread)
3. 서버1 → Redis: B 매핑 조회 → 없음 (오프라인)
4. 끝. DB에 저장되어 있으므로 유실 없음

... B가 나중에 접속 ...

5. B → WebSocket 서버3: 연결
6. 서버3 → Redis: user:B → server:3 매핑 저장
7. 서버3 → DB: B의 안 읽은 메시지 조회
8. 서버3 → B: 밀린 메시지 일괄 전달
```

### 메시지 수정 (Phase 2)
```
1. A → WebSocket: message.update (messageId, newContent)
2. 서버: 본인 메시지 검증 + deleted 아닌지 검증
3. 서버 → DB: content 업데이트, edited = true
4. 서버 → 채팅방 멤버: message.updated 브로드캐스트
```

### 메시지 삭제 (Phase 2)
```
1. A → WebSocket: message.delete (messageId)
2. 서버: 본인 메시지 검증
3. 서버 → DB: deleted = true (content 보존)
4. 서버 → 채팅방 멤버: message.updated 브로드캐스트
```

### 채팅방 퇴장 (Phase 2)
```
1. A → WebSocket: room.leave (chatRoomId)
2. 서버 → DB: ChatRoomUser.status = LEFT
3. 서버 → DB: 시스템 메시지 저장 ("A님이 퇴장하셨습니다", type=SYSTEM)
4. 서버 → 채팅방 멤버: message.received 브로드캐스트 (시스템 메시지)
```

### 멤버 초대 (Phase 2)
```
1. A → WebSocket: room.invite (chatRoomId, userIds)
2. 서버: 친구 관계 검증
3. 서버 → DB: ChatRoomUser 생성 또는 status=ACTIVE, joined_at 갱신
4. 서버 → DB: 시스템 메시지 저장 ("B님이 입장하셨습니다", type=SYSTEM)
5. 서버 → 채팅방 멤버: message.received 브로드캐스트 (시스템 메시지)
```

### 파일 업로드 + 전송 (Phase 3)
```
1. A → REST POST /files: 파일 업로드 (multipart, 3MB 제한)
2. 서버: 파일 검증 (크기, MIME 타입)
3. 서버 → S3: 원본 저장 (files/{fileId}/original.xxx)
4. 서버 → DB: FileAttachment 저장 (thumbnailStatus=PENDING or NONE)
5. 서버: FileUploadedEvent 발행
6. 서버 → A: fileId 반환

[비동기 — @Async + @EventListener]
7. ThumbnailEventListener: 이미지면 썸네일 생성 → S3 저장 (files/{fileId}/thumbnail.jpg)
8. DB: thumbnailStatus = COMPLETED (또는 FAILED)

[메시지 전송 — 기존 WebSocket 흐름]
9. A → WebSocket: message.send(chatRoomId, fileId)
10. 서버: FileAttachment 조회 + 업로더 본인 검증
11. 서버 → DB: Message 저장 (type=FILE, file_id)
12. 서버 → 채팅방 멤버: message.received 브로드캐스트 (파일 메타데이터 포함)

[다운로드 — Presigned URL]
13. 수신자 → REST GET /files/{fileId}/url: Presigned URL 요청
14. 서버 → S3: Presigned URL 생성 (만료: 5분)
15. 서버 → 수신자: { originalUrl, thumbnailUrl, expiresIn }
16. 수신자 → S3: Presigned URL로 직접 다운로드
```

---

## 7. Tech Stack

| 구분 | 선택 |
|------|------|
| 백엔드 | Java 17+ / Spring Boot 3 |
| 프론트엔드 | React |
| 데이터베이스 | PostgreSQL |
| 캐시 / Pub/Sub | Redis |
| 파일 저장소 | S3 (로컬: MinIO) **(Phase 3)** |
| 배포 | Docker Compose (로컬) |

---

## 8. Implementation Phases

```
Phase 1 (완료)
  Step 1: 프로젝트 셋업 (Spring Boot + Docker Compose)
  Step 2: DB 스키마 생성
  Step 3: REST API (회원가입, 로그인, 채팅방 생성/조회)
  Step 4: WebSocket 연결 (1:1 메시지 전송 - 단일 서버)
  Step 5: Redis 연동 (온라인 상태 + Pub/Sub)
  Step 6: 읽음/안읽음 처리
  Step 7: 프론트 (React 채팅 UI)

Phase 2 (완료)
  Step 8: 그룹 채팅 (GROUP 채팅방 생성, 멤버 초대)
  Step 9: 메시지 수정/삭제
  Step 10: 채팅방 퇴장 + 시스템 메시지

Phase 3
  Step 11: MinIO + S3 연동 (Docker Compose, S3Client 설정)
  Step 12: 파일 업로드 API (FileAttachment 엔티티, POST /files)
  Step 13: 비동기 썸네일 생성 (ApplicationEvent + @Async)
  Step 14: 파일 메시지 전송 (WebSocket message.send + fileId)
  Step 15: Presigned URL 다운로드 (GET /files/{fileId}/url)
  Step 16: 프론트엔드 파일 UI (첨부, 미리보기, 다운로드)
```

---

## 부록: 설계 결정 로그

| # | 결정 | 선택지 | 선택 | 근거 |
|---|------|--------|------|------|
| 1 | DB 선택 | PostgreSQL vs MongoDB | PostgreSQL | 관계형 데이터 구조, 트랜잭션 필요 |
| 2 | 시스템 메시지 저장 | Message 테이블 통합 vs 별도 테이블 | Message 테이블 통합 (type 컬럼) | 항상 같은 타임라인에 조회되므로 UNION 비용 불필요 |
| 3 | 삭제 시 content 처리 | soft delete (content 보존) vs content 제거 | soft delete (content 보존) | 관리자 조회/신고 기능 확장 가능성 |
| 4 | 퇴장/초대 통신 방식 | REST vs WebSocket | WebSocket | 채팅방 안에서 발생하는 실시간 이벤트는 WebSocket으로 통일 |
| 5 | 재입장 방식 | 본인 재입장 vs 멤버 초대 vs 불가 | 기존 멤버가 재초대 | 방장 없는 구조에서 자연스러운 방식 |
| 6 | 재입장 시 메시지 범위 | 전체 조회 vs 입장 시점 이후만 | joined_at 이후만 조회 | 퇴장 시 과거 메시지 접근 차단 요구사항 |
| 7 | 그룹 초대 권한 | 누구나 vs 친구만 | 친구 관계인 경우만 | Phase 1과 동일한 제약 유지 |
| 8 | 파일 업로드 채널 | REST vs WebSocket | REST API | WebSocket에 바이너리 넣으면 메시지 채널 블로킹 |
| 9 | 업로드-메시지 관계 | 통합 (한 API) vs 분리 (업로드 → fileId → 메시지) | 분리 | 단계별 독립 실패 처리, 썸네일 비동기 가능 |
| 10 | 썸네일 알림 방식 | Pull vs Push vs 규칙 기반 URL | 규칙 기반 URL ({fileId}/thumbnail.jpg) | 가장 단순, 3MB 제한이면 생성 빠름 |
| 11 | 파일 메타데이터 저장 | Message 테이블 통합 vs 별도 테이블 | 별도 테이블 (FileAttachment) | 대부분 텍스트 메시지라 공간 낭비 방지 + 독립 조회 가능 |
| 12 | 비동기 처리 방식 | @Async vs ApplicationEvent + @Async vs 메시지 큐 | ApplicationEvent + @Async | 디커플링 + 비동기, 현재 규모에 적합 |
| 13 | 다운로드 방식 | 서버 프록시 vs S3 Presigned URL | Presigned URL | 서버 대역폭 절약, S3 직접 전달 |
| 14 | 파일 접근 제어 | 채팅방 멤버 체크 vs 업로더 체크 | 업로더 본인 체크 | 최소한의 보안, 같은 파일 여러 방 공유 허용 |
| 15 | 로컬 S3 환경 | 실제 AWS vs MinIO | MinIO (Docker Compose) | S3 API 호환, 코드 변경 없이 전환 가능 |
