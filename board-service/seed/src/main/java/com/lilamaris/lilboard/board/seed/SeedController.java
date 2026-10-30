package com.lilamaris.lilboard.board.seed;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 부하 테스트용 시딩 엔드포인트.
 *
 * <p>{@code @Profile("loadtest")}와 {@code @RestController} 조합으로 loadtest 프로파일에서만 스테레오타입 스캔에 등록된다.
 * loadtest가 없는 프로파일에서는 컨트롤러와 접근 허용 빈이 등록되지 않아 요청이 기본 보안 정책으로 차단된다.
 */
@Profile("loadtest")
@Tag(name = "부하 테스트 시딩", description = "loadtest 프로파일 전용 시딩 API")
@RestController
@RequestMapping("/api/seed")
public class SeedController {
    private final SeedService seedService;

    public SeedController(SeedService seedService) {
        this.seedService = seedService;
    }

    @Operation(
            summary = "시딩 데이터 생성",
            description =
                    "게시판 boards개와 각 게시판의 게시글 postsPerBoard개를 생성한다. 개수는 쿼리 파라미터로 지정하고, 합산 행 수가 상한을 넘으면 400을 반환한다.")
    @ApiResponse(
            responseCode = "200",
            description = "시딩 성공",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SeedResult.class)))
    @ApiResponse(
            responseCode = "400",
            description = "개수가 0 이하·음수 또는 총 행 상한 초과",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public SeedResult seed(
            @RequestParam(name = "boards", defaultValue = "100") int boards,
            @RequestParam(name = "postsPerBoard", defaultValue = "10") int postsPerBoard) {
        return seedService.seed(boards, postsPerBoard);
    }
}
