package com.lilamaris.lilboard.board.application.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApplicationConfigurationTest {
    @Test
    @DisplayName("기본 시계는 UTC 시간대를 사용한다")
    void providesUtcClock() {
        assertThat(new ApplicationConfiguration().clock().getZone()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    @DisplayName("UUID 생성기는 버전 4 UUID를 생성한다")
    void generatesVersionFourUuid() {
        assertThat(new ApplicationConfiguration().uuidGenerator().generate().version())
                .isEqualTo(4);
    }
}
