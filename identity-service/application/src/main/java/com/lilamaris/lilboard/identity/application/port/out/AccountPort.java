package com.lilamaris.lilboard.identity.application.port.out;

import com.lilamaris.lilboard.identity.domain.Account;
import java.util.Optional;

public interface AccountPort {
    Optional<Account> findByEmail(String email);

    Account save(Account account);
}
