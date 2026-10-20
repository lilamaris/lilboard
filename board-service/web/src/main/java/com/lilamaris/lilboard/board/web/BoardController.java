package com.lilamaris.lilboard.board.web;

import com.lilamaris.lilboard.board.application.port.in.CreateBoardUseCase;
import com.lilamaris.lilboard.board.application.port.in.command.CreateBoardCommand;
import com.lilamaris.lilboard.board.application.port.in.result.CreateBoardResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "게시판", description = "게시판 관리 API")
@RestController
@RequestMapping("/api/boards")
public class BoardController {
    private final CreateBoardUseCase createBoardUseCase;

    public BoardController(CreateBoardUseCase createBoardUseCase) {
        this.createBoardUseCase = createBoardUseCase;
    }

    @Operation(
            summary = "게시판 생성",
            description = "이름과 설명으로 게시판을 생성합니다. ID와 생성 시각은 서버에서 지정합니다.",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            description =
                                    "이름과 설명은 필수이며 공백만 입력할 수 없습니다. 이름은 최대 50자, 설명은 최대 300자입니다.",
                            content =
                                    @Content(
                                            mediaType = "application/json",
                                            schema =
                                                    @Schema(
                                                            implementation =
                                                                    CreateBoardCommand.class),
                                            examples =
                                                    @ExampleObject(
                                                            value =
                                                                    """
                                    {"name":"자유게시판","description":"자유롭게 이야기를 나눠요"}
                                    """))))
    @ApiResponse(
            responseCode = "201",
            description = "게시판 생성 성공",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateBoardResult.class),
                            examples =
                                    @ExampleObject(
                                            value =
                                                    """
                            {"id":"00000000-0000-0000-0000-000000000001","name":"자유게시판","description":"자유롭게 이야기를 나눠요","createdAt":"2026-01-01T00:00:00Z"}
                            """)))
    @ApiResponse(
            responseCode = "400",
            description = "필수 값 누락, 공백, 최대 길이 초과 또는 잘못된 JSON 요청",
            content = @Content)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateBoardResult create(@Valid @RequestBody CreateBoardCommand command) {
        return createBoardUseCase.create(command);
    }
}
