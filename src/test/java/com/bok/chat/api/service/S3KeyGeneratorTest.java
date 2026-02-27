package com.bok.chat.api.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("S3KeyGenerator")
class S3KeyGeneratorTest {

    private final S3KeyGenerator s3KeyGenerator = new S3KeyGenerator();

    @Nested
    @DisplayName("S3 키 생성")
    class BuildKey {

        @Test
        @DisplayName("파일 ID와 확장자로 키를 생성한다")
        void buildKey_shouldGenerateCorrectKey() {
            String key = s3KeyGenerator.buildKey(42L, "photo.jpg");

            assertThat(key).isEqualTo("files/42/original.jpg");
        }

        @Test
        @DisplayName("확장자가 없는 파일은 확장자 없이 키를 생성한다")
        void buildKey_noExtension_shouldGenerateKeyWithoutExtension() {
            String key = s3KeyGenerator.buildKey(1L, "README");

            assertThat(key).isEqualTo("files/1/original");
        }

        @Test
        @DisplayName("복합 확장자는 마지막 확장자만 사용한다")
        void buildKey_multipleExtensions_shouldUseLastExtension() {
            String key = s3KeyGenerator.buildKey(5L, "archive.tar.gz");

            assertThat(key).isEqualTo("files/5/original.gz");
        }
    }

    @Nested
    @DisplayName("썸네일 키 생성")
    class BuildThumbnailKey {

        @Test
        @DisplayName("파일 ID와 확장자로 썸네일 키를 생성한다")
        void buildThumbnailKey_shouldGenerateCorrectKey() {
            String key = s3KeyGenerator.buildThumbnailKey(42L, "photo.jpg");

            assertThat(key).isEqualTo("files/42/thumbnail.jpg");
        }

        @Test
        @DisplayName("확장자가 없는 파일은 확장자 없이 썸네일 키를 생성한다")
        void buildThumbnailKey_noExtension_shouldGenerateKeyWithoutExtension() {
            String key = s3KeyGenerator.buildThumbnailKey(1L, "README");

            assertThat(key).isEqualTo("files/1/thumbnail");
        }
    }

    @Nested
    @DisplayName("확장자 추출")
    class ExtractExtension {

        @Test
        @DisplayName("일반 파일명에서 확장자를 추출한다")
        void extractExtension_shouldReturnExtension() {
            String extension = s3KeyGenerator.extractExtension("photo.jpg");

            assertThat(extension).isEqualTo(".jpg");
        }

        @Test
        @DisplayName("확장자가 없으면 빈 문자열을 반환한다")
        void extractExtension_noExtension_shouldReturnEmpty() {
            String extension = s3KeyGenerator.extractExtension("README");

            assertThat(extension).isEmpty();
        }

        @Test
        @DisplayName("파일명이 null이면 빈 문자열을 반환한다")
        void extractExtension_null_shouldReturnEmpty() {
            String extension = s3KeyGenerator.extractExtension(null);

            assertThat(extension).isEmpty();
        }
    }
}
