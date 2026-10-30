package com.lilamaris.lilboard.board.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@DisplayName("loadtest 없는 프로파일의 시딩 구성 테스트")
class SeedConfigurationWithoutLoadtestIntegrationTest {
    @Autowired private ApplicationContext context;

    @Test
    @DisplayName("loadtest가 없는 test 프로파일에서는 시딩 빈이 등록되지 않는다")
    void doesNotRegisterSeedBeansWithoutLoadtestProfile() {
        assertThat(context.getBeansOfType(SeedController.class)).isEmpty();
        assertThat(context.getBeansOfType(SeedService.class)).isEmpty();
        assertThat(context.getBeansOfType(SeedStore.class)).isEmpty();
        assertThat(context.getBeansOfType(SeedLimits.class)).isEmpty();
    }
}
