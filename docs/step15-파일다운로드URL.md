# Step 15: 파일 다운로드 URL 발급 (Presigned URL)

## Context
Step 11~14에서 파일 업로드 → 썸네일 생성 → 파일 메시지 전송까지 구현했다.
클라이언트가 파일을 실제로 열거나 다운로드할 수단이 없으므로, S3 Presigned URL을 발급하는 API를 추가한다.

## 설계 결정 로그

| # | 결정 | 선택지 | 선택 | 근거 |
|---|------|--------|------|------|
| 1 | 파일 전달 방식 | 서버 프록시 vs Presigned URL | **Presigned URL** | 3MB 제한 채팅 서비스에서 서버가 바이트를 중계할 이유 없음. S3가 직접 서빙하여 서버 부하 제거 |
| 2 | 접근 제어 | 로그인만 확인 vs 채팅방 멤버 검증 | **채팅방 멤버 검증** | fileId가 순차 증가(IDENTITY)라 추측 가능. 로그인만 확인하면 비멤버가 남의 파일에 접근 가능 |
| 3 | 멤버 검증 경로 | FileAttachment → Message 역추적 | **MessageRepository 쿼리** | FileAttachment에 chatRoom 역참조 없으므로, Message 테이블에서 file_id로 chatRoom을 조회 |
| 4 | Presigned URL TTL | 5분 / 15분 / 1시간 | **5분** | 클릭 즉시 다운로드/뷰 용도. 짧을수록 유출 시 피해 최소화 |
| 5 | 배치 API 메서드 | GET + 쿼리 파라미터 vs POST + body | **GET + 쿼리 파라미터** | 조회 행위이므로 GET이 RESTful. 한 화면 파일 10~20개 수준이라 URL 길이 제한 문제 없음 |
| 6 | 단건/배치 API | 배치만 vs 둘 다 | **둘 다** | 파일 클릭 다운로드(단건 GET)와 채팅방 진입 시 썸네일 로딩(배치 GET)은 용도가 다름 |
| 7 | 배치 부분 접근 불가 | 가능한 것만 반환 vs 전체 거부 | **전체 거부 (403)** | 정상 클라이언트는 접근 불가 fileId를 요청할 리 없음. 부분 허용은 비정상 요청을 용인하는 것 |

## 핵심 흐름

### 단건 — 파일 클릭 시
```
클라이언트                         서버                              S3
   │                              │                               │
   │  GET /files/{id}/download-url │                               │
   │ ─────────────────────────────>│                               │
   │                              │  1. FileAttachment 조회         │
   │                              │  2. Message에서 chatRoomId 조회  │
   │                              │  3. ChatRoomUser 멤버 검증       │
   │                              │  4. S3Presigner로 URL 생성      │
   │  { downloadUrl, metadata }   │                               │
   │ <─────────────────────────────│                               │
   │                                                              │
   │  GET presigned-url (직접 접근)                                  │
   │ ─────────────────────────────────────────────────────────────>│
   │  파일 바이트                                                    │
   │ <─────────────────────────────────────────────────────────────│
```

### 배치 — 채팅방 진입 시
```
클라이언트                         서버                              S3
   │                              │                               │
   │  GET /files/download-urls     │                               │
   │    ?fileIds=1,2,3             │                               │
   │ ─────────────────────────────>│                               │
   │                              │  1. FileAttachment IN 조회      │
   │                              │  2. chatRoomIds 배치 조회        │
   │                              │  3. 멤버 검증 (배치)             │
   │                              │  4. Presigned URL 배치 생성      │
   │  [ { downloadUrl, thumb... }] │                               │
   │ <─────────────────────────────│                               │
```

## API 설계

### 1. 원본 파일 다운로드 URL (단건)
```
GET /files/{fileId}/download-url
Authorization: Bearer {token}

Response 200:
{
  "downloadUrl": "https://s3.../files/1/original.jpg?X-Amz-...",
  "originalFilename": "photo.jpg",
  "contentType": "image/jpeg",
  "fileSize": 2048
}
```

### 2. 썸네일 다운로드 URL (단건)
```
GET /files/{fileId}/thumbnail-url
Authorization: Bearer {token}

Response 200:
{
  "downloadUrl": "https://s3.../files/1/thumbnail.jpg?X-Amz-...",
  "originalFilename": "photo.jpg",
  "contentType": "image/jpeg",
  "fileSize": 2048
}

Response 404: 썸네일 없음 (이미지가 아닌 파일 또는 생성 실패)
```

