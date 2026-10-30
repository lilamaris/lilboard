package com.lilamaris.lilboard.identity.web.security;

import com.lilamaris.lilboard.identity.application.port.out.CredentialAuthenticatePort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CredentialAuthenticationAdapter implements CredentialAuthenticatePort {
    private final AuthenticationManager authenticationManager;

    @Override
    public UUID authenticate(String email, String password) {
        var authentication =
                authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated(email, password));
        return UUID.fromString(authentication.getName());
    }
}
