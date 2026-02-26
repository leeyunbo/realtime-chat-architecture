package com.bok.chat.entity;

import com.bok.chat.entity.FileAttachment.ThumbnailStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.bok.chat.support.TestFixtures.createUser;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileAttachment 엔티티")
class FileAttachmentTest {

    @Nested
    @DisplayName("팩토리 메서드")
    class Create {

        @Test
        @DisplayName("이미지 파일이면 thumbnailStatus가 PENDING이다")
        void create_imageFile_shouldSetThumbnailStatusPending() {
            User uploader = createUser(1L, "user1");

            FileAttachment attachment = FileAttachment.create(uploader, "photo.jpg", "image/jpeg", 1024);

            assertThat(attachment.getUploader()).isEqualTo(uploader);
            assertThat(attachment.getOriginalFilename()).isEqualTo("photo.jpg");
            assertThat(attachment.getContentType()).isEqualTo("image/jpeg");
            assertThat(attachment.getFileSize()).isEqualTo(1024);
            assertThat(attachment.getThumbnailStatus()).isEqualTo(ThumbnailStatus.PENDING);
        }

        @Test
        @DisplayName("이미지가 아닌 파일이면 thumbnailStatus가 NONE이다")
        void create_nonImageFile_shouldSetThumbnailStatusNone() {
            User uploader = createUser(1L, "user1");

            FileAttachment attachment = FileAttachment.create(uploader, "doc.pdf", "application/pdf", 2048);

            assertThat(attachment.getThumbnailStatus()).isEqualTo(ThumbnailStatus.NONE);
        }

        @Test
        @DisplayName("image/png는 이미지로 인식된다")
        void create_pngFile_shouldSetThumbnailStatusPending() {
            User uploader = createUser(1L, "user1");

            FileAttachment attachment = FileAttachment.create(uploader, "image.png", "image/png", 512);

            assertThat(attachment.getThumbnailStatus()).isEqualTo(ThumbnailStatus.PENDING);
        }

        @Test
        @DisplayName("image/gif는 이미지로 인식된다")
        void create_gifFile_shouldSetThumbnailStatusPending() {
            User uploader = createUser(1L, "user1");

            FileAttachment attachment = FileAttachment.create(uploader, "anim.gif", "image/gif", 256);

            assertThat(attachment.getThumbnailStatus()).isEqualTo(ThumbnailStatus.PENDING);
        }

        @Test
        @DisplayName("image/webp는 이미지로 인식된다")
        void create_webpFile_shouldSetThumbnailStatusPending() {
            User uploader = createUser(1L, "user1");

            FileAttachment attachment = FileAttachment.create(uploader, "photo.webp", "image/webp", 768);

            assertThat(attachment.getThumbnailStatus()).isEqualTo(ThumbnailStatus.PENDING);
        }

        @Test
        @DisplayName("storedPath와 thumbnailPath는 초기에 null이다")
        void create_shouldHaveNullPaths() {
            User uploader = createUser(1L, "user1");

            FileAttachment attachment = FileAttachment.create(uploader, "file.pdf", "application/pdf", 1024);

            assertThat(attachment.getStoredPath()).isNull();
            assertThat(attachment.getThumbnailPath()).isNull();
        }
    }

    @Nested
    @DisplayName("비즈니스 메서드")
    class BusinessMethods {

        @Test
        @DisplayName("assignStoredPath로 저장 경로를 설정한다")
        void assignStoredPath_shouldSetStoredPath() {
            User uploader = createUser(1L, "user1");
            FileAttachment attachment = FileAttachment.create(uploader, "photo.jpg", "image/jpeg", 1024);

            attachment.assignStoredPath("files/1/original.jpg");

            assertThat(attachment.getStoredPath()).isEqualTo("files/1/original.jpg");
        }

        @Test
        @DisplayName("completeThumbnail로 썸네일 경로와 상태를 설정한다")
        void completeThumbnail_shouldSetPathAndStatus() {
            User uploader = createUser(1L, "user1");
            FileAttachment attachment = FileAttachment.create(uploader, "photo.jpg", "image/jpeg", 1024);

            attachment.completeThumbnail("files/1/thumbnail.jpg");

            assertThat(attachment.getThumbnailPath()).isEqualTo("files/1/thumbnail.jpg");
            assertThat(attachment.getThumbnailStatus()).isEqualTo(ThumbnailStatus.COMPLETED);
        }

        @Test
        @DisplayName("failThumbnail로 썸네일 실패 상태를 설정한다")
        void failThumbnail_shouldSetFailedStatus() {
            User uploader = createUser(1L, "user1");
            FileAttachment attachment = FileAttachment.create(uploader, "photo.jpg", "image/jpeg", 1024);

            attachment.failThumbnail();

            assertThat(attachment.getThumbnailStatus()).isEqualTo(ThumbnailStatus.FAILED);
        }
    }
}
