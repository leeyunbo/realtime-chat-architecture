package com.bok.chat.api.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PostgresTsQueryConverter")
class PostgresTsQueryConverterTest {

    private final PostgresTsQueryConverter converter = new PostgresTsQueryConverter();

    @Test
    @DisplayName("단일 단어를 prefix 매칭 tsquery로 변환한다")
    void convert_singleWord() {
        assertThat(converter.convert("hello")).isEqualTo("hello:*");
    }

    @Test
    @DisplayName("여러 단어를 AND 조건 tsquery로 변환한다")
    void convert_multipleWords() {
        assertThat(converter.convert("hello world")).isEqualTo("hello:* & world:*");
    }

    @Test
    @DisplayName("특수문자를 제거한다 (tsquery 연산자 인젝션 방지)")
    void convert_specialCharacters() {
        assertThat(converter.convert("hello!!! & world")).isEqualTo("hello:* & world:*");
    }

    @Test
    @DisplayName("특수문자만 입력하면 빈 문자열을 반환한다")
    void convert_onlySpecialCharacters() {
        assertThat(converter.convert("!@#$%")).isEqualTo("");
    }

    @Test
    @DisplayName("공백만 입력하면 빈 문자열을 반환한다")
    void convert_blankInput() {
        assertThat(converter.convert("   ")).isEqualTo("");
    }

    @Test
    @DisplayName("한국어 단어를 prefix 매칭 tsquery로 변환한다")
    void convert_korean() {
        assertThat(converter.convert("점심 먹자")).isEqualTo("점심:* & 먹자:*");
    }
}
