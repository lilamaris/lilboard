package com.lilamaris.lilboard.identity.jpa.repository;

import com.lilamaris.lilboard.identity.domain.Account;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByEmail(String email);
}
