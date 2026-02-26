package com.bok.chat.api.service;

import com.bok.chat.api.dto.FileUploadResponse;
import com.bok.chat.entity.FileAttachment;
import com.bok.chat.entity.FileAttachment.ThumbnailStatus;
import com.bok.chat.entity.User;
import com.bok.chat.event.FileUploadedEvent;
import com.bok.chat.repository.FileAttachmentRepository;
import com.bok.chat.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static com.bok.chat.support.TestFixtures.createUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("FileUploadService")
@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {

    @InjectMocks
    private FileUploadService fileUploadService;

    @Mock
    private FileAttachmentRepository fileAttachmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Nested
    @DisplayName("업로드 성공")
    class UploadSuccess {

        @Test
        @DisplayName("이미지 파일 업로드 시 PENDING 상태로 응답한다")
        void upload_imageFile_shouldReturnPendingStatus() throws IOException {
            User uploader = createUser(1L, "user1");
            MultipartFile file = mockMultipartFile("photo.jpg", "image/jpeg", 1024);

            given(userRepository.findById(1L)).willReturn(Optional.of(uploader));
            given(fileAttachmentRepository.save(any(FileAttachment.class)))
                    .willAnswer(invocation -> {
                        FileAttachment attachment = invocation.getArgument(0);
                        org.springframework.test.util.ReflectionTestUtils.setField(attachment, "id", 1L);
                        return attachment;
                    });
            given(fileStorageService.upload(eq(1L), eq("photo.jpg"), eq("image/jpeg"), any(byte[].class)))
                    .willReturn("files/1/original.jpg");

            FileUploadResponse response = fileUploadService.upload(1L, file);

            assertThat(response.fileId()).isEqualTo(1L);
            assertThat(response.originalFilename()).isEqualTo("photo.jpg");
            assertThat(response.contentType()).isEqualTo("image/jpeg");
            assertThat(response.fileSize()).isEqualTo(1024);
            assertThat(response.thumbnailStatus()).isEqualTo(ThumbnailStatus.PENDING);
        }

        @Test
        @DisplayName("PDF 파일 업로드 시 NONE 상태로 응답한다")
        void upload_pdfFile_shouldReturnNoneStatus() throws IOException {
            User uploader = createUser(1L, "user1");
            MultipartFile file = mockMultipartFile("doc.pdf", "application/pdf", 2048);

            given(userRepository.findById(1L)).willReturn(Optional.of(uploader));
            given(fileAttachmentRepository.save(any(FileAttachment.class)))
                    .willAnswer(invocation -> {
                        FileAttachment attachment = invocation.getArgument(0);
                        org.springframework.test.util.ReflectionTestUtils.setField(attachment, "id", 2L);
                        return attachment;
                    });
            given(fileStorageService.upload(eq(2L), eq("doc.pdf"), eq("application/pdf"), any(byte[].class)))
                    .willReturn("files/2/original.pdf");

            FileUploadResponse response = fileUploadService.upload(1L, file);

            assertThat(response.thumbnailStatus()).isEqualTo(ThumbnailStatus.NONE);
        }

        @Test
        @DisplayName("업로드 성공 시 FileUploadedEvent가 발행된다")
        void upload_shouldPublishEvent() throws IOException {
            User uploader = createUser(1L, "user1");
            MultipartFile file = mockMultipartFile("photo.jpg", "image/jpeg", 1024);

            given(userRepository.findById(1L)).willReturn(Optional.of(uploader));
            given(fileAttachmentRepository.save(any(FileAttachment.class)))
                    .willAnswer(invocation -> {
                        FileAttachment attachment = invocation.getArgument(0);
                        org.springframework.test.util.ReflectionTestUtils.setField(attachment, "id", 1L);
                        return attachment;
                    });
            given(fileStorageService.upload(eq(1L), eq("photo.jpg"), eq("image/jpeg"), any(byte[].class)))
                    .willReturn("files/1/original.jpg");

            fileUploadService.upload(1L, file);

            ArgumentCaptor<FileUploadedEvent> captor = ArgumentCaptor.forClass(FileUploadedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().fileId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("업로드 성공 시 storedPath가 설정된다")
        void upload_shouldAssignStoredPath() throws IOException {
            User uploader = createUser(1L, "user1");
            MultipartFile file = mockMultipartFile("photo.jpg", "image/jpeg", 1024);

            given(userRepository.findById(1L)).willReturn(Optional.of(uploader));
            given(fileAttachmentRepository.save(any(FileAttachment.class)))
                    .willAnswer(invocation -> {
                        FileAttachment attachment = invocation.getArgument(0);
                        org.springframework.test.util.ReflectionTestUtils.setField(attachment, "id", 1L);
                        return attachment;
                    });
            given(fileStorageService.upload(eq(1L), eq("photo.jpg"), eq("image/jpeg"), any(byte[].class)))
                    .willReturn("files/1/original.jpg");

            fileUploadService.upload(1L, file);

            ArgumentCaptor<FileAttachment> captor = ArgumentCaptor.forClass(FileAttachment.class);
            verify(fileAttachmentRepository).save(captor.capture());
            FileAttachment saved = captor.getValue();
            assertThat(saved.getStoredPath()).isEqualTo("files/1/original.jpg");
        }
    }

    @Nested
    @DisplayName("업로드 검증 실패")
    class UploadValidation {

        @Test
        @DisplayName("빈 파일은 업로드할 수 없다")
        void upload_emptyFile_shouldThrow() {
            MultipartFile file = mock(MultipartFile.class);
            given(file.isEmpty()).willReturn(true);

            assertThatThrownBy(() -> fileUploadService.upload(1L, file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("빈 파일");
        }

        @Test
        @DisplayName("파일명이 null이면 업로드할 수 없다")
        void upload_nullFilename_shouldThrow() {
            MultipartFile file = mock(MultipartFile.class);
            given(file.isEmpty()).willReturn(false);
            given(file.getOriginalFilename()).willReturn(null);

            assertThatThrownBy(() -> fileUploadService.upload(1L, file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("파일명");
        }

        @Test
        @DisplayName("3MB 초과 파일은 업로드할 수 없다")
        void upload_oversizedFile_shouldThrow() {
            MultipartFile file = mock(MultipartFile.class);
            given(file.isEmpty()).willReturn(false);
            given(file.getOriginalFilename()).willReturn("large.jpg");
            given(file.getSize()).willReturn(4 * 1024 * 1024L);

            assertThatThrownBy(() -> fileUploadService.upload(1L, file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("3MB");
        }

        @Test
        @DisplayName("허용되지 않는 MIME 타입은 업로드할 수 없다")
        void upload_disallowedMimeType_shouldThrow() {
            MultipartFile file = mock(MultipartFile.class);
            given(file.isEmpty()).willReturn(false);
            given(file.getOriginalFilename()).willReturn("script.js");
            given(file.getSize()).willReturn(1024L);
            given(file.getContentType()).willReturn("application/javascript");

            assertThatThrownBy(() -> fileUploadService.upload(1L, file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("허용되지 않는 파일 형식");
        }
    }

    private MultipartFile mockMultipartFile(String filename, String contentType, long size) throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getOriginalFilename()).willReturn(filename);
        given(file.getSize()).willReturn(size);
        given(file.getContentType()).willReturn(contentType);
        given(file.getBytes()).willReturn(new byte[(int) size]);
        return file;
    }
}
