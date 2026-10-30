package com.lilamaris.lilboard.identity.web;

import com.lilamaris.lilboard.identity.application.exception.EmailAlreadyRegisteredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class AuthExceptionHandler {
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail unauthorized(AuthenticationException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "인증 정보가 일치하지 않습니다.");
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ProblemDetail duplicate(EmailAlreadyRegisteredException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }
}
