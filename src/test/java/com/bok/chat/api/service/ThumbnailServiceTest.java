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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Nested
    @DisplayName("이벤트 핸들러")
    class HandleFileUploaded {

        @Test
        @DisplayName("PENDING 상태의 이미지 파일에 대해 썸네일을 생성한다")
        void shouldGenerateThumbnailForPendingImage() throws IOException {
            // given
            FileAttachment file = createImageAttachment();
            byte[] imageData = createTestImage(400, 300);

            given(fileAttachmentRepository.findById(1L)).willReturn(Optional.of(file));
            given(fileStorageService.download("files/1/original.png")).willReturn(imageData);
            given(s3KeyGenerator.buildThumbnailKey(1L, "test.png")).willReturn("files/1/thumbnail.png");

            // when
            thumbnailService.handleFileUploaded(new FileUploadedEvent(1L));

            // then
            then(fileStorageService).should().uploadWithKey(eq("files/1/thumbnail.png"), eq("image/png"), any(byte[].class));
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
        void shouldMarkAsFailedWhenResizeFails() {
            // given
            FileAttachment file = createImageAttachment();
            byte[] invalidData = "not an image".getBytes();

            given(fileAttachmentRepository.findById(1L)).willReturn(Optional.of(file));
            given(fileStorageService.download("files/1/original.png")).willReturn(invalidData);

            // when
            thumbnailService.handleFileUploaded(new FileUploadedEvent(1L));

            // then
            then(fileStorageService).should(never()).uploadWithKey(anyString(), anyString(), any(byte[].class));
            assertThat(file.getThumbnailStatus()).isEqualTo(ThumbnailStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("이미지 리사이즈")
    class Resize {

        @Test
        @DisplayName("큰 이미지를 200x200 이내로 리사이즈한다")
        void shouldResizeLargeImage() throws IOException {
            // given
            byte[] imageData = createTestImage(400, 300);

            // when
            byte[] result = thumbnailService.resize(imageData, "image/png");

            // then
            BufferedImage thumbnail = ImageIO.read(new ByteArrayInputStream(result));
            assertThat(thumbnail.getWidth()).isEqualTo(200);
            assertThat(thumbnail.getHeight()).isEqualTo(150);
        }

        @Test
        @DisplayName("세로로 긴 이미지는 높이를 기준으로 리사이즈한다")
        void shouldResizeByHeightForTallImage() throws IOException {
            // given
            byte[] imageData = createTestImage(300, 600);

            // when
            byte[] result = thumbnailService.resize(imageData, "image/png");

            // then
            BufferedImage thumbnail = ImageIO.read(new ByteArrayInputStream(result));
            assertThat(thumbnail.getWidth()).isEqualTo(100);
            assertThat(thumbnail.getHeight()).isEqualTo(200);
        }

        @Test
        @DisplayName("이미 작은 이미지는 원본을 그대로 반환한다")
        void shouldReturnOriginalForSmallImage() throws IOException {
            // given
            byte[] imageData = createTestImage(100, 80);

            // when
            byte[] result = thumbnailService.resize(imageData, "image/png");

            // then
            assertThat(result).isEqualTo(imageData);
        }

        @Test
        @DisplayName("유효하지 않은 이미지 데이터는 IOException을 던진다")
        void shouldThrowForInvalidImageData() {
            // given
            byte[] invalidData = "not an image".getBytes();

            // when & then
            assertThatThrownBy(() -> thumbnailService.resize(invalidData, "image/png"))
                    .isInstanceOf(IOException.class);
        }
    }

    private FileAttachment createImageAttachment() {
        User uploader = User.builder().username("tester").password("password").build();
        FileAttachment file = FileAttachment.create(uploader, "test.png", "image/png", 1024);
        file.assignStoredPath("files/1/original.png");
        // Use reflection to set ID since it's auto-generated
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

    private byte[] createTestImage(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
