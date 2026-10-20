package com.lilamaris.lilboard.board.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class LauncherApplicationTest {

    @Autowired private DataSource dataSource;

    @Autowired private ApplicationContext applicationContext;

    @Test
    @DisplayName("test 프로파일로 기동하면 H2에 연결되고 Flyway는 비활성화된다")
    void startsWithH2AndDisablesFlyway() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("H2");
            assertThat(connection.isValid(1)).isTrue();
        }
        assertThat(applicationContext.getBeansOfType(Flyway.class)).isEmpty();
    }
}
