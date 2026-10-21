package com.lilamaris.lilboard.board.application.config;

import com.github.f4b6a3.uuid.UuidCreator;
import com.lilamaris.lilboard.board.application.port.out.UuidGenerator;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

    @Bean
    public UuidGenerator uuidGenerator() {
        return UuidCreator::getTimeOrderedEpoch;
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
