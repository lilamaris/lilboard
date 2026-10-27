package com.lilamaris.lilboard.board.web;

import com.lilamaris.lilboard.kernel.exception.IllegalBoundsException;
import com.lilamaris.lilboard.kernel.exception.IllegalConditionException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.Path.Node;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * web 계층의 단일 예외 처리 진입점.
 *
 * <p>컨트롤러는 개별 try/catch로 HTTP 상태를 변환하지 않는다. 계층 경계에서 발생한 예외를 한곳으로 모으고 RFC 9457 ProblemDetail 응답으로
 * 변환한다. Spring MVC가 발생시키는 요청·바인딩 예외는 {@link ResponseEntityExceptionHandler}의 표준 처리를 따르고 한국어 제목·설명만
 * 교체한다.
 *
 * <p>매핑 기준:
 *
 * <ul>
 *   <li>{@link IllegalConditionException}: 요청값이 kernel 조건을 위반 → 400
 *   <li>{@link IllegalArgumentException}: application 계층의 대상 미존재 → 404
 *   <li>{@link IllegalBoundsException}: 기준 자체의 정의 오류(서버 결함) → 500
 *   <li>{@link ErrorResponseException}·{@link NoResourceFoundException}: 예외가 지정한 상태 → 해당 상태 (5xx
 *       이유는 노출하지 않는다)
 *   <li>그 외 예외 → 500, 내부 메시지는 노출하지 않는다
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String BAD_REQUEST_TITLE = "잘못된 요청";
    private static final String INVALID_REQUEST_DETAIL = "요청값이 유효하지 않습니다.";
    private static final String NOT_FOUND_TITLE = "요청한 대상을 찾을 수 없습니다";
    private static final String INTERNAL_ERROR_TITLE = "서버 오류";
    private static final String INTERNAL_ERROR_DETAIL = "서버 내부 오류가 발생했습니다.";

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(
            ConstraintViolationException exception, WebRequest request) {
        return respond(
                HttpStatus.BAD_REQUEST,
                BAD_REQUEST_TITLE,
                INVALID_REQUEST_DETAIL,
                violationErrors(exception),
                request,
                exception);
    }

    @ExceptionHandler(IllegalConditionException.class)
    public ResponseEntity<Object> handleIllegalCondition(
            IllegalConditionException exception, WebRequest request) {
        return respond(
                HttpStatus.BAD_REQUEST,
                BAD_REQUEST_TITLE,
                message(exception.getMessage()),
                List.of(),
                request,
                exception);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(
            IllegalArgumentException exception, WebRequest request) {
        return respond(
                HttpStatus.NOT_FOUND,
                NOT_FOUND_TITLE,
                message(exception.getMessage()),
                List.of(),
                request,
                exception);
    }

    @ExceptionHandler(IllegalBoundsException.class)
    public ResponseEntity<Object> handleIllegalBounds(
            IllegalBoundsException exception, WebRequest request) {
        return respond(
                HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_ERROR_TITLE,
                INTERNAL_ERROR_DETAIL,
                List.of(),
                request,
                exception);
    }

    @Override
    protected ResponseEntity<Object> handleErrorResponseException(
            ErrorResponseException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        // 5xx 이유는 내부 상태 정보일 수 있어 클라이언트로 전달하지 않는다.
        String detail =
                status.is5xxServerError()
                        ? INTERNAL_ERROR_DETAIL
                        : message(exception.getBody().getDetail());
        return respond(status, titleFor(status), detail, List.of(), request, exception);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return respond(
                status,
                titleFor(status),
                message(exception.getBody().getDetail()),
                List.of(),
                request,
                exception);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception exception, WebRequest request) {
        return respond(
                HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_ERROR_TITLE,
                INTERNAL_ERROR_DETAIL,
                List.of(),
                request,
                exception);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return respond(
                status,
                BAD_REQUEST_TITLE,
                INVALID_REQUEST_DETAIL,
                bindingErrors(exception.getBindingResult()),
                request,
                exception);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return respond(
                status, BAD_REQUEST_TITLE, INVALID_REQUEST_DETAIL, List.of(), request, exception);
    }

    @Override
    protected ResponseEntity<Object> handleMethodValidationException(
            MethodValidationException exception,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request) {
        return respond(
                HttpStatus.BAD_REQUEST,
                BAD_REQUEST_TITLE,
                INVALID_REQUEST_DETAIL,
                List.of(),
                request,
                exception);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return respond(
                status,
                BAD_REQUEST_TITLE,
                "경로·쿼리 값의 형식이 올바르지 않습니다.",
                List.of(),
                request,
                exception);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return respond(
                status, BAD_REQUEST_TITLE, "요청 본문을 읽을 수 없습니다.", List.of(), request, exception);
    }

    private ResponseEntity<Object> respond(
            HttpStatusCode status,
            String title,
            String detail,
            List<Map<String, String>> errors,
            WebRequest request,
            Exception exception) {
        log(status, exception);

        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(title);
        body.setInstance(instanceOf(request));
        if (!errors.isEmpty()) {
            body.setProperty("errors", errors);
        }
        return createResponseEntity(body, new HttpHeaders(), status, request);
    }

    private static void log(HttpStatusCode status, Exception exception) {
        if (status.is5xxServerError()) {
            log.error("요청 처리 중 서버 오류: {}", exception.getMessage(), exception);
        } else {
            log.debug("요청 처리 중 클라이언트 오류: status={}, {}", status.value(), exception.getMessage());
        }
    }

    private static String titleFor(HttpStatusCode status) {
        if (status.value() == HttpStatus.NOT_FOUND.value()) {
            return NOT_FOUND_TITLE;
        }
        if (status.is4xxClientError()) {
            return BAD_REQUEST_TITLE;
        }
        return INTERNAL_ERROR_TITLE;
    }

    private static URI instanceOf(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            try {
                return URI.create(servletRequest.getRequest().getRequestURI());
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private static List<Map<String, String>> bindingErrors(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(
                        error ->
                                Map.of(
                                        "field", error.getField(),
                                        "message", message(error.getDefaultMessage())))
                .toList();
    }

    private static List<Map<String, String>> violationErrors(
            ConstraintViolationException exception) {
        return exception.getConstraintViolations().stream()
                .map(GlobalExceptionHandler::violationEntry)
                .sorted(
                        Comparator.<Map<String, String>, String>comparing(
                                entry -> entry.get("field")))
                .toList();
    }

    private static Map<String, String> violationEntry(ConstraintViolation<?> violation) {
        return Map.of("field", fieldOf(violation), "message", message(violation.getMessage()));
    }

    /** 속성 경로의 마지막 노드를 필드 이름으로 쓰고, 빈 단위 제약에서는 루트 타입 이름을 쓴다. */
    private static String fieldOf(ConstraintViolation<?> violation) {
        Path path = violation.getPropertyPath();
        Node leaf = null;
        for (Node node : path) {
            leaf = node;
        }
        if (leaf == null) {
            return violation.getRootBeanClass().getSimpleName();
        }
        return leaf.getName().isEmpty() ? path.toString() : leaf.getName();
    }

    private static String message(String value) {
        return Objects.toString(value, "");
    }
}
