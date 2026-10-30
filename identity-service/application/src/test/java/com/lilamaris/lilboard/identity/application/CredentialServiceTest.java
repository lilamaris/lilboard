package com.lilamaris.lilboard.identity.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.lilamaris.lilboard.identity.application.exception.EmailAlreadyRegisteredException;
import com.lilamaris.lilboard.identity.application.port.in.command.RegisterAccountCommand;
import com.lilamaris.lilboard.identity.application.port.out.AccountPort;
import com.lilamaris.lilboard.identity.domain.Account;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

class CredentialServiceTest {
    private final AccountPort accounts = mock(AccountPort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final Pbkdf2PasswordEncoder passwords =
            Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    private final CredentialService service =
            new CredentialService(
                    accounts,
                    passwords,
                    clock,
                    () -> UUID.fromString("00000000-0000-0000-0000-000000000001"));

    @Test
    @DisplayName("회원가입은 정규화된 이메일과 암호화된 비밀번호를 저장한다")
    void registersNormalizedCredential() {
        when(accounts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var id = service.register(new RegisterAccountCommand(" User@Example.com ", "password123"));
        var saved = org.mockito.ArgumentCaptor.forClass(Account.class);
        verify(accounts).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(id.id());
        assertThat(saved.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(passwords.matches("password123", saved.getValue().getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("등록된 이메일로 가입하면 중복 예외가 발생하고 저장하지 않는다")
    void rejectsDuplicateCredential() {
        when(accounts.findByEmail("user@example.com")).thenReturn(Optional.of(account()));
        assertThatThrownBy(
                        () ->
                                service.register(
                                        new RegisterAccountCommand(
                                                "USER@example.com", "password123")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(accounts, never()).save(any());
    }

    @Test
    @DisplayName("이메일로 조회한 인증 주체는 사용자 ID와 회원 권한을 가진다")
    void loadsMemberByNormalizedEmail() {
        var account = account();
        when(accounts.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        var user = service.loadUserByUsername("USER@example.com");
        assertThat(user.getUsername()).isEqualTo(account.getId().toString());
        assertThat(user.getPassword()).isEqualTo("hash");
        assertThat(user.getAuthorities()).extracting("authority").containsExactly("ROLE_MEMBER");
    }

    @Test
    @DisplayName("없는 이메일로 인증 주체를 조회하면 인증 실패 예외가 발생한다")
    void rejectsUnknownCredential() {
        assertThatThrownBy(() -> service.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    private Account account() {
        return Account.of(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "user@example.com",
                "hash",
                clock.instant());
    }
}
