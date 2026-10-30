package com.lilamaris.lilboard.board.seed;

import com.lilamaris.lilboard.board.application.port.out.UuidGenerator;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * loadtest 프로파일에서만 시딩 구성을 등록한다.
 *
 * <p>빈을 프로필로 게이트하므로 prod·test에서는 서비스·저장소·상한이 생성되지 않는다. {@code SeedController}는 같은 프로필의 스테레오타입 스캔으로
 * 등록된다. 시딩은 부하 테스트 환경의 DB에만 수행해야 하며, loadtest는 해당 환경의 DB와 함께 사용한다.
 */
@Profile("loadtest")
@Configuration
public class SeedConfiguration {
    @Bean
    public SeedLimits seedLimits(
            @Value("${board.seed.max-total-rows:100000}") long maxTotalRows,
            @Value("${board.seed.batch-size:500}") int batchSize) {
        return new SeedLimits(maxTotalRows, batchSize);
    }

    @Bean
    public SeedDataGenerator seedDataGenerator(UuidGenerator uuidGenerator) {
        return new SeedDataGenerator(uuidGenerator);
    }

    @Bean
    public SeedStore seedStore(JdbcTemplate jdbcTemplate, SeedLimits limits) {
        return new JdbcSeedStore(jdbcTemplate, limits);
    }

    @Bean
    public SeedService seedService(
            SeedDataGenerator generator, SeedStore store, SeedLimits limits, Clock clock) {
        return new SeedService(generator, store, limits, clock);
    }
}
