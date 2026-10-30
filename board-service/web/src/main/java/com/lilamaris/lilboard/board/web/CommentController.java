package com.lilamaris.lilboard.board.web;

import com.lilamaris.lilboard.board.application.port.in.CreateCommentUseCase;
import com.lilamaris.lilboard.board.application.port.in.DeleteCommentUseCase;
import com.lilamaris.lilboard.board.application.port.in.FindCommentUseCase;
import com.lilamaris.lilboard.board.application.port.in.FindCommentsUseCase;
import com.lilamaris.lilboard.board.application.port.in.UpdateCommentUseCase;
import com.lilamaris.lilboard.board.application.port.in.command.CreateCommentCommand;
import com.lilamaris.lilboard.board.application.port.in.command.DeleteCommentCommand;
import com.lilamaris.lilboard.board.application.port.in.command.UpdateCommentCommand;
import com.lilamaris.lilboard.board.application.port.in.query.FindCommentQuery;
import com.lilamaris.lilboard.board.application.port.in.query.FindCommentsQuery;
import com.lilamaris.lilboard.board.application.port.in.result.CreateCommentResult;
import com.lilamaris.lilboard.board.application.port.in.result.FindCommentResult;
import com.lilamaris.lilboard.board.application.port.in.result.UpdateCommentResult;
import com.lilamaris.lilboard.kernel.pagination.Cursor;
import com.lilamaris.lilboard.kernel.pagination.CursorPage;
import com.lilamaris.lilboard.kernel.pagination.CursorPageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "댓글", description = "댓글 관리 API")
@RestController
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {
    private final CreateCommentUseCase createCommentUseCase;
    private final UpdateCommentUseCase updateCommentUseCase;
    private final DeleteCommentUseCase deleteCommentUseCase;
    private final FindCommentUseCase findCommentUseCase;
    private final FindCommentsUseCase findCommentsUseCase;

    public CommentController(
            CreateCommentUseCase createCommentUseCase,
            UpdateCommentUseCase updateCommentUseCase,
            DeleteCommentUseCase deleteCommentUseCase,
            FindCommentUseCase findCommentUseCase,
            FindCommentsUseCase findCommentsUseCase) {
        this.createCommentUseCase = createCommentUseCase;
        this.updateCommentUseCase = updateCommentUseCase;
        this.deleteCommentUseCase = deleteCommentUseCase;
        this.findCommentUseCase = findCommentUseCase;
        this.findCommentsUseCase = findCommentsUseCase;
    }

    @Operation(
            summary = "댓글 생성",
            description = "게시글 ID와 본문으로 댓글을 생성합니다. ID와 생성 시각은 서버에서 지정합니다. 조회 가능한 게시글에만 생성할 수 있습니다.")
    @ApiResponse(
            responseCode = "201",
            description = "댓글 생성 성공",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateCommentResult.class)))
    @ApiResponse(
            responseCode = "400",
            description = "필수 값 누락, 공백, 최대 길이 초과 또는 잘못된 JSON 요청",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "조회 가능한 게시글이 존재하지 않음",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateCommentResult create(
            @PathVariable("postId") UUID postId, @Valid @RequestBody CreateCommentRequest request) {
        return createCommentUseCase.create(new CreateCommentCommand(postId, request.content()));
    }

    @Operation(summary = "댓글 수정", description = "댓글의 본문을 수정합니다. 본문은 최대 5000자이며 공백만 입력할 수 없습니다.")
    @ApiResponse(
            responseCode = "200",
            description = "댓글 수정 성공",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UpdateCommentResult.class)))
    @ApiResponse(
            responseCode = "400",
            description = "잘못된 댓글 ID, 필수 값 누락, 공백, 최대 길이 초과 또는 잘못된 JSON 요청",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "조회 가능한 댓글이 존재하지 않음",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/{commentId}")
    public UpdateCommentResult update(
            @PathVariable("postId") UUID postId,
            @PathVariable("commentId") UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        return updateCommentUseCase.update(
                new UpdateCommentCommand(commentId, postId, request.content()));
    }

    @Operation(
            summary = "댓글 삭제",
            description = "댓글을 soft delete로 삭제합니다. 삭제 시각을 기록하고 이후 조회에서 제외합니다.")
    @ApiResponse(responseCode = "204", description = "댓글 삭제 성공")
    @ApiResponse(
            responseCode = "400",
            description = "잘못된 댓글 ID",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "조회 가능한 댓글이 존재하지 않음",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable("postId") UUID postId, @PathVariable("commentId") UUID commentId) {
        deleteCommentUseCase.delete(new DeleteCommentCommand(commentId));
    }

    @Operation(summary = "댓글 단건 조회", description = "댓글 ID로 댓글을 조회합니다.")
    @ApiResponse(
            responseCode = "200",
            description = "댓글 조회 성공",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FindCommentResult.class)))
    @ApiResponse(
            responseCode = "400",
            description = "잘못된 댓글 ID",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "댓글이 존재하지 않음",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{commentId}")
    public FindCommentResult find(
            @PathVariable("postId") UUID postId, @PathVariable("commentId") UUID commentId) {
        return findCommentUseCase.find(new FindCommentQuery(commentId));
    }

    @Operation(
            summary = "댓글 목록 조회",
            description = "생성 시각과 ID 내림차순으로 댓글을 조회합니다. 커서가 없으면 첫 페이지를 조회합니다. size는 기본값 20인 양수입니다.")
    @ApiResponse(responseCode = "200", description = "댓글 목록 조회 성공")
    @ApiResponse(
            responseCode = "400",
            description = "잘못된 커서 값 또는 페이지 크기",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping
    public CursorPage<FindCommentResult, UUID> findAll(
            @PathVariable("postId") UUID postId,
            @RequestParam(name = "cursorCreatedAt", required = false) Instant cursorCreatedAt,
            @RequestParam(name = "cursorId", required = false) UUID cursorId,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Cursor<UUID> cursor = cursorCreatedAt == null ? null : Cursor.of(cursorCreatedAt, cursorId);
        return findCommentsUseCase.find(
                new FindCommentsQuery(postId, CursorPageRequest.of(cursor, size)));
    }
}
