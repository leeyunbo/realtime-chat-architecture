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
- 이미지/파일 전송
- 이모티콘

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
| created_at | TIMESTAMP | 생성일 |
| updated_at | TIMESTAMP | 수정일 |

### Message
| 필드 | 타입 | 설명 |
|------|------|------|
| id | BIGINT (PK) | 식별자 |
| chatroom_id | BIGINT (FK) | 채팅방 ID |
| sender_id | BIGINT (FK) | 발신자 ID |
| content | TEXT | 메시지 내용 |
| unread_count | INT | 안 읽은 사람 수 |
| created_at | TIMESTAMP | 생성일 |
| updated_at | TIMESTAMP | 수정일 |

---

## 4. API Design

### REST API

| Method | Path | 설명 |
|--------|------|------|
| POST | /users/register | 회원가입 |
| POST | /users/login | 로그인 |
| GET | /chatrooms | 내 채팅방 목록 조회 |
| POST | /chatrooms | 채팅방 생성 |
| GET | /chatrooms/{roomId}/messages | 과거 메시지 조회 |

### WebSocket Events

**클라이언트 → 서버**
| 이벤트 | 설명 |
|--------|------|
| message.send | 메시지 전송 |
| message.read | 메시지 읽음 처리 |
| message.update | 메시지 수정 (Phase 2) |
| message.delete | 메시지 삭제 (Phase 2) |

**서버 → 클라이언트**
| 이벤트 | 설명 |
|--------|------|
| chatroom.created | 새 채팅방 생성 알림 |
| message.received | 메시지 수신 |
| message.updated | 메시지 상태 변경 알림 (읽음/수정/삭제) |
| user.status | 친구 온라인/오프라인 상태 알림 |

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

---

## 7. Tech Stack

| 구분 | 선택 |
|------|------|
| 백엔드 | Java 17+ / Spring Boot 3 |
| 프론트엔드 | React |
| 데이터베이스 | PostgreSQL |
| 캐시 / Pub/Sub | Redis |
| 배포 | Docker Compose (로컬) |

---

## 8. Implementation Phases

```
Step 1: 프로젝트 셋업 (Spring Boot + Docker Compose)
Step 2: DB 스키마 생성
Step 3: REST API (회원가입, 로그인, 채팅방 생성/조회)
Step 4: WebSocket 연결 (1:1 메시지 전송 - 단일 서버)
Step 5: Redis 연동 (온라인 상태 + Pub/Sub)
Step 6: 읽음/안읽음 처리
Step 7: 프론트 (React 채팅 UI)
```
