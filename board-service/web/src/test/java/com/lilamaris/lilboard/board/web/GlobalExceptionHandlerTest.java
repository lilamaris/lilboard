package com.lilamaris.lilboard.board.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lilamaris.lilboard.board.application.port.in.command.CreateBoardCommand;
import com.lilamaris.lilboard.kernel.exception.IllegalBoundsException;
import com.lilamaris.lilboard.kernel.pagination.OffsetPageRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {
    private ValidatorFactory validatorFactory;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        mvc =
                MockMvcBuilders.standaloneSetup(
                                new ExceptionFixtureController(validatorFactory.getValidator()))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("application 계층의 IllegalArgumentException은 404와 ProblemDetail로 변환된다")
    void mapsIllegalArgumentToNotFound() throws Exception {
        mvc.perform(get("/exception/illegal-argument"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("요청한 대상을 찾을 수 없습니다"))
                .andExpect(jsonPath("$.detail").value("Board를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.instance").value("/exception/illegal-argument"));
    }

    @Test
    @DisplayName("kernel 조건 위반은 404가 아니라 400으로 변환되고 위반 내용을 설명에 담는다")
    void mapsConditionViolationToBadRequest() throws Exception {
        mvc.perform(get("/exception/condition"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("잘못된 요청"))
                .andExpect(jsonPath("$.detail").value("must be non-negative."));
    }

    @Test
    @DisplayName("jakarta 메서드 검증 위반은 400으로 변환되고 field별 메시지를 errors로 노출한다")
    void mapsConstraintViolationToBadRequestWithErrors() throws Exception {
        mvc.perform(get("/exception/constraint-violation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("잘못된 요청"))
                .andExpect(jsonPath("$.detail").value("요청값이 유효하지 않습니다."))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
    }

    @Test
    @DisplayName("기준 정의 오류는 요청 잘못이 아니라 서버 결함으로 500을 반환한다")
    void mapsBoundsViolationToInternalServerError() throws Exception {
        mvc.perform(get("/exception/bounds"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("서버 오류"))
                .andExpect(jsonPath("$.detail").value("서버 내부 오류가 발생했습니다."));
    }

    @Test
    @DisplayName("알 수 없는 예외는 500으로 변환되고 내부 메시지는 노출하지 않는다")
    void hidesInternalMessageForUnexpectedException() throws Exception {
        mvc.perform(get("/exception/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("서버 오류"))
                .andExpect(jsonPath("$.detail").value("서버 내부 오류가 발생했습니다."));
    }

    @Test
    @DisplayName("ResponseStatusException은 예외가 지정한 상태와 이유를 그대로 사용한다")
    void keepsStatusOfResponseStatusException() throws Exception {
        mvc.perform(get("/exception/response-status"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("잘못된 요청"))
                .andExpect(jsonPath("$.detail").value("요청이 충돌했습니다."));
    }

    @Test
    @DisplayName("404 ResponseStatusException은 조회하지 못함을 뜻하는 제목을 쓴다")
    void mapsResponseStatusNotFoundToNotFoundTitle() throws Exception {
        mvc.perform(get("/exception/response-status-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("요청한 대상을 찾을 수 없습니다"))
                .andExpect(jsonPath("$.detail").value("Post를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("5xx ResponseStatusException은 내부 이유를 클라이언트에 노출하지 않는다")
    void hidesReasonOfServerSideResponseStatus() throws Exception {
        mvc.perform(get("/exception/response-status-server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("서버 오류"))
                .andExpect(jsonPath("$.detail").value("서버 내부 오류가 발생했습니다."));
    }

    @Test
    @DisplayName("프레임워크 ErrorResponse의 상태를 500으로 뒤집지 않는다")
    void keepsStatusOfFrameworkErrorResponse() throws Exception {
        mvc.perform(get("/exception/no-resource"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("요청한 대상을 찾을 수 없습니다"))
                .andExpect(jsonPath("$.detail").value("No static resource missing.png."));
    }

    @Test
    @DisplayName("경로 변수 형식 오류는 한국어 ProblemDetail로 400을 반환한다")
    void mapsTypeMismatchToBadRequest() throws Exception {
        mvc.perform(get("/exception/type-mismatch/{id}", "invalid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("잘못된 요청"))
                .andExpect(jsonPath("$.detail").value("경로·쿼리 값의 형식이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("메서드 파라미터 검증 위반은 500이 아니라 400으로 변환된다")
    void mapsMethodValidationToBadRequest() throws Exception {
        mvc.perform(get("/exception/method-validation").param("count", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("잘못된 요청"))
                .andExpect(jsonPath("$.detail").value("요청값이 유효하지 않습니다."));
    }

    @RestController
    @RequestMapping("/exception")
    static class ExceptionFixtureController {
        private final Validator validator;

        ExceptionFixtureController(Validator validator) {
            this.validator = validator;
        }

        @GetMapping("/illegal-argument")
        void illegalArgument() {
            throw new IllegalArgumentException("Board를 찾을 수 없습니다.");
        }

        @GetMapping("/condition")
        void condition() {
            OffsetPageRequest.of(-1, 20);
        }

        @GetMapping("/constraint-violation")
        void constraintViolation() {
            throw new ConstraintViolationException(
                    validator.validate(new CreateBoardCommand(" ", "설명")));
        }

        @GetMapping("/bounds")
        void bounds() {
            throw new IllegalBoundsException(2, 1);
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("내부 상태 오류");
        }

        @GetMapping("/response-status")
        void responseStatus() {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "요청이 충돌했습니다.");
        }

        @GetMapping("/response-status-not-found")
        void responseStatusNotFound() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post를 찾을 수 없습니다.");
        }

        @GetMapping("/response-status-server-error")
        void responseStatusServerError() {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "내부 상태 오류");
        }

        @GetMapping("/no-resource")
        void noResource() throws NoResourceFoundException {
            throw new NoResourceFoundException(
                    HttpMethod.GET, "/exception/no-resource", "missing.png");
        }

        @GetMapping("/type-mismatch/{id}")
        void typeMismatch(@PathVariable("id") UUID id) {}

        @GetMapping("/method-validation")
        void methodValidation(@RequestParam(name = "count") @Valid @Positive int count) {}
    }
}
