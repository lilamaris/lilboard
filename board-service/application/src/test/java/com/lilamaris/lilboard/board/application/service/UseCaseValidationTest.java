package com.lilamaris.lilboard.board.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.lilamaris.lilboard.board.application.port.in.*;
import com.lilamaris.lilboard.board.application.port.in.command.*;
import com.lilamaris.lilboard.board.application.port.in.query.FindBoardQuery;
import com.lilamaris.lilboard.board.application.port.out.*;
import com.lilamaris.lilboard.board.domain.Board;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

class UseCaseValidationTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private AnnotationConfigApplicationContext context;
    private BoardFindPort findPort;
    private BoardSavePort savePort;
    private UuidGenerator uuidGenerator;

    @BeforeEach
    void setUp() {
        findPort = mock(BoardFindPort.class);
        savePort = mock(BoardSavePort.class);
        uuidGenerator = mock(UuidGenerator.class);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(MethodValidationPostProcessor.class);
        context.registerBean(
                CreateBoardService.class,
                () -> new CreateBoardService(savePort, uuidGenerator, clock));
        context.registerBean(
                UpdateBoardService.class, () -> new UpdateBoardService(savePort, findPort, clock));
        context.registerBean(FindBoardService.class, () -> new FindBoardService(findPort));
        context.refresh();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @ParameterizedTest
    @MethodSource("useCases")
    @DisplayName("유스케이스 입력이 null이면 검증 예외를 던지고 외부 의존성을 호출하지 않는다")
    void rejectsNullInput(String useCase) {
        assertThatThrownBy(() -> invoke(useCase, null, null, null, true))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(findPort, savePort, uuidGenerator);
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    @DisplayName("생성·수정 이름이 누락·공백·길이 초과이면 검증 예외를 던진다")
    void rejectsInvalidName(String name) {
        for (String useCase : new String[] {"create", "update"}) {
            assertThatThrownBy(() -> invoke(useCase, ID, name, "설명", false))
                    .isInstanceOf(ConstraintViolationException.class);
        }
        verifyNoInteractions(findPort, savePort, uuidGenerator);
    }

    @ParameterizedTest
    @MethodSource("invalidDescriptions")
    @DisplayName("생성·수정 설명이 누락·공백·길이 초과이면 검증 예외를 던진다")
    void rejectsInvalidDescription(String description) {
        for (String useCase : new String[] {"create", "update"}) {
            assertThatThrownBy(() -> invoke(useCase, ID, "게시판", description, false))
                    .isInstanceOf(ConstraintViolationException.class);
        }
        verifyNoInteractions(findPort, savePort, uuidGenerator);
    }

    @ParameterizedTest
    @MethodSource("idUseCases")
    @DisplayName("조회·수정 게시판 ID가 null이면 검증 예외를 던진다")
    void rejectsNullBoardId(String useCase) {
        assertThatThrownBy(() -> invoke(useCase, null, "게시판", "설명", false))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(findPort, savePort, uuidGenerator);
    }

    @ParameterizedTest
    @MethodSource("useCases")
    @DisplayName("유효한 입력과 최대 길이이면 검증을 통과하고 결과를 반환한다")
    void acceptsValidInput(String useCase) {
        String name = "a".repeat(50);
        String description = "b".repeat(300);
        Board board = Board.of(ID, name, description, NOW);
        if (useCase.equals("create")) {
            when(uuidGenerator.generate()).thenReturn(ID);
        } else {
            when(findPort.findById(ID)).thenReturn(Optional.of(board));
        }
        if (!useCase.equals("find")) {
            when(savePort.save(any(Board.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
        }
        assertThat(invoke(useCase, ID, name, description, false)).isNotNull();
    }

    private Object invoke(
            String useCase, UUID id, String name, String description, boolean nullInput) {
        return switch (useCase) {
            case "create" ->
                    context.getBean(CreateBoardUseCase.class)
                            .create(nullInput ? null : new CreateBoardCommand(name, description));
            case "update" ->
                    context.getBean(UpdateBoardUseCase.class)
                            .update(
                                    nullInput
                                            ? null
                                            : new UpdateBoardCommand(id, name, description));
            case "find" ->
                    context.getBean(FindBoardUseCase.class)
                            .find(nullInput ? null : new FindBoardQuery(id));
            default -> throw new IllegalArgumentException(useCase);
        };
    }

    static Stream<String> useCases() {
        return Stream.of("create", "update", "find");
    }

    static Stream<String> idUseCases() {
        return Stream.of("update", "find");
    }

    static Stream<String> invalidNames() {
        return Stream.of(null, "", " ", "a".repeat(51));
    }

    static Stream<String> invalidDescriptions() {
        return Stream.of(null, "", " ", "b".repeat(301));
    }
}
