package com.bok.chat.api.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileStorageService")
@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @InjectMocks
    private FileStorageService fileStorageService;

    @Nested
    @DisplayName("S3 키 생성")
    class BuildKey {

        @Test
        @DisplayName("파일 ID와 확장자로 키를 생성한다")
        void buildKey_shouldGenerateCorrectKey() {
            String key = fileStorageService.buildKey(42L, "photo.jpg");

            assertThat(key).isEqualTo("files/42/original.jpg");
        }

        @Test
        @DisplayName("확장자가 없는 파일은 확장자 없이 키를 생성한다")
        void buildKey_noExtension_shouldGenerateKeyWithoutExtension() {
            String key = fileStorageService.buildKey(1L, "README");

            assertThat(key).isEqualTo("files/1/original");
        }

        @Test
        @DisplayName("복합 확장자는 마지막 확장자만 사용한다")
        void buildKey_multipleExtensions_shouldUseLastExtension() {
            String key = fileStorageService.buildKey(5L, "archive.tar.gz");

            assertThat(key).isEqualTo("files/5/original.gz");
        }
    }

    @Nested
    @DisplayName("확장자 추출")
    class ExtractExtension {

        @Test
        @DisplayName("일반 파일명에서 확장자를 추출한다")
        void extractExtension_shouldReturnExtension() {
            String extension = fileStorageService.extractExtension("photo.jpg");

            assertThat(extension).isEqualTo(".jpg");
        }

        @Test
        @DisplayName("확장자가 없으면 빈 문자열을 반환한다")
        void extractExtension_noExtension_shouldReturnEmpty() {
            String extension = fileStorageService.extractExtension("README");

            assertThat(extension).isEmpty();
        }

        @Test
        @DisplayName("파일명이 null이면 빈 문자열을 반환한다")
        void extractExtension_null_shouldReturnEmpty() {
            String extension = fileStorageService.extractExtension(null);

            assertThat(extension).isEmpty();
        }
    }
}
