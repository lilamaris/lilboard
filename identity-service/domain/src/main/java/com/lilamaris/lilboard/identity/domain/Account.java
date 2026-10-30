package com.lilamaris.lilboard.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "identity_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MAX_PASSWORD_HASH_LENGTH = 255;

    @Id private UUID id;

    @Column(name = "email", nullable = false, length = MAX_EMAIL_LENGTH)
    private String email;

    @Column(name = "password_hash", nullable = false, length = MAX_PASSWORD_HASH_LENGTH)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private Account(UUID id, String email, String passwordHash, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null.");
        this.email = normalizeEmail(email);
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null.");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null.");
    }

    public static Account of(UUID id, String email, String passwordHash, Instant createdAt) {
        return new Account(id, email, passwordHash, createdAt);
    }

    public static String normalizeEmail(String email) {
        return Objects.requireNonNull(email, "email must not be null.")
                .strip()
                .toLowerCase(Locale.ROOT);
    }
}
