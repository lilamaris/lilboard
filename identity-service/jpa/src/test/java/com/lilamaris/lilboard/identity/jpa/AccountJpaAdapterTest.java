package com.lilamaris.lilboard.identity.jpa;

import static org.assertj.core.api.Assertions.*;

import com.lilamaris.lilboard.identity.application.exception.EmailAlreadyRegisteredException;
import com.lilamaris.lilboard.identity.domain.Account;
import com.lilamaris.lilboard.identity.jpa.repository.AccountRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest(properties = {"spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate"})
@ContextConfiguration(classes = AccountJpaAdapterTest.TestApplication.class)
class AccountJpaAdapterTest {
    @Autowired AccountJpaAdapter adapter;
    @Autowired EntityManager entityManager;

    @Test
    @DisplayName("계정을 저장하면 정규화된 이메일로 동일한 계정을 다시 조회한다")
    void savesAndFindsAccount() {
        var account = account("00000000-0000-0000-0000-000000000001", "hash");
        adapter.save(account);
        entityManager.clear();
        assertThat(adapter.findByEmail("user@example.com"))
                .hasValueSatisfying(
                        actual -> assertThat(actual).usingRecursiveComparison().isEqualTo(account));
    }

    @Test
    @DisplayName("동일 이메일을 다른 사용자 ID로 저장하면 중복 이메일 예외가 발생한다")
    void rejectsDuplicateEmailAtDatabase() {
        adapter.save(account("00000000-0000-0000-0000-000000000001", "hash"));
        assertThatThrownBy(
                        () -> adapter.save(account("00000000-0000-0000-0000-000000000002", "hash")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    @DisplayName("이메일 중복 외의 저장 제약 위반은 중복 이메일로 처리하지 않는다")
    void preservesOtherIntegrityFailures() {
        assertThatThrownBy(
                        () ->
                                adapter.save(
                                        account(
                                                "00000000-0000-0000-0000-000000000001",
                                                "x".repeat(256))))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    private Account account(String id, String hash) {
        return Account.of(UUID.fromString(id), "User@Example.com", hash, Instant.EPOCH);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Account.class)
    @EnableJpaRepositories(basePackageClasses = AccountRepository.class)
    @Import(AccountJpaAdapter.class)
    static class TestApplication {}
}
