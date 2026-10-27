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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Board 테스트")
class BoardTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @DisplayName("Board를 생성하면 입력한 값으로 필드를 채우고 수정 시각과 삭제 시각은 null이다")
    void createsBoardWithGivenValues() {
        Board board = Board.of(ID, "게시판", "설명", CREATED_AT);

        assertThat(board.isDeleted()).isFalse();
        assertThat(board.getId()).isEqualTo(ID);
        assertThat(board.getName()).isEqualTo("게시판");
        assertThat(board.getDescription()).isEqualTo("설명");
        assertThat(board.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(board.getUpdatedAt()).isNull();
        assertThat(board.getDeletedAt()).isNull();
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
    @DisplayName("Board를 생성하면 name이 공백 문자열일 때 예외를 던진다")
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
    @DisplayName("Board를 생성하면 description이 공백 문자열일 때 예외를 던진다")
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

    @ParameterizedTest
    @ValueSource(ints = {1, 50})
    @DisplayName("name을 최소·최대 길이로 수정하면 해당 값과 수정 시각을 저장하고 나머지 필드를 유지한다")
    void updatesNameAtLengthBoundaries(int length) {
        Board board = Board.of(ID, "게시판", "설명", CREATED_AT);
        String originalOther = board.getDescription();
        String value = "a".repeat(length);
        Instant updatedAt = CREATED_AT.plusSeconds(1);

        board.updateName(value, updatedAt);

        assertThat(board.getName()).isEqualTo(value);
        assertThat(board.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(board.getDescription()).isEqualTo(originalOther);
        assertThat(board.getId()).isEqualTo(ID);
        assertThat(board.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n"})
    @NullSource
    @DisplayName("name을 null이나 빈 문자열·공백으로 수정하면 예외를 던지고 기존 상태를 유지한다")
    void rejectsInvalidNameWithoutChangingState(String value) {
        Board board = Board.of(ID, "게시판", "설명", CREATED_AT);
        board.updateName("기존 값", CREATED_AT.plusSeconds(1));

        assertThatThrownBy(() -> board.updateName(value, CREATED_AT.plusSeconds(2)))
                .isInstanceOf(
                        value == null
                                ? NullPointerException.class
                                : IllegalValueConditionException.class);

        assertThat(board.getName()).isEqualTo("기존 값");
        assertThat(board.getUpdatedAt()).isEqualTo(CREATED_AT.plusSeconds(1));
    }

    @Test
    @DisplayName("name을 최대 길이보다 길게 수정하면 예외를 던지고 기존 상태를 유지한다")
    void rejectsNameExceedingMaxLengthWithoutChangingState() {
        Board board = Board.of(ID, "게시판", "설명", CREATED_AT);
        String original = board.getName();

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> board.updateName("a".repeat(51), CREATED_AT.plusSeconds(1)));

        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AT_MOST);
        assertThat(exception.subject()).isEqualTo(51);
        assertThat(exception.reference()).isEqualTo(50);
        assertThat(board.getName()).isEqualTo(original);
        assertThat(board.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("name의 수정 시각이 생성 시각과 같으면 수정을 허용한다")
    void updatesNameAtCreationTime() {
        Board board = Board.of(ID, "게시판", "설명", CREATED_AT);

        board.updateName("수정 값", CREATED_AT);

        assertThat(board.getName()).isEqualTo("수정 값");
        assertThat(board.getUpdatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("name의 수정 시각이 생성 시각보다 1나노초 빠르면 예외를 던지고 기존 상태를 유지한다")
    void rejectsNameUpdateBeforeCreationWithoutChangingState() {
        Board board = Board.of(ID, "게시판", "설명", CREATED_AT);
        board.updateName("기존 값", CREATED_AT.plusSeconds(1));
        Instant invalidTime = CREATED_AT.minusNanos(1);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> board.updateName("수정 값", invalidTime));

        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AFTER_OR_EQUAL);
        assertThat(exception.subject()).isEqualTo(invalidTime);
        assertThat(exception.reference()).isEqualTo(CREATED_AT);
        assertThat(board.getName()).isEqualTo("기존 값");
        assertThat(board.getUpdatedAt()).isEqualTo(CREATED_AT.plusSeconds(1));
    }

    @Test
    @DisplayName("name의 수정 시각이 null이면 예외를 던지고 기존 상태를 유지한다")
    void rejectsNameUpdateWithNullTimeWithoutChangingState() {
        Board board = Board.of(ID, "게시판", "설명", CREATED_AT);
        String original = board.getName();

        assertThatThrownBy(() -> board.updateName("수정 값", null))
                .isInstanceOf(NullPointerException.class);

        assertThat(board.getName()).isEqualTo(original);
        assertThat(board.getUpdatedAt()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 300})
    @DisplayName("description을 최소·최대 길이로 수정하면 해당 값과 수정 시각을 저장하고 나머지 필드를 유지한다")
    void updatesDescriptionAtLengthBoundaries(int length) {
        Board board = Board.of(ID, "게시판", "설명", CREATED_AT);
        String originalOther = board.getName();
        String value = "a".repeat(length);
        Instant updatedAt = CREATED_AT.plusSeconds(1);

        board.updateDescription(value, updatedAt);

        assertThat(board.getDescription()).isEqualTo(value);
        assertThat(board.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(board.getName()).isEqualTo(originalOther);
        assertThat(board.getId()).isEqualTo(ID);
        assertThat(board.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n"})
    @NullSource
    @DisplayName("description을 null이나 빈 문자열·공백으로 수정하면 예외를 던지고 기존 상태를 유지한다")
    void rejectsInvalidDescriptionWithoutChangingState(String value) {
        Board board = Board.of(ID, "게시판", "설명", CREATED_AT);
        board.updateDescription("기존 값", CREATED_AT.plusSeconds(1));

        assertThatThrownBy(() -> board.updateDescription(value, CREATED_AT.plusSeconds(2)))
                .isInstanceOf(
                        value == null
                                ? NullPointerException.class
                                : IllegalValueConditionException.class);

        assertThat(board.getDescription()).isEqualTo("기존 값");
        assertThat(board.getUpdatedAt()).isEqualTo(CREATED_AT.plusSeconds(1));
    }

    @Test
    @DisplayName("description을 최대 길이보다 길게 수정하면 예외를 던지고 기존 상태를 유지한다")
    void rejectsDescriptionExceedingMaxLengthWithoutChangingState() {
        Board board = Board.of(ID, "게시판", "설명", CREATED_AT);
        String original = board.getDescription();

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> board.updateDescription("a".repeat(301), CREATED_AT.plusSeconds(1)));

        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AT_MOST);
        assertThat(exception.subject()).isEqualTo(301);
        assertThat(exception.reference()).isEqualTo(300);
        assertThat(board.getDescription()).isEqualTo(original);
        assertThat(board.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("description의 수정 시각이 생성 시각과 같으면 수정을 허용한다")
    void updatesDescriptionAtCreationTime() {
        Board board = Board.of(ID, "게시판", "설명", CREATED_AT);

        board.updateDescription("수정 값", CREATED_AT);

        assertThat(board.getDescription()).isEqualTo("수정 값");
        assertThat(board.getUpdatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("description의 수정 시각이 생성 시각보다 1나노초 빠르면 예외를 던지고 기존 상태를 유지한다")
    void rejectsDescriptionUpdateBeforeCreationWithoutChangingState() {
        Board board = Board.of(ID, "게시판", "설명", CREATED_AT);
        board.updateDescription("기존 값", CREATED_AT.plusSeconds(1));
        Instant invalidTime = CREATED_AT.minusNanos(1);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> board.updateDescription("수정 값", invalidTime));

        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AFTER_OR_EQUAL);
        assertThat(exception.subject()).isEqualTo(invalidTime);
        assertThat(exception.reference()).isEqualTo(CREATED_AT);
        assertThat(board.getDescription()).isEqualTo("기존 값");
        assertThat(board.getUpdatedAt()).isEqualTo(CREATED_AT.plusSeconds(1));
    }

    @Test
    @DisplayName("description의 수정 시각이 null이면 예외를 던지고 기존 상태를 유지한다")
    void rejectsDescriptionUpdateWithNullTimeWithoutChangingState() {
        Board board = Board.of(ID, "게시판", "설명", CREATED_AT);
        String original = board.getDescription();

        assertThatThrownBy(() -> board.updateDescription("수정 값", null))
                .isInstanceOf(NullPointerException.class);

        assertThat(board.getDescription()).isEqualTo(original);
        assertThat(board.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("삭제 시각으로 게시판을 삭제하면 삭제 시각을 저장하고 나머지 필드를 유지한다")
    void deletesBoardAndKeepsOtherFields() {
        Board board = Board.of(ID, "게시판", "설명", CREATED_AT);
        board.updateName("기존 값", CREATED_AT.plusSeconds(1));
        Instant deletedAt = CREATED_AT.plusSeconds(10);

        board.delete(deletedAt);

        assertThat(board.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(board.isDeleted()).isTrue();
        assertThat(board.getName()).isEqualTo("기존 값");
        assertThat(board.getUpdatedAt()).isEqualTo(CREATED_AT.plusSeconds(1));
        assertThat(board.getDescription()).isEqualTo("설명");
        assertThat(board.getId()).isEqualTo(ID);
        assertThat(board.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("삭제 시각이 생성 시각과 같으면 삭제를 허용한다")
    void allowsDeletionAtCreationTime() {
        Board board = Board.of(ID, "게시판", "설명", CREATED_AT);

        board.delete(CREATED_AT);

        assertThat(board.getDeletedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("삭제 시각이 생성 시각보다 1나노초 빠르면 예외를 던지고 삭제되지 않는다")
    void rejectsDeletionBeforeCreationWithoutChangingState() {
        Board board = Board.of(ID, "게시판", "설명", CREATED_AT);
        Instant invalidTime = CREATED_AT.minusNanos(1);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class, () -> board.delete(invalidTime));

        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AFTER_OR_EQUAL);
        assertThat(exception.subject()).isEqualTo(invalidTime);
        assertThat(exception.reference()).isEqualTo(CREATED_AT);
        assertThat(board.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("삭제 시각이 null이면 예외를 던지고 삭제되지 않는다")
    void rejectsDeletionWithNullTimeWithoutChangingState() {
        Board board = Board.of(ID, "게시판", "설명", CREATED_AT);

        assertThatThrownBy(() -> board.delete(null)).isInstanceOf(NullPointerException.class);

        assertThat(board.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("이미 삭제된 게시판을 다시 삭제하면 IllegalStateException을 던지고 첫 삭제 시각을 유지한다")
    void rejectsDeletingAlreadyDeletedBoardWithoutChangingDeletedAt() {
        Board board = Board.of(ID, "게시판", "설명", CREATED_AT);
        Instant firstDeletedAt = CREATED_AT.plusSeconds(10);
        board.delete(firstDeletedAt);

        assertThatThrownBy(() -> board.delete(CREATED_AT.plusSeconds(20)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Board는 이미 삭제되었습니다.");

        assertThat(board.getDeletedAt()).isEqualTo(firstDeletedAt);
    }
}
