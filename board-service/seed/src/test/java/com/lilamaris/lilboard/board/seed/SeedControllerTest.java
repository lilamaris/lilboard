package com.lilamaris.lilboard.board.seed;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("SeedController 테스트")
class SeedControllerTest {
    private SeedService seedService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        seedService = mock(SeedService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SeedController(seedService)).build();
    }

    @Test
    @DisplayName("POST /api/seed에 boards와 postsPerBoard를 넘기면 시딩 결과를 200으로 반환한다")
    void returnsSeedResultForRequestedCounts() throws Exception {
        when(seedService.seed(3, 2)).thenReturn(SeedResult.of(3, 6));

        mvc.perform(post("/api/seed?boards=3&postsPerBoard=2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.boards").value(3))
                .andExpect(jsonPath("$.posts").value(6))
                .andExpect(jsonPath("$.totalRows").value(9));

        verify(seedService).seed(3, 2);
    }

    @Test
    @DisplayName("파라미터가 없으면 기본값 게시판 100개와 게시글 10개로 시딩한다")
    void usesDefaultCountsWhenParametersAreOmitted() throws Exception {
        when(seedService.seed(100, 10)).thenReturn(SeedResult.of(100, 1000));

        mvc.perform(post("/api/seed")).andExpect(status().isOk());

        verify(seedService).seed(100, 10);
    }

    @Test
    @DisplayName("개수 검증은 시딩 서비스가 담당하므로 컨트롤러는 요청 값을 그대로 전달한다")
    void passesInvalidCountsToServiceWithoutPreValidating() throws Exception {
        when(seedService.seed(0, 0)).thenReturn(SeedResult.of(0, 0));

        mvc.perform(post("/api/seed?boards=0&postsPerBoard=0")).andExpect(status().isOk());

        verify(seedService).seed(0, 0);
    }
}
