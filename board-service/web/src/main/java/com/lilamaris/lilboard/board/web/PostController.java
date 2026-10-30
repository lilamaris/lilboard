package com.lilamaris.lilboard.board.web;

import com.lilamaris.lilboard.board.application.port.in.CreatePostUseCase;
import com.lilamaris.lilboard.board.application.port.in.FindPostUseCase;
import com.lilamaris.lilboard.board.application.port.in.FindPostsUseCase;
import com.lilamaris.lilboard.board.application.port.in.UpdatePostUseCase;
import com.lilamaris.lilboard.board.application.port.in.command.CreatePostCommand;
import com.lilamaris.lilboard.board.application.port.in.command.UpdatePostCommand;
import com.lilamaris.lilboard.board.application.port.in.query.FindPostQuery;
import com.lilamaris.lilboard.board.application.port.in.query.FindPostsQuery;
import com.lilamaris.lilboard.board.application.port.in.result.CreatePostResult;
import com.lilamaris.lilboard.board.application.port.in.result.FindPostResult;
import com.lilamaris.lilboard.board.application.port.in.result.UpdatePostResult;
import com.lilamaris.lilboard.kernel.pagination.OffsetPage;
import com.lilamaris.lilboard.kernel.pagination.OffsetPageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "게시글", description = "게시글 관리 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final CreatePostUseCase createPostUseCase;
    private final UpdatePostUseCase updatePostUseCase;
    private final FindPostUseCase findPostUseCase;
    private final FindPostsUseCase findPostsUseCase;

    @Operation(
            summary = "게시글 생성",
            description =
                    "게시판 ID와 제목, 본문으로 게시글을 생성합니다. ID와 생성 시각은 서버에서 지정합니다. 조회 가능한 게시판(삭제되지 않은 게시판)에만 생성할 수 있습니다.",
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
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "조회 가능한 게시판이 존재하지 않음",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatePostResult create(@Valid @RequestBody CreatePostCommand command) {
        return createPostUseCase.create(command);
    }

    @Operation(
            summary = "게시글 수정",
            description = "게시글의 제목과 본문을 수정합니다. 제목은 최대 300자, 본문은 최대 50000자이며 공백만 입력할 수 없습니다.")
    @ApiResponse(
            responseCode = "200",
            description = "게시글 수정 성공",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UpdatePostResult.class)))
    @ApiResponse(
            responseCode = "400",
            description = "잘못된 게시글 ID, 필수 값 누락, 공백, 최대 길이 초과 또는 잘못된 JSON 요청",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "조회 가능한 게시글이 존재하지 않음 (삭제된 게시판의 게시글 포함)",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/{postId}")
    public UpdatePostResult update(
            @PathVariable("postId") UUID postId, @Valid @RequestBody UpdatePostRequest request) {
        return updatePostUseCase.update(
                new UpdatePostCommand(postId, request.title(), request.content()));
    }

    @Operation(summary = "게시글 단건 조회", description = "게시글 ID로 게시글을 조회합니다. 삭제된 게시판의 게시글은 조회할 수 없습니다.")
    @ApiResponse(
            responseCode = "200",
            description = "게시글 조회 성공",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FindPostResult.class)))
    @ApiResponse(
            responseCode = "400",
            description = "잘못된 게시글 ID",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "게시글이 존재하지 않거나 삭제된 게시판에 소속된 게시글",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{postId}")
    public FindPostResult find(@PathVariable("postId") UUID postId) {
        return findPostUseCase.find(new FindPostQuery(postId));
    }

    @Operation(
            summary = "게시글 목록 조회",
            description =
                    "생성 시각과 ID 내림차순으로 게시글을 조회합니다. 삭제된 게시판에 소속된 게시글은 목록과 전체 개수에서 제외됩니다. page는 0부터 시작하며 기본값은 0, size는 기본값 20인 양수입니다. offset(page × size)은 2147483647 이하여야 합니다.")
    @ApiResponse(responseCode = "200", description = "게시글 목록 조회 성공")
    @ApiResponse(
            responseCode = "400",
            description = "잘못된 페이지 번호·크기 또는 offset 범위 초과",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping
    public OffsetPage<FindPostResult> findAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return findPostsUseCase.find(new FindPostsQuery(OffsetPageRequest.of(page, size)));
    }
}
