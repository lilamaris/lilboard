package com.lilamaris.lilboard.identity.web.controller;

import com.lilamaris.lilboard.identity.application.port.in.LoginUseCase;
import com.lilamaris.lilboard.identity.application.port.in.RegisterAccountUseCase;
import com.lilamaris.lilboard.identity.application.port.in.command.LoginCommand;
import com.lilamaris.lilboard.identity.application.port.in.command.RegisterAccountCommand;
import com.lilamaris.lilboard.identity.application.port.in.result.RegisterAccountResult;
import com.lilamaris.lilboard.identity.web.request.CredentialRequest;
import com.lilamaris.lilboard.identity.web.response.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "이메일·비밀번호 기반 회원가입 및 로그인 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final RegisterAccountUseCase registerAccountUseCase;
    private final LoginUseCase loginUseCase;

    @Operation(
            summary = "회원가입",
            description = "이메일과 비밀번호로 회원가입합니다. 이메일은 소문자로 정규화하고 사용자 ID는 서버에서 지정합니다.",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            description = "이메일은 최대 254자, 비밀번호는 8~128자이며 공백만 입력할 수 없습니다.",
                            content =
                                    @Content(
                                            mediaType = "application/json",
                                            schema =
                                                    @Schema(
                                                            implementation =
                                                                    CredentialRequest.class),
                                            examples =
                                                    @ExampleObject(
                                                            value =
                                                                    "{\"email\":\"user@example.com\",\"password\":\"password123\"}"))))
    @ApiResponse(
            responseCode = "201",
            description = "회원가입 성공",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RegisterAccountResult.class),
                            examples =
                                    @ExampleObject(
                                            value =
                                                    "{\"id\":\"00000000-0000-0000-0000-000000000001\"}")))
    @ApiResponse(
            responseCode = "400",
            description = "이메일 형식 오류, 비밀번호 길이 위반, 필수 값 누락 또는 잘못된 JSON 요청",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = "이미 등록된 이메일",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterAccountResult register(@Valid @RequestBody CredentialRequest request) {
        return registerAccountUseCase.register(
                new RegisterAccountCommand(request.email(), request.password()));
    }

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호를 검증하고 액세스 JWT를 발급합니다. 토큰 수명은 expiresIn에 초 단위로 반환합니다.",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            description = "가입한 이메일과 비밀번호를 전달합니다. 이메일은 최대 254자, 비밀번호는 8~128자입니다.",
                            content =
                                    @Content(
                                            mediaType = "application/json",
                                            schema =
                                                    @Schema(
                                                            implementation =
                                                                    CredentialRequest.class),
                                            examples =
                                                    @ExampleObject(
                                                            value =
                                                                    "{\"email\":\"user@example.com\",\"password\":\"password123\"}"))))
    @ApiResponse(
            responseCode = "200",
            description = "로그인 성공",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponse.class),
                            examples =
                                    @ExampleObject(
                                            value =
                                                    "{\"accessToken\":\"<JWT>\",\"tokenType\":\"Bearer\",\"expiresIn\":900}")))
    @ApiResponse(
            responseCode = "400",
            description = "필수 값 누락, 이메일 형식·비밀번호 길이 위반 또는 잘못된 JSON 요청",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "이메일 또는 비밀번호 불일치",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody CredentialRequest request) {
        return LoginResponse.from(
                loginUseCase.login(new LoginCommand(request.email(), request.password())));
    }
}
