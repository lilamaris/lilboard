package com.lilamaris.lilboard.board.web;

import com.lilamaris.lilboard.board.application.port.in.CreateBoardUseCase;
import com.lilamaris.lilboard.board.application.port.in.FindBoardUseCase;
import com.lilamaris.lilboard.board.application.port.in.FindBoardsUseCase;
import com.lilamaris.lilboard.board.application.port.in.UpdateBoardUseCase;
import com.lilamaris.lilboard.board.application.port.in.command.CreateBoardCommand;
import com.lilamaris.lilboard.board.application.port.in.command.UpdateBoardCommand;
import com.lilamaris.lilboard.board.application.port.in.query.FindBoardQuery;
import com.lilamaris.lilboard.board.application.port.in.query.FindBoardsQuery;
import com.lilamaris.lilboard.board.application.port.in.result.CreateBoardResult;
import com.lilamaris.lilboard.board.application.port.in.result.FindBoardResult;
import com.lilamaris.lilboard.board.application.port.in.result.UpdateBoardResult;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "게시판", description = "게시판 관리 API")
@RestController
@RequestMapping("/api/boards")
public class BoardController {
    private final CreateBoardUseCase createBoardUseCase;
    private final UpdateBoardUseCase updateBoardUseCase;
    private final FindBoardUseCase findBoardUseCase;
    private final FindBoardsUseCase findBoardsUseCase;

    public BoardController(
            CreateBoardUseCase createBoardUseCase,
            UpdateBoardUseCase updateBoardUseCase,
            FindBoardUseCase findBoardUseCase,
            FindBoardsUseCase findBoardsUseCase) {
        this.createBoardUseCase = createBoardUseCase;
        this.updateBoardUseCase = updateBoardUseCase;
        this.findBoardUseCase = findBoardUseCase;
        this.findBoardsUseCase = findBoardsUseCase;
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

    @Operation(
            summary = "게시판 수정",
            description = "게시판의 이름과 설명을 수정합니다. 이름은 최대 50자, 설명은 최대 300자이며 공백만 입력할 수 없습니다.")
    @ApiResponse(
            responseCode = "200",
            description = "게시판 수정 성공",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UpdateBoardResult.class)))
    @ApiResponse(
            responseCode = "400",
            description = "잘못된 게시판 ID, 필수 값 누락, 공백, 최대 길이 초과 또는 잘못된 JSON 요청",
            content = @Content)
    @PutMapping("/{boardId}")
    public UpdateBoardResult update(
            @PathVariable("boardId") UUID boardId, @Valid @RequestBody UpdateBoardRequest request) {
        return updateBoardUseCase.update(
                new UpdateBoardCommand(boardId, request.name(), request.description()));
    }

    @Operation(summary = "게시판 단건 조회", description = "게시판 ID로 게시판을 조회합니다.")
    @ApiResponse(
            responseCode = "200",
            description = "게시판 조회 성공",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FindBoardResult.class)))
    @ApiResponse(responseCode = "400", description = "잘못된 게시판 ID", content = @Content)
    @ApiResponse(responseCode = "404", description = "게시판이 존재하지 않음", content = @Content)
    @GetMapping("/{boardId}")
    public FindBoardResult find(@PathVariable("boardId") UUID boardId) {
        try {
            return findBoardUseCase.find(new FindBoardQuery(boardId));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    @Operation(
            summary = "게시판 목록 조회",
            description =
                    "생성 시각과 ID 내림차순으로 게시판을 조회합니다. page는 0부터 시작하며 기본값은 0, size는 기본값 20인 양수입니다. offset(page × size)은 2147483647 이하여야 합니다.")
    @ApiResponse(responseCode = "200", description = "게시판 목록 조회 성공")
    @ApiResponse(
            responseCode = "400",
            description = "잘못된 페이지 번호·크기 또는 offset 범위 초과",
            content = @Content)
    @GetMapping
    public OffsetPage<FindBoardResult> findAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        OffsetPageRequest pageRequest;
        try {
            pageRequest = OffsetPageRequest.of(page, size);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 페이지 요청입니다.", exception);
        }
        return findBoardsUseCase.find(new FindBoardsQuery(pageRequest));
    }
}
