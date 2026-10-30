package com.lilamaris.lilboard.identity.application;

import com.lilamaris.lilboard.identity.application.exception.EmailAlreadyRegisteredException;
import com.lilamaris.lilboard.identity.application.port.in.RegisterAccountUseCase;
import com.lilamaris.lilboard.identity.application.port.in.command.RegisterAccountCommand;
import com.lilamaris.lilboard.identity.application.port.in.result.RegisterAccountResult;
import com.lilamaris.lilboard.identity.application.port.out.AccountPort;
import com.lilamaris.lilboard.identity.domain.Account;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CredentialService implements UserDetailsService, RegisterAccountUseCase {
    private final AccountPort accounts;
    private final PasswordEncoder passwords;
    private final Clock clock;
    private final Supplier<UUID> ids;

    @Transactional
    @Override
    public RegisterAccountResult register(RegisterAccountCommand command) {
        String email = command.email();
        String password = command.password();
        String normalized = Account.normalizeEmail(email);
        if (accounts.findByEmail(normalized).isPresent())
            throw new EmailAlreadyRegisteredException();
        return new RegisterAccountResult(
                accounts.save(
                                Account.of(
                                        ids.get(),
                                        normalized,
                                        passwords.encode(password),
                                        clock.instant()))
                        .getId());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        Account account =
                accounts.findByEmail(Account.normalizeEmail(email))
                        .orElseThrow(() -> new UsernameNotFoundException("인증 정보가 일치하지 않습니다."));
        return User.withUsername(account.getId().toString())
                .password(account.getPasswordHash())
                .roles("MEMBER")
                .build();
    }
}
