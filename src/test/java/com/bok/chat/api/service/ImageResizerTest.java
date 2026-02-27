package com.bok.chat.api.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ImageResizer")
class ImageResizerTest {

    private final ImageResizer imageResizer = new ImageResizer();

    @Nested
    @DisplayName("이미지 리사이즈")
    class Resize {

        @Test
        @DisplayName("큰 이미지를 200x200 이내로 리사이즈한다")
        void shouldResizeLargeImage() throws IOException {
            // given
            byte[] imageData = createTestImage(400, 300);

            // when
            byte[] result = imageResizer.resize(imageData, "image/png");

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
            byte[] result = imageResizer.resize(imageData, "image/png");

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
            byte[] result = imageResizer.resize(imageData, "image/png");

            // then
            assertThat(result).isEqualTo(imageData);
        }

        @Test
        @DisplayName("유효하지 않은 이미지 데이터는 IOException을 던진다")
        void shouldThrowForInvalidImageData() {
            // given
            byte[] invalidData = "not an image".getBytes();

            // when & then
            assertThatThrownBy(() -> imageResizer.resize(invalidData, "image/png"))
                    .isInstanceOf(IOException.class);
        }
    }

    private byte[] createTestImage(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
