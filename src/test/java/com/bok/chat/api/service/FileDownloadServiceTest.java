package com.bok.chat.api.service;

import com.bok.chat.api.dto.FileBatchDownloadResponse;
import com.bok.chat.api.dto.FileDownloadResponse;
import com.bok.chat.entity.ChatRoom;
import com.bok.chat.entity.ChatRoomUser;
import com.bok.chat.entity.FileAttachment;
import com.bok.chat.entity.User;
import com.bok.chat.repository.ChatRoomUserRepository;
import com.bok.chat.repository.FileAttachmentRepository;
import com.bok.chat.repository.MessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.bok.chat.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@DisplayName("FileDownloadService")
@ExtendWith(MockitoExtension.class)
class FileDownloadServiceTest {

    @InjectMocks
    private FileDownloadService fileDownloadService;

    @Mock
    private FileAttachmentRepository fileAttachmentRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRoomUserRepository chatRoomUserRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private S3KeyGenerator s3KeyGenerator;

    @Nested
    @DisplayName("단건 다운로드 URL")
    class GetDownloadUrl {

        @Test
        @DisplayName("성공 시 presigned URL과 파일 메타데이터를 반환한다")
        void getDownloadUrl_shouldReturnPresignedUrl() {
            User user = createUser(1L, "user1");
            FileAttachment file = createFileAttachment(10L, user, "photo.jpg", "image/jpeg", 2048);
            ChatRoom chatRoom = createChatRoom(1L, 2);
            ChatRoomUser member = createChatRoomUser(1L, chatRoom, user);

            given(fileAttachmentRepository.findById(10L)).willReturn(Optional.of(file));
            given(messageRepository.findChatRoomIdByFileId(10L)).willReturn(Optional.of(1L));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L)).willReturn(Optional.of(member));
            given(s3KeyGenerator.buildKey(10L, "photo.jpg")).willReturn("files/10/original.jpg");
            given(fileStorageService.generatePresignedUrl("files/10/original.jpg")).willReturn("https://s3/presigned-url");

            FileDownloadResponse result = fileDownloadService.getDownloadUrl(1L, 10L);

