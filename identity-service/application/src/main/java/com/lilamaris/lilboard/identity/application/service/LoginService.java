package com.lilamaris.lilboard.identity.application.service;

import com.lilamaris.lilboard.identity.application.port.in.LoginUseCase;
import com.lilamaris.lilboard.identity.application.port.in.command.LoginCommand;
import com.lilamaris.lilboard.identity.application.port.in.result.LoginResult;
import com.lilamaris.lilboard.identity.application.port.out.AccessTokenIssuePort;
import com.lilamaris.lilboard.identity.application.port.out.CredentialAuthenticatePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {
    private final CredentialAuthenticatePort credentials;
    private final AccessTokenIssuePort tokens;

    @Override
    public LoginResult login(LoginCommand command) {
        return tokens.issue(credentials.authenticate(command.email(), command.password()));
    }
}
