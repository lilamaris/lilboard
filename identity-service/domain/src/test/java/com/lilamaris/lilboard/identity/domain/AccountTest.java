package com.lilamaris.lilboard.identity.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;

class AccountTest {
    @Test
    @DisplayName("계정은 이메일의 주변 공백을 제거하고 소문자로 정규화한다")
    void normalizesEmail() {
        var id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var account = Account.of(id, " User@Example.com ", "hash", Instant.EPOCH);
        assertThat(account.getEmail()).isEqualTo("user@example.com");
        assertThat(account.getId()).isEqualTo(id);
        assertThat(account.getPasswordHash()).isEqualTo("hash");
        assertThat(account.getCreatedAt()).isEqualTo(Instant.EPOCH);
    }

    @Test
    @DisplayName("사용자 ID가 없으면 계정을 생성할 수 없다")
    void rejectsMissingId() {
        assertThatNullPointerException()
                .isThrownBy(() -> Account.of(null, "user@example.com", "hash", Instant.EPOCH));
    }

    @Test
    @DisplayName("비밀번호 해시가 없으면 계정을 생성할 수 없다")
    void rejectsMissingPasswordHash() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                Account.of(
                                        UUID.fromString("00000000-0000-0000-0000-000000000001"),
                                        "user@example.com",
                                        null,
                                        Instant.EPOCH));
    }

    @Test
    @DisplayName("생성 시각이 없으면 계정을 생성할 수 없다")
    void rejectsMissingCreatedAt() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                Account.of(
                                        UUID.fromString("00000000-0000-0000-0000-000000000001"),
                                        "user@example.com",
                                        "hash",
                                        null));
    }

    @Test
    @DisplayName("이메일이 없으면 계정을 생성할 수 없다")
    void rejectsMissingEmail() {
        assertThatNullPointerException().isThrownBy(() -> Account.normalizeEmail(null));
    }
}
