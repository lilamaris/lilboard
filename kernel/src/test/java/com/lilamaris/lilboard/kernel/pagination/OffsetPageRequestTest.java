package com.lilamaris.lilboard.kernel.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilboard.kernel.exception.IllegalValueConditionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OffsetPageRequestTest {
    @ParameterizedTest
    @CsvSource({"0, 1, 0", "2, 20, 40", "2147483647, 2147483647, 4611686014132420609"})
    @DisplayName("유효한 페이지와 크기로 요청을 만들면 오버플로 없이 오프셋을 계산한다")
    void calculatesOffset(int page, int size, long offset) {
        var request = OffsetPageRequest.of(page, size);
        assertThat(request.page()).isEqualTo(page);
        assertThat(request.size()).isEqualTo(size);
        assertThat(request.offset()).isEqualTo(offset);
    }

    @ParameterizedTest
    @CsvSource({"-1, 20", "0, 0", "0, -1"})
    @DisplayName("페이지가 음수이거나 크기가 양수가 아니면 요청을 거부한다")
    void rejectsInvalidRequest(int page, int size) {
        assertThatThrownBy(() -> OffsetPageRequest.of(page, size))
                .isInstanceOf(IllegalValueConditionException.class);
    }
}
