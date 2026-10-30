package com.lilamaris.lilboard.identity.jpa;

import com.lilamaris.lilboard.identity.application.exception.EmailAlreadyRegisteredException;
import com.lilamaris.lilboard.identity.application.port.out.AccountPort;
import com.lilamaris.lilboard.identity.domain.Account;
import com.lilamaris.lilboard.identity.jpa.repository.AccountRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AccountJpaAdapter implements AccountPort {
    private final AccountRepository repository;

    public Optional<Account> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    public Account save(Account account) {
        try {
            return repository.saveAndFlush(account);
        } catch (DataIntegrityViolationException exception) {
            if (String.valueOf(exception.getMostSpecificCause().getMessage())
                    .toLowerCase(java.util.Locale.ROOT)
                    .contains("uk_identity_account_email")) {
                throw new EmailAlreadyRegisteredException();
            }
            throw exception;
        }
    }
}
