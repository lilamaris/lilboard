package com.lilamaris.lilboard.board.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.lilamaris.lilboard.kernel.condition.ComparisonCondition;
import com.lilamaris.lilboard.kernel.condition.StringCondition;
import com.lilamaris.lilboard.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilboard.kernel.exception.IllegalValueConditionException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Board 테스트")
class BoardTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @DisplayName("Board를 생성하면 입력한 값으로 필드를 채우고 updatedAt은 null이다")
    void createsBoardWithGivenValues() {
        Board board = Board.of(ID, "게시판", "설명", CREATED_AT);

        assertThat(board.getId()).isEqualTo(ID);
        assertThat(board.getName()).isEqualTo("게시판");
        assertThat(board.getDescription()).isEqualTo("설명");
        assertThat(board.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(board.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("Board를 생성하면 조건을 만족하는 최소 길이 문자열은 값을 그대로 저장한다")
    void keepsValuesWhenTheyAreMinimumLengthNonBlankStrings() {
        Board board = Board.of(ID, "ㅏ", "ㅏ", CREATED_AT);

        assertThat(board.getName()).isEqualTo("ㅏ");
        assertThat(board.getDescription()).isEqualTo("ㅏ");
    }

    @Test
    @DisplayName("Board를 생성하면 최대 길이에 정확히 도달한 문자열은 값을 그대로 저장한다")
    void keepsValuesWhenTheyAreExactlyMaximumLength() {
        String name = "a".repeat(50);
        String description = "b".repeat(300);

        Board board = Board.of(ID, name, description, CREATED_AT);

        assertThat(board.getName()).isEqualTo(name);
        assertThat(board.getDescription()).isEqualTo(description);
    }

    @Test
    @DisplayName("Board를 생성하면 name이 최대 길이 50을 넘으면 예외를 던진다")
    void throwsWhenNameExceedsMaxLength() {
        String name = "a".repeat(51);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> Board.of(ID, name, "설명", CREATED_AT));

        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AT_MOST);
        assertThat(exception.subject()).isEqualTo(51);
        assertThat(exception.reference()).isEqualTo(50);
    }

    @Test
    @DisplayName("Board를 생성하면 description이 최대 길이 300을 넘으면 예외를 던진다")
    void throwsWhenDescriptionExceedsMaxLength() {
        String description = "b".repeat(301);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> Board.of(ID, "게시판", description, CREATED_AT));

        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AT_MOST);
        assertThat(exception.subject()).isEqualTo(301);
        assertThat(exception.reference()).isEqualTo(300);
    }

    @Test
    @DisplayName("Board를 생성하면 name이 빈 문자열일 때 예외를 던진다")
    void throwsWhenNameIsBlank() {
        String name = " ";

        IllegalValueConditionException exception =
                catchThrowableOfType(
                        IllegalValueConditionException.class,
                        () -> Board.of(ID, name, "설명", CREATED_AT));

        assertThat(exception).hasMessage("must not be blank.");
        assertThat(exception.condition()).isEqualTo(StringCondition.NOT_BLANK);
        assertThat(exception.subject()).isEqualTo(name);
    }

    @Test
    @DisplayName("Board를 생성하면 description이 빈 문자열일 때 예외를 던진다")
    void throwsWhenDescriptionIsBlank() {
        String description = "   ";

        IllegalValueConditionException exception =
                catchThrowableOfType(
                        IllegalValueConditionException.class,
                        () -> Board.of(ID, "게시판", description, CREATED_AT));

        assertThat(exception).hasMessage("must not be blank.");
        assertThat(exception.condition()).isEqualTo(StringCondition.NOT_BLANK);
        assertThat(exception.subject()).isEqualTo(description);
    }

    @Test
    @DisplayName("Board를 생성하면 name이 null일 때 NullPointerException을 던진다")
    void throwsNullPointerExceptionWhenNameIsNull() {
        String name = null;

        assertThatThrownBy(() -> Board.of(ID, name, "설명", CREATED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("Board를 생성하면 description이 null일 때 NullPointerException을 던진다")
    void throwsNullPointerExceptionWhenDescriptionIsNull() {
        String description = null;

        assertThatThrownBy(() -> Board.of(ID, "게시판", description, CREATED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("Board를 생성하면 createdAt이 null일 때 NullPointerException을 던진다")
    void throwsNullPointerExceptionWhenCreatedAtIsNull() {
        Instant createdAt = null;

        assertThatThrownBy(() -> Board.of(ID, "게시판", "설명", createdAt))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Board를 생성하면 id가 null일 때 NullPointerException을 던진다")
    void throwsNullPointerExceptionWhenIdIsNull() {
        UUID id = null;

        assertThatThrownBy(() -> Board.of(id, "게시판", "설명", CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }
}