            assertThat(result.downloadUrl()).isEqualTo("https://s3/presigned-url");
            assertThat(result.originalFilename()).isEqualTo("photo.jpg");
            assertThat(result.contentType()).isEqualTo("image/jpeg");
            assertThat(result.fileSize()).isEqualTo(2048);
        }

        @Test
        @DisplayName("파일이 존재하지 않으면 예외가 발생한다")
        void getDownloadUrl_fileNotFound_shouldThrow() {
            given(fileAttachmentRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> fileDownloadService.getDownloadUrl(1L, 99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("파일이 존재하지 않습니다");
        }

        @Test
        @DisplayName("채팅방 멤버가 아니면 예외가 발생한다")
        void getDownloadUrl_notMember_shouldThrow() {
            User uploader = createUser(2L, "uploader");
            FileAttachment file = createFileAttachment(10L, uploader, "photo.jpg", "image/jpeg", 2048);

            given(fileAttachmentRepository.findById(10L)).willReturn(Optional.of(file));
            given(messageRepository.findChatRoomIdByFileId(10L)).willReturn(Optional.of(1L));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> fileDownloadService.getDownloadUrl(1L, 10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("접근 권한이 없습니다");
        }

        @Test
        @DisplayName("메시지에 연결되지 않은 파일이면 예외가 발생한다")
        void getDownloadUrl_noMessage_shouldThrow() {
            User user = createUser(1L, "user1");
            FileAttachment file = createFileAttachment(10L, user, "photo.jpg", "image/jpeg", 2048);

            given(fileAttachmentRepository.findById(10L)).willReturn(Optional.of(file));
            given(messageRepository.findChatRoomIdByFileId(10L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> fileDownloadService.getDownloadUrl(1L, 10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("메시지에 연결되지 않은 파일");
        }
    }

    @Nested
    @DisplayName("단건 썸네일 URL")
    class GetThumbnailUrl {

        @Test
        @DisplayName("성공 시 썸네일 presigned URL을 반환한다")
        void getThumbnailUrl_shouldReturnPresignedUrl() {
            User user = createUser(1L, "user1");
            FileAttachment file = createFileAttachment(10L, user, "photo.jpg", "image/jpeg", 2048);
            file.completeThumbnail("files/10/thumbnail.jpg");
            ChatRoom chatRoom = createChatRoom(1L, 2);
            ChatRoomUser member = createChatRoomUser(1L, chatRoom, user);

            given(fileAttachmentRepository.findById(10L)).willReturn(Optional.of(file));
            given(messageRepository.findChatRoomIdByFileId(10L)).willReturn(Optional.of(1L));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L)).willReturn(Optional.of(member));
            given(s3KeyGenerator.buildThumbnailKey(10L, "photo.jpg")).willReturn("files/10/thumbnail.jpg");
            given(fileStorageService.generatePresignedUrl("files/10/thumbnail.jpg")).willReturn("https://s3/thumb-url");

            FileDownloadResponse result = fileDownloadService.getThumbnailUrl(1L, 10L);

            assertThat(result.downloadUrl()).isEqualTo("https://s3/thumb-url");
        }

        @Test
        @DisplayName("썸네일이 없으면 예외가 발생한다")
        void getThumbnailUrl_noThumbnail_shouldThrow() {
            User user = createUser(1L, "user1");
            FileAttachment file = createFileAttachment(10L, user, "report.pdf", "application/pdf", 5000);
            ChatRoom chatRoom = createChatRoom(1L, 2);
            ChatRoomUser member = createChatRoomUser(1L, chatRoom, user);

            given(fileAttachmentRepository.findById(10L)).willReturn(Optional.of(file));
            given(messageRepository.findChatRoomIdByFileId(10L)).willReturn(Optional.of(1L));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L)).willReturn(Optional.of(member));

            assertThatThrownBy(() -> fileDownloadService.getThumbnailUrl(1L, 10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("썸네일이 존재하지 않습니다");
        }
    }

    @Nested
    @DisplayName("배치 다운로드 URL")
    class GetBatchDownloadUrls {

        @Test
        @DisplayName("성공 시 원본 + 썸네일 URL 목록을 반환한다")
        void getBatchDownloadUrls_shouldReturnUrls() {
            User user = createUser(1L, "user1");
            FileAttachment imageFile = createFileAttachment(10L, user, "photo.jpg", "image/jpeg", 2048);
            imageFile.completeThumbnail("files/10/thumbnail.jpg");
            FileAttachment pdfFile = createFileAttachment(11L, user, "report.pdf", "application/pdf", 5000);

            ChatRoom chatRoom = createChatRoom(1L, 2);
            ChatRoomUser member = createChatRoomUser(1L, chatRoom, user);

            given(fileAttachmentRepository.findAllById(List.of(10L, 11L))).willReturn(List.of(imageFile, pdfFile));
            given(messageRepository.findChatRoomIdsByFileIds(List.of(10L, 11L)))
                    .willReturn(List.of(new Object[]{10L, 1L}, new Object[]{11L, 1L}));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L)).willReturn(Optional.of(member));

            given(s3KeyGenerator.buildKey(10L, "photo.jpg")).willReturn("files/10/original.jpg");
            given(s3KeyGenerator.buildThumbnailKey(10L, "photo.jpg")).willReturn("files/10/thumbnail.jpg");
            given(s3KeyGenerator.buildKey(11L, "report.pdf")).willReturn("files/11/original.pdf");
            given(fileStorageService.generatePresignedUrl("files/10/original.jpg")).willReturn("https://s3/img-url");
            given(fileStorageService.generatePresignedUrl("files/10/thumbnail.jpg")).willReturn("https://s3/thumb-url");
            given(fileStorageService.generatePresignedUrl("files/11/original.pdf")).willReturn("https://s3/pdf-url");

            List<FileBatchDownloadResponse> result = fileDownloadService.getBatchDownloadUrls(1L, List.of(10L, 11L));

            assertThat(result).hasSize(2);

            FileBatchDownloadResponse imgResponse = result.stream()
                    .filter(r -> r.fileId().equals(10L)).findFirst().orElseThrow();
            assertThat(imgResponse.downloadUrl()).isEqualTo("https://s3/img-url");
            assertThat(imgResponse.thumbnailUrl()).isEqualTo("https://s3/thumb-url");

            FileBatchDownloadResponse pdfResponse = result.stream()
                    .filter(r -> r.fileId().equals(11L)).findFirst().orElseThrow();
            assertThat(pdfResponse.downloadUrl()).isEqualTo("https://s3/pdf-url");
            assertThat(pdfResponse.thumbnailUrl()).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 파일이 포함되면 전체 거부한다")
        void getBatchDownloadUrls_fileNotFound_shouldThrow() {
            User user = createUser(1L, "user1");
            FileAttachment file = createFileAttachment(10L, user, "photo.jpg", "image/jpeg", 2048);

            given(fileAttachmentRepository.findAllById(List.of(10L, 99L))).willReturn(List.of(file));

            assertThatThrownBy(() -> fileDownloadService.getBatchDownloadUrls(1L, List.of(10L, 99L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 파일");
        }

        @Test
        @DisplayName("접근 권한이 없는 파일이 포함되면 전체 거부한다")
        void getBatchDownloadUrls_noAccess_shouldThrow() {
            User user = createUser(1L, "user1");
            FileAttachment file1 = createFileAttachment(10L, user, "photo.jpg", "image/jpeg", 2048);
            FileAttachment file2 = createFileAttachment(11L, user, "doc.pdf", "application/pdf", 3000);

            given(fileAttachmentRepository.findAllById(List.of(10L, 11L))).willReturn(List.of(file1, file2));
            given(messageRepository.findChatRoomIdsByFileIds(List.of(10L, 11L)))
                    .willReturn(List.of(new Object[]{10L, 1L}, new Object[]{11L, 2L}));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Optional.of(createChatRoomUser(1L, createChatRoom(1L, 2), user)));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(2L, 1L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> fileDownloadService.getBatchDownloadUrls(1L, List.of(10L, 11L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("접근 권한이 없는 파일");
        }
    }
}
