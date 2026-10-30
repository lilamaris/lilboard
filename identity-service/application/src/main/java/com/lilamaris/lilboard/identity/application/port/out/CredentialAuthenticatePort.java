package com.lilamaris.lilboard.identity.application.port.out;

import java.util.UUID;

public interface CredentialAuthenticatePort {
    UUID authenticate(String email, String password);
}
