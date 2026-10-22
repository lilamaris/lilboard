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
}
