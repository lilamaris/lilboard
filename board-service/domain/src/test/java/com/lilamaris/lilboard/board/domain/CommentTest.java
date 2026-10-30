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

@DisplayName("Comment 테스트")
class CommentTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID POST_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @DisplayName("댓글을 생성하면 입력한 값을 저장하고 수정 시각과 삭제 시각은 null이다")
    void createsCommentWithGivenValues() {
        Comment comment = Comment.of(ID, POST_ID, "첫 번째 줄\n두 번째 줄", CREATED_AT);

        assertThat(comment.isDeleted()).isFalse();
        assertThat(comment.getId()).isEqualTo(ID);
        assertThat(comment.getPostId()).isEqualTo(POST_ID);
        assertThat(comment.getContent()).isEqualTo("첫 번째 줄\n두 번째 줄");
        assertThat(comment.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(comment.getUpdatedAt()).isNull();
        assertThat(comment.getDeletedAt()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5000})
    @DisplayName("본문이 최소·최대 길이이면 댓글을 생성하고 본문을 그대로 저장한다")
    void createsCommentWithContentAtLengthBoundaries(int length) {
        String content = "나".repeat(length);

        Comment comment = Comment.of(ID, POST_ID, content, CREATED_AT);

        assertThat(comment.getContent()).isEqualTo(content);
    }

    @Test
    @DisplayName("본문이 최대 길이 5000자를 초과하면 댓글 생성 시 예외를 던진다")
    void rejectsContentExceedingMaxLength() {
        String content = "나".repeat(5001);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> Comment.of(ID, POST_ID, content, CREATED_AT));

        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AT_MOST);
        assertThat(exception.subject()).isEqualTo(5001);
        assertThat(exception.reference()).isEqualTo(5000);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n"})
    @DisplayName("본문이 빈 문자열이나 공백이면 댓글 생성 시 예외를 던진다")
    void rejectsBlankContent(String content) {
        IllegalValueConditionException exception =
                catchThrowableOfType(
                        IllegalValueConditionException.class,
                        () -> Comment.of(ID, POST_ID, content, CREATED_AT));

        assertThat(exception.condition()).isEqualTo(StringCondition.NOT_BLANK);
        assertThat(exception.subject()).isEqualTo(content);
    }

    @Test
    @DisplayName("댓글 ID가 null이면 댓글 생성 시 NullPointerException을 던진다")
    void rejectsNullId() {
        assertThatThrownBy(() -> Comment.of(null, POST_ID, "본문", CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("게시글 ID가 null이면 댓글 생성 시 NullPointerException을 던진다")
    void rejectsNullPostId() {
        assertThatThrownBy(() -> Comment.of(ID, null, "본문", CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("본문이 null이면 댓글 생성 시 NullPointerException을 던진다")
    void rejectsNullContent() {
        assertThatThrownBy(() -> Comment.of(ID, POST_ID, null, CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("생성 시각이 null이면 댓글 생성 시 NullPointerException을 던진다")
    void rejectsNullCreatedAt() {
        assertThatThrownBy(() -> Comment.of(ID, POST_ID, "본문", null))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5000})
    @DisplayName("본문을 최소·최대 길이로 수정하면 본문과 수정 시각을 저장하고 나머지 필드를 유지한다")
    void updatesContentAtLengthBoundaries(int length) {
        Comment comment = Comment.of(ID, POST_ID, "기존 본문", CREATED_AT);
        String content = "나".repeat(length);
        Instant updatedAt = CREATED_AT.plusSeconds(1);

        comment.updateContent(content, updatedAt);

        assertThat(comment.getContent()).isEqualTo(content);
        assertThat(comment.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(comment.getId()).isEqualTo(ID);
        assertThat(comment.getPostId()).isEqualTo(POST_ID);
        assertThat(comment.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("본문의 수정 시각이 생성 시각과 같으면 수정을 허용한다")
    void updatesContentAtCreationTime() {
        Comment comment = Comment.of(ID, POST_ID, "본문", CREATED_AT);

        comment.updateContent("새 본문", CREATED_AT);

        assertThat(comment.getContent()).isEqualTo("새 본문");
        assertThat(comment.getUpdatedAt()).isEqualTo(CREATED_AT);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t\n"})
    @DisplayName("본문을 null이나 빈 문자열·공백으로 수정하면 예외를 던지고 기존 상태를 유지한다")
    void rejectsInvalidContentWithoutChangingState(String content) {
        Comment comment = Comment.of(ID, POST_ID, "기존 본문", CREATED_AT);
        Instant previousUpdate = CREATED_AT.plusSeconds(1);
        comment.updateContent("기존 본문", previousUpdate);

        assertThatThrownBy(() -> comment.updateContent(content, CREATED_AT.plusSeconds(2)))
                .isInstanceOf(
                        content == null
                                ? NullPointerException.class
                                : IllegalValueConditionException.class);

        assertThat(comment.getContent()).isEqualTo("기존 본문");
        assertThat(comment.getUpdatedAt()).isEqualTo(previousUpdate);
    }

    @Test
    @DisplayName("본문을 최대 길이 5000자보다 길게 수정하면 예외를 던지고 기존 상태를 유지한다")
    void rejectsContentUpdateExceedingMaxLengthWithoutChangingState() {
        Comment comment = Comment.of(ID, POST_ID, "기존 본문", CREATED_AT);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> comment.updateContent("나".repeat(5001), CREATED_AT.plusSeconds(1)));

        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AT_MOST);
        assertThat(exception.subject()).isEqualTo(5001);
        assertThat(exception.reference()).isEqualTo(5000);
        assertThat(comment.getContent()).isEqualTo("기존 본문");
        assertThat(comment.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("본문의 수정 시각이 생성 시각보다 1나노초 빠르면 예외를 던지고 기존 상태를 유지한다")
    void rejectsContentUpdateBeforeCreationWithoutChangingState() {
        Comment comment = Comment.of(ID, POST_ID, "기존 본문", CREATED_AT);
        Instant previousUpdate = CREATED_AT.plusSeconds(1);
        comment.updateContent("기존 본문", previousUpdate);
        Instant invalidTime = CREATED_AT.minusNanos(1);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> comment.updateContent("새 본문", invalidTime));

        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AFTER_OR_EQUAL);
        assertThat(exception.subject()).isEqualTo(invalidTime);
        assertThat(exception.reference()).isEqualTo(CREATED_AT);
        assertThat(comment.getContent()).isEqualTo("기존 본문");
        assertThat(comment.getUpdatedAt()).isEqualTo(previousUpdate);
    }

    @Test
    @DisplayName("본문의 수정 시각이 null이면 예외를 던지고 기존 상태를 유지한다")
    void rejectsContentUpdateWithNullTimeWithoutChangingState() {
        Comment comment = Comment.of(ID, POST_ID, "기존 본문", CREATED_AT);

        assertThatThrownBy(() -> comment.updateContent("새 본문", null))
                .isInstanceOf(NullPointerException.class);

        assertThat(comment.getContent()).isEqualTo("기존 본문");
        assertThat(comment.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("삭제 시각으로 댓글을 삭제하면 삭제 시각을 저장하고 나머지 필드를 유지한다")
    void deletesCommentAndKeepsOtherFields() {
        Comment comment = Comment.of(ID, POST_ID, "기존 본문", CREATED_AT);
        comment.updateContent("수정된 본문", CREATED_AT.plusSeconds(1));
        Instant deletedAt = CREATED_AT.plusSeconds(10);

        comment.delete(deletedAt);

        assertThat(comment.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(comment.isDeleted()).isTrue();
        assertThat(comment.getContent()).isEqualTo("수정된 본문");
        assertThat(comment.getUpdatedAt()).isEqualTo(CREATED_AT.plusSeconds(1));
        assertThat(comment.getId()).isEqualTo(ID);
        assertThat(comment.getPostId()).isEqualTo(POST_ID);
        assertThat(comment.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("삭제 시각이 생성 시각과 같으면 삭제를 허용한다")
    void allowsDeletionAtCreationTime() {
        Comment comment = Comment.of(ID, POST_ID, "본문", CREATED_AT);

        comment.delete(CREATED_AT);

        assertThat(comment.getDeletedAt()).isEqualTo(CREATED_AT);
        assertThat(comment.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("삭제 시각이 생성 시각보다 1나노초 빠르면 예외를 던지고 삭제되지 않는다")
    void rejectsDeletionBeforeCreationWithoutChangingState() {
        Comment comment = Comment.of(ID, POST_ID, "본문", CREATED_AT);
        Instant invalidTime = CREATED_AT.minusNanos(1);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> comment.delete(invalidTime));

        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AFTER_OR_EQUAL);
        assertThat(exception.subject()).isEqualTo(invalidTime);
        assertThat(exception.reference()).isEqualTo(CREATED_AT);
        assertThat(comment.isDeleted()).isFalse();
        assertThat(comment.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("삭제 시각이 null이면 예외를 던지고 삭제되지 않는다")
    void rejectsDeletionWithNullTimeWithoutChangingState() {
        Comment comment = Comment.of(ID, POST_ID, "본문", CREATED_AT);

        assertThatThrownBy(() -> comment.delete(null)).isInstanceOf(NullPointerException.class);

        assertThat(comment.isDeleted()).isFalse();
        assertThat(comment.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("이미 삭제된 댓글을 다시 삭제하면 IllegalStateException을 던지고 첫 삭제 시각을 유지한다")
    void rejectsDeletingAlreadyDeletedCommentWithoutChangingDeletedAt() {
        Comment comment = Comment.of(ID, POST_ID, "본문", CREATED_AT);
        Instant firstDeletedAt = CREATED_AT.plusSeconds(10);
        comment.delete(firstDeletedAt);

        assertThatThrownBy(() -> comment.delete(CREATED_AT.plusSeconds(20)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("already deleted.");

        assertThat(comment.getDeletedAt()).isEqualTo(firstDeletedAt);
    }
}
