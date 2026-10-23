package com.lilamaris.lilboard.board.web;

import com.lilamaris.lilboard.board.application.port.in.CreatePostUseCase;
import com.lilamaris.lilboard.board.application.port.in.command.CreatePostCommand;
import com.lilamaris.lilboard.board.application.port.in.result.CreatePostResult;
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

@Tag(name = "게시글", description = "게시글 관리 API")
@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final CreatePostUseCase createPostUseCase;

    public PostController(CreatePostUseCase createPostUseCase) {
        this.createPostUseCase = createPostUseCase;
    }

    @Operation(
            summary = "게시글 생성",
            description = "게시판 ID와 제목, 본문으로 게시글을 생성합니다. ID와 생성 시각은 서버에서 지정합니다.",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            description =
                                    "게시판 ID, 제목, 본문은 필수이며 공백만 입력할 수 없습니다. 제목은 최대 300자, 본문은 최대 50000자입니다.",
                            content =
                                    @Content(
                                            mediaType = "application/json",
                                            schema =
                                                    @Schema(
                                                            implementation =
                                                                    CreatePostCommand.class),
                                            examples =
                                                    @ExampleObject(
                                                            value =
                                                                    """
                                    {"boardId":"00000000-0000-0000-0000-000000000001","title":"게시글 제목","content":"첫 번째 줄\\n두 번째 줄"}
                                    """))))
    @ApiResponse(
            responseCode = "201",
            description = "게시글 생성 성공",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreatePostResult.class),
                            examples =
                                    @ExampleObject(
                                            value =
                                                    """
                            {"id":"00000000-0000-0000-0000-000000000001","boardId":"00000000-0000-0000-0000-000000000001","title":"게시글 제목","content":"첫 번째 줄\\n두 번째 줄","createdAt":"2026-01-01T00:00:00Z"}
                            """)))
    @ApiResponse(
            responseCode = "400",
            description = "필수 값 누락, 공백, 최대 길이 초과 또는 잘못된 JSON 요청",
            content = @Content)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatePostResult create(@Valid @RequestBody CreatePostCommand command) {
        return createPostUseCase.create(command);
    }
}
