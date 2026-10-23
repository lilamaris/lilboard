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

class PostTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @DisplayName("게시글을 생성하면 입력한 값을 저장하고 수정 시각은 null이다")
    void createsPostWithGivenValues() {
        Post post = Post.of(ID, BOARD_ID, "게시글 제목", "첫 번째 줄\n두 번째 줄", CREATED_AT);

        assertThat(post.getId()).isEqualTo(ID);
        assertThat(post.getBoardId()).isEqualTo(BOARD_ID);
        assertThat(post.getTitle()).isEqualTo("게시글 제목");
        assertThat(post.getContent()).isEqualTo("첫 번째 줄\n두 번째 줄");
        assertThat(post.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(post.getUpdatedAt()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 300})
    @DisplayName("제목이 최소·최대 길이이면 게시글을 생성하고 제목을 그대로 저장한다")
    void createsPostWithTitleAtLengthBoundaries(int length) {
        String title = "가".repeat(length);

        Post post = Post.of(ID, BOARD_ID, title, "본문", CREATED_AT);

        assertThat(post.getTitle()).isEqualTo(title);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 50000})
    @DisplayName("본문이 최소·최대 길이이면 게시글을 생성하고 본문을 그대로 저장한다")
    void createsPostWithContentAtLengthBoundaries(int length) {
        String content = "나".repeat(length);

        Post post = Post.of(ID, BOARD_ID, "제목", content, CREATED_AT);

        assertThat(post.getContent()).isEqualTo(content);
    }

    @Test
    @DisplayName("제목이 최대 길이 300자를 초과하면 게시글 생성 시 예외를 던진다")
    void rejectsTitleExceedingMaxLength() {
        String title = "가".repeat(301);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> Post.of(ID, BOARD_ID, title, "본문", CREATED_AT));

        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AT_MOST);
        assertThat(exception.subject()).isEqualTo(301);
        assertThat(exception.reference()).isEqualTo(300);
    }

    @Test
    @DisplayName("본문이 최대 길이 50000자를 초과하면 게시글 생성 시 예외를 던진다")
    void rejectsContentExceedingMaxLength() {
        String content = "나".repeat(50001);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> Post.of(ID, BOARD_ID, "제목", content, CREATED_AT));

        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AT_MOST);
        assertThat(exception.subject()).isEqualTo(50001);
        assertThat(exception.reference()).isEqualTo(50000);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n"})
    @DisplayName("제목이 빈 문자열이나 공백이면 게시글 생성 시 예외를 던진다")
    void rejectsBlankTitle(String title) {
        IllegalValueConditionException exception =
                catchThrowableOfType(
                        IllegalValueConditionException.class,
                        () -> Post.of(ID, BOARD_ID, title, "본문", CREATED_AT));

        assertThat(exception.condition()).isEqualTo(StringCondition.NOT_BLANK);
        assertThat(exception.subject()).isEqualTo(title);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n"})
    @DisplayName("본문이 빈 문자열이나 공백이면 게시글 생성 시 예외를 던진다")
    void rejectsBlankContent(String content) {
        IllegalValueConditionException exception =
                catchThrowableOfType(
                        IllegalValueConditionException.class,
                        () -> Post.of(ID, BOARD_ID, "제목", content, CREATED_AT));

        assertThat(exception.condition()).isEqualTo(StringCondition.NOT_BLANK);
        assertThat(exception.subject()).isEqualTo(content);
    }

    @Test
    @DisplayName("게시글 ID가 null이면 게시글 생성 시 NullPointerException을 던진다")
    void rejectsNullId() {
        assertThatThrownBy(() -> Post.of(null, BOARD_ID, "제목", "본문", CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("게시판 ID가 null이면 게시글 생성 시 NullPointerException을 던진다")
    void rejectsNullBoardId() {
        assertThatThrownBy(() -> Post.of(ID, null, "제목", "본문", CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("제목이 null이면 게시글 생성 시 NullPointerException을 던진다")
    void rejectsNullTitle() {
        assertThatThrownBy(() -> Post.of(ID, BOARD_ID, null, "본문", CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("본문이 null이면 게시글 생성 시 NullPointerException을 던진다")
    void rejectsNullContent() {
        assertThatThrownBy(() -> Post.of(ID, BOARD_ID, "제목", null, CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("생성 시각이 null이면 게시글 생성 시 NullPointerException을 던진다")
    void rejectsNullCreatedAt() {
        assertThatThrownBy(() -> Post.of(ID, BOARD_ID, "제목", "본문", null))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 300})
    @DisplayName("제목을 최소·최대 길이로 수정하면 제목과 수정 시각을 저장하고 나머지 필드를 유지한다")
    void updatesTitleAtLengthBoundaries(int length) {
        Post post = Post.of(ID, BOARD_ID, "기존 제목", "기존 본문", CREATED_AT);
        String title = "가".repeat(length);
        Instant updatedAt = CREATED_AT.plusSeconds(1);

        post.updateTitle(title, updatedAt);

        assertThat(post.getTitle()).isEqualTo(title);
        assertThat(post.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(post.getContent()).isEqualTo("기존 본문");
        assertThat(post.getId()).isEqualTo(ID);
        assertThat(post.getBoardId()).isEqualTo(BOARD_ID);
        assertThat(post.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t\n"})
    @DisplayName("제목을 null이나 빈 문자열·공백으로 수정하면 예외를 던지고 기존 상태를 유지한다")
    void rejectsInvalidTitleWithoutChangingState(String title) {
        Post post = Post.of(ID, BOARD_ID, "제목", "본문", CREATED_AT);
        Instant previousUpdate = CREATED_AT.plusSeconds(1);
        post.updateTitle("기존 제목", previousUpdate);

        assertThatThrownBy(() -> post.updateTitle(title, CREATED_AT.plusSeconds(2)))
                .isInstanceOf(
                        title == null
                                ? NullPointerException.class
                                : IllegalValueConditionException.class);

        assertThat(post.getTitle()).isEqualTo("기존 제목");
        assertThat(post.getUpdatedAt()).isEqualTo(previousUpdate);
    }

    @Test
    @DisplayName("제목을 최대 길이 300자보다 길게 수정하면 예외를 던지고 기존 상태를 유지한다")
    void rejectsTitleUpdateExceedingMaxLengthWithoutChangingState() {
        Post post = Post.of(ID, BOARD_ID, "기존 제목", "본문", CREATED_AT);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> post.updateTitle("가".repeat(301), CREATED_AT.plusSeconds(1)));

        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AT_MOST);
        assertThat(exception.subject()).isEqualTo(301);
        assertThat(exception.reference()).isEqualTo(300);
        assertThat(post.getTitle()).isEqualTo("기존 제목");
        assertThat(post.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("제목의 수정 시각이 생성 시각과 같으면 수정을 허용한다")
    void updatesTitleAtCreationTime() {
        Post post = Post.of(ID, BOARD_ID, "제목", "본문", CREATED_AT);

        post.updateTitle("새 제목", CREATED_AT);

        assertThat(post.getTitle()).isEqualTo("새 제목");
        assertThat(post.getUpdatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("제목의 수정 시각이 생성 시각보다 1나노초 빠르면 예외를 던지고 기존 상태를 유지한다")
    void rejectsTitleUpdateBeforeCreationWithoutChangingState() {
        Post post = Post.of(ID, BOARD_ID, "제목", "본문", CREATED_AT);
        Instant previousUpdate = CREATED_AT.plusSeconds(1);
        post.updateTitle("기존 제목", previousUpdate);
        Instant invalidTime = CREATED_AT.minusNanos(1);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> post.updateTitle("새 제목", invalidTime));

        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AFTER_OR_EQUAL);
        assertThat(exception.subject()).isEqualTo(invalidTime);
        assertThat(exception.reference()).isEqualTo(CREATED_AT);
        assertThat(post.getTitle()).isEqualTo("기존 제목");
        assertThat(post.getUpdatedAt()).isEqualTo(previousUpdate);
    }

    @Test
    @DisplayName("제목의 수정 시각이 null이면 예외를 던지고 기존 상태를 유지한다")
    void rejectsTitleUpdateWithNullTimeWithoutChangingState() {
        Post post = Post.of(ID, BOARD_ID, "기존 제목", "본문", CREATED_AT);

        assertThatThrownBy(() -> post.updateTitle("새 제목", null))
                .isInstanceOf(NullPointerException.class);

        assertThat(post.getTitle()).isEqualTo("기존 제목");
        assertThat(post.getUpdatedAt()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 50000})
    @DisplayName("본문을 최소·최대 길이로 수정하면 본문과 수정 시각을 저장하고 나머지 필드를 유지한다")
    void updatesContentAtLengthBoundaries(int length) {
        Post post = Post.of(ID, BOARD_ID, "기존 제목", "기존 본문", CREATED_AT);
        String content = "나".repeat(length);
        Instant updatedAt = CREATED_AT.plusSeconds(1);

        post.updateContent(content, updatedAt);

        assertThat(post.getContent()).isEqualTo(content);
        assertThat(post.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(post.getTitle()).isEqualTo("기존 제목");
        assertThat(post.getId()).isEqualTo(ID);
        assertThat(post.getBoardId()).isEqualTo(BOARD_ID);
        assertThat(post.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t\n"})
    @DisplayName("본문을 null이나 빈 문자열·공백으로 수정하면 예외를 던지고 기존 상태를 유지한다")
    void rejectsInvalidContentWithoutChangingState(String content) {
        Post post = Post.of(ID, BOARD_ID, "제목", "본문", CREATED_AT);
        Instant previousUpdate = CREATED_AT.plusSeconds(1);
        post.updateContent("기존 본문", previousUpdate);

        assertThatThrownBy(() -> post.updateContent(content, CREATED_AT.plusSeconds(2)))
                .isInstanceOf(
                        content == null
                                ? NullPointerException.class
                                : IllegalValueConditionException.class);

        assertThat(post.getContent()).isEqualTo("기존 본문");
        assertThat(post.getUpdatedAt()).isEqualTo(previousUpdate);
    }

    @Test
    @DisplayName("본문을 최대 길이 50000자보다 길게 수정하면 예외를 던지고 기존 상태를 유지한다")
    void rejectsContentUpdateExceedingMaxLengthWithoutChangingState() {
        Post post = Post.of(ID, BOARD_ID, "제목", "기존 본문", CREATED_AT);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> post.updateContent("나".repeat(50001), CREATED_AT.plusSeconds(1)));

        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AT_MOST);
        assertThat(exception.subject()).isEqualTo(50001);
        assertThat(exception.reference()).isEqualTo(50000);
        assertThat(post.getContent()).isEqualTo("기존 본문");
        assertThat(post.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("본문의 수정 시각이 생성 시각과 같으면 수정을 허용한다")
    void updatesContentAtCreationTime() {
        Post post = Post.of(ID, BOARD_ID, "제목", "본문", CREATED_AT);

        post.updateContent("첫 번째 줄\n두 번째 줄", CREATED_AT);

        assertThat(post.getContent()).isEqualTo("첫 번째 줄\n두 번째 줄");
        assertThat(post.getUpdatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("본문의 수정 시각이 생성 시각보다 1나노초 빠르면 예외를 던지고 기존 상태를 유지한다")
    void rejectsContentUpdateBeforeCreationWithoutChangingState() {
        Post post = Post.of(ID, BOARD_ID, "제목", "본문", CREATED_AT);
        Instant previousUpdate = CREATED_AT.plusSeconds(1);
        post.updateContent("기존 본문", previousUpdate);
        Instant invalidTime = CREATED_AT.minusNanos(1);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> post.updateContent("새 본문", invalidTime));

        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AFTER_OR_EQUAL);
        assertThat(exception.subject()).isEqualTo(invalidTime);
        assertThat(exception.reference()).isEqualTo(CREATED_AT);
        assertThat(post.getContent()).isEqualTo("기존 본문");
        assertThat(post.getUpdatedAt()).isEqualTo(previousUpdate);
    }

    @Test
    @DisplayName("본문의 수정 시각이 null이면 예외를 던지고 기존 상태를 유지한다")
    void rejectsContentUpdateWithNullTimeWithoutChangingState() {
        Post post = Post.of(ID, BOARD_ID, "제목", "기존 본문", CREATED_AT);

        assertThatThrownBy(() -> post.updateContent("새 본문", null))
                .isInstanceOf(NullPointerException.class);

        assertThat(post.getContent()).isEqualTo("기존 본문");
        assertThat(post.getUpdatedAt()).isNull();
    }
}
