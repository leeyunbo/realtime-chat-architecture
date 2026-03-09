package com.bok.chat.api.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CursorPage")
class CursorPageTest {

    @Test
    @DisplayName("결과가 size보다 많으면 hasNext=true이고 nextCursor를 Base64로 인코딩한다")
    void of_withMoreResults_shouldReturnHasNextAndEncodedCursor() {
        List<Long> results = List.of(30L, 20L, 10L);

        CursorPage<Long> page = CursorPage.of(results, 2, id -> id);

        assertThat(page.hasNext()).isTrue();
        assertThat(page.items()).containsExactly(30L, 20L);
        assertThat(page.nextCursor()).isNotNull();
        assertThat(CursorPage.decodeCursor(page.nextCursor())).isEqualTo(20L);
    }

    @Test
    @DisplayName("결과가 size 이하이면 hasNext=false이고 nextCursor는 null이다")
    void of_withNoMore_shouldReturnNoNext() {
        List<Long> results = List.of(30L, 20L);

        CursorPage<Long> page = CursorPage.of(results, 2, id -> id);

        assertThat(page.hasNext()).isFalse();
        assertThat(page.items()).containsExactly(30L, 20L);
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    @DisplayName("null 커서를 디코딩하면 null을 반환한다")
    void decodeCursor_null_shouldReturnNull() {
        assertThat(CursorPage.decodeCursor(null)).isNull();
    }

    @Test
    @DisplayName("빈 문자열 커서를 디코딩하면 null을 반환한다")
    void decodeCursor_blank_shouldReturnNull() {
        assertThat(CursorPage.decodeCursor("  ")).isNull();
    }

    @Test
    @DisplayName("잘못된 커서 값을 디코딩하면 예외가 발생한다")
    void decodeCursor_invalid_shouldThrow() {
        assertThatThrownBy(() -> CursorPage.decodeCursor("not-valid-base64!!!"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("인코딩된 커서를 디코딩하면 원래 ID가 복원된다")
    void encodeDecode_roundTrip() {
        List<Long> results = List.of(45L, 44L);

        CursorPage<Long> page = CursorPage.of(results, 1, id -> id);

        assertThat(CursorPage.decodeCursor(page.nextCursor())).isEqualTo(45L);
    }
}
