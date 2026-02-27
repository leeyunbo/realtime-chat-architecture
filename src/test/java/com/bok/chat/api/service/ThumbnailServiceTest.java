package com.bok.chat.api.service;

import com.bok.chat.entity.FileAttachment;
import com.bok.chat.entity.FileAttachment.ThumbnailStatus;
import com.bok.chat.entity.User;
import com.bok.chat.event.FileUploadedEvent;
import com.bok.chat.repository.FileAttachmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("ThumbnailService")
class ThumbnailServiceTest {

    @InjectMocks
    private ThumbnailService thumbnailService;

    @Mock
    private FileAttachmentRepository fileAttachmentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private S3KeyGenerator s3KeyGenerator;

    @Mock
    private ImageResizer imageResizer;

    @Nested
    @DisplayName("이벤트 핸들러")
    class HandleFileUploaded {

        @Test
        @DisplayName("PENDING 상태의 이미지 파일에 대해 썸네일을 생성한다")
        void shouldGenerateThumbnailForPendingImage() throws IOException {
            // given
            FileAttachment file = createImageAttachment();
            byte[] imageData = new byte[]{1, 2, 3};
            byte[] thumbnailData = new byte[]{4, 5, 6};

            given(fileAttachmentRepository.findById(1L)).willReturn(Optional.of(file));
            given(fileStorageService.download("files/1/original.png")).willReturn(imageData);
            given(imageResizer.resize(imageData, "image/png")).willReturn(thumbnailData);
            given(s3KeyGenerator.buildThumbnailKey(1L, "test.png")).willReturn("files/1/thumbnail.png");

            // when
            thumbnailService.handleFileUploaded(new FileUploadedEvent(1L));

            // then
            then(fileStorageService).should().uploadWithKey(eq("files/1/thumbnail.png"), eq("image/png"), eq(thumbnailData));
            assertThat(file.getThumbnailStatus()).isEqualTo(ThumbnailStatus.COMPLETED);
            assertThat(file.getThumbnailPath()).isEqualTo("files/1/thumbnail.png");
        }

        @Test
        @DisplayName("NONE 상태의 파일은 처리하지 않는다")
        void shouldSkipNonImageFile() {
            // given
            FileAttachment file = createNonImageAttachment();
            given(fileAttachmentRepository.findById(1L)).willReturn(Optional.of(file));

            // when
            thumbnailService.handleFileUploaded(new FileUploadedEvent(1L));

            // then
            then(fileStorageService).should(never()).download(anyString());
            assertThat(file.getThumbnailStatus()).isEqualTo(ThumbnailStatus.NONE);
        }

        @Test
        @DisplayName("파일이 존재하지 않으면 무시한다")
        void shouldIgnoreWhenFileNotFound() {
            // given
            given(fileAttachmentRepository.findById(1L)).willReturn(Optional.empty());

            // when
            thumbnailService.handleFileUploaded(new FileUploadedEvent(1L));

            // then
            then(fileStorageService).should(never()).download(anyString());
        }

        @Test
        @DisplayName("리사이즈 실패 시 FAILED 상태로 변경한다")
        void shouldMarkAsFailedWhenResizeFails() throws IOException {
            // given
            FileAttachment file = createImageAttachment();
            byte[] imageData = new byte[]{1, 2, 3};

            given(fileAttachmentRepository.findById(1L)).willReturn(Optional.of(file));
            given(fileStorageService.download("files/1/original.png")).willReturn(imageData);
            given(imageResizer.resize(imageData, "image/png")).willThrow(new IOException("Failed to read image"));

            // when
            thumbnailService.handleFileUploaded(new FileUploadedEvent(1L));

            // then
            then(fileStorageService).should(never()).uploadWithKey(anyString(), anyString(), any(byte[].class));
            assertThat(file.getThumbnailStatus()).isEqualTo(ThumbnailStatus.FAILED);
        }
    }

    private FileAttachment createImageAttachment() {
        User uploader = User.builder().username("tester").password("password").build();
        FileAttachment file = FileAttachment.create(uploader, "test.png", "image/png", 1024);
        file.assignStoredPath("files/1/original.png");
        setId(file, 1L);
        return file;
    }

    private FileAttachment createNonImageAttachment() {
        User uploader = User.builder().username("tester").password("password").build();
        FileAttachment file = FileAttachment.create(uploader, "doc.pdf", "application/pdf", 2048);
        file.assignStoredPath("files/1/original.pdf");
        setId(file, 1L);
        return file;
    }

    private void setId(FileAttachment file, Long id) {
        try {
            var field = FileAttachment.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(file, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
