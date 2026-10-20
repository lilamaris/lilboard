package com.lilamaris.lilboard.board.application.config;

import com.lilamaris.lilboard.board.application.port.out.UuidGenerator;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

    @Bean
    public UuidGenerator uuidGenerator() {
        return UUID::randomUUID;
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
