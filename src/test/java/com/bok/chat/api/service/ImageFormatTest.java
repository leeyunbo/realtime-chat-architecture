package com.bok.chat.api.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ImageFormat")
class ImageFormatTest {

    @ParameterizedTest
    @CsvSource({
            "image/jpeg, jpg",
            "image/png, png",
            "image/gif, gif",
            "image/webp, webp"
    })
    @DisplayName("contentType에 해당하는 포맷명을 반환한다")
    void formatNameOf_shouldReturnCorrectFormatName(String contentType, String expectedFormat) {
        assertThat(ImageFormat.formatNameOf(contentType)).isEqualTo(expectedFormat);
    }

    @Test
    @DisplayName("알 수 없는 contentType은 png를 반환한다")
    void formatNameOf_unknownType_shouldReturnPng() {
        assertThat(ImageFormat.formatNameOf("image/bmp")).isEqualTo("png");
    }

    @Test
    @DisplayName("null contentType은 png를 반환한다")
    void formatNameOf_null_shouldReturnPng() {
        assertThat(ImageFormat.formatNameOf(null)).isEqualTo("png");
    }
}