### 3. 배치 다운로드 URL (원본 + 썸네일)
```
GET /files/download-urls?fileIds=1,2,3
Authorization: Bearer {token}

Response 200:
[
  {
    "fileId": 1,
    "downloadUrl": "https://s3.../files/1/original.jpg?X-Amz-...",
    "thumbnailUrl": "https://s3.../files/1/thumbnail.jpg?X-Amz-...",
    "originalFilename": "photo.jpg",
    "contentType": "image/jpeg",
    "fileSize": 2048
  },
  {
    "fileId": 2,
    "downloadUrl": "https://s3.../files/2/original.pdf?X-Amz-...",
    "thumbnailUrl": null,
    "originalFilename": "report.pdf",
    "contentType": "application/pdf",
    "fileSize": 102400
  }
]
```

## 변경 사항

### 1. FileDownloadResponse DTO (단건용)
**신규**: `src/main/java/com/bok/chat/api/dto/FileDownloadResponse.java`
```java
public record FileDownloadResponse(
    String downloadUrl,
    String originalFilename,
    String contentType,
    long fileSize
) {}
```

### 2. FileBatchDownloadResponse DTO (배치용)
**신규**: `src/main/java/com/bok/chat/api/dto/FileBatchDownloadResponse.java`
```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FileBatchDownloadResponse(
    Long fileId,
    String downloadUrl,
    String thumbnailUrl,
    String originalFilename,
    String contentType,
    long fileSize
) {}
```

### 3. MessageRepository — chatRoomId 역추적 쿼리
**수정**: `src/main/java/com/bok/chat/repository/MessageRepository.java`
- 단건: `findChatRoomIdByFileId(Long fileId)`
  - `SELECT m.chatRoom.id FROM Message m WHERE m.file.id = :fileId`
- 배치: `findChatRoomIdsByFileIds(List<Long> fileIds)`
  - `SELECT m.file.id, m.chatRoom.id FROM Message m WHERE m.file.id IN :fileIds`

### 4. FileStorageService — Presigned URL 생성
**수정**: `src/main/java/com/bok/chat/api/service/FileStorageService.java`
- `S3Presigner` 의존성 추가
- `generatePresignedUrl(String key)` 메서드 추가 — 5분 TTL

### 5. FileDownloadService — 접근 제어 + URL 발급 오케스트레이션
**신규**: `src/main/java/com/bok/chat/api/service/FileDownloadService.java`
- `getDownloadUrl(Long userId, Long fileId)` — 단건 원본
- `getThumbnailUrl(Long userId, Long fileId)` — 단건 썸네일
- `getBatchDownloadUrls(Long userId, List<Long> fileIds)` — 배치 (원본 + 썸네일)
- 접근 제어: 파일 조회 → chatRoomId 역추적 → 멤버 검증

### 6. FileController — 엔드포인트 추가
**수정**: `src/main/java/com/bok/chat/api/controller/FileController.java`
- `GET /files/{fileId}/download-url` — 단건 원본
- `GET /files/{fileId}/thumbnail-url` — 단건 썸네일
- `GET /files/download-urls?fileIds=1,2,3` — 배치

### 7. 테스트
- **FileDownloadServiceTest**: 단건 성공 / 파일 미존재 / 비멤버 접근 차단 / 썸네일 없음 / 배치 성공 / 배치 부분 접근 차단

## 파일 목록

| 파일 | 작업 |
|------|------|
| `src/main/java/com/bok/chat/api/dto/FileDownloadResponse.java` | 신규 |
| `src/main/java/com/bok/chat/api/dto/FileBatchDownloadResponse.java` | 신규 |
| `src/main/java/com/bok/chat/api/service/FileDownloadService.java` | 신규 |
| `src/main/java/com/bok/chat/api/service/FileStorageService.java` | 수정 — presigned URL |
| `src/main/java/com/bok/chat/api/controller/FileController.java` | 수정 — 엔드포인트 추가 |
| `src/main/java/com/bok/chat/repository/MessageRepository.java` | 수정 — 역추적 쿼리 |
| `src/test/java/com/bok/chat/api/service/FileDownloadServiceTest.java` | 신규 |

## 검증
1. `./gradlew test` — 전체 테스트 통과
