package com.lilamaris.lilboard.board.seed;

import com.github.f4b6a3.uuid.UuidCreator;
import com.lilamaris.lilboard.board.application.port.out.UuidGenerator;
import com.lilamaris.lilboard.board.domain.Board;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * seed 모듈 통합 테스트 애플리케이션. seed 패키지만 스캔하고 domain 엔티티를 EntityScan으로 등록한다.
 *
 * <p>application 구성을 스캔하지 않으므로 시간과 UUID는 테스트가 직접 제공하는 빈으로 제어한다.
 */
@SpringBootApplication
@ComponentScan(basePackageClasses = SeedConfiguration.class)
@EntityScan(basePackageClasses = Board.class)
class SeedTestApplication {
    private static final Instant SEED_BASE = Instant.parse("2026-01-01T00:00:00Z");

    @Bean
    UuidGenerator uuidGenerator() {
        return UuidCreator::getTimeOrderedEpoch;
    }

    @Bean
    Clock clock() {
        return Clock.fixed(SEED_BASE, ZoneOffset.UTC);
    }
}
