package com.lilamaris.lilboard.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.lilamaris.lilboard.identity.application.port.in.command.LoginCommand;
import com.lilamaris.lilboard.identity.application.port.in.result.LoginResult;
import com.lilamaris.lilboard.identity.application.port.out.AccessTokenIssuePort;
import com.lilamaris.lilboard.identity.application.port.out.CredentialAuthenticatePort;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoginServiceTest {
    private final CredentialAuthenticatePort credentials = mock(CredentialAuthenticatePort.class);
    private final AccessTokenIssuePort tokens = mock(AccessTokenIssuePort.class);
    private final LoginService service = new LoginService(credentials, tokens);

    @Test
    @DisplayName("인증에 성공하면 해당 사용자에게 발급한 액세스 토큰을 반환한다")
    void returnsTokenForAuthenticatedAccount() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(credentials.authenticate("user@example.com", "password123")).thenReturn(id);
        when(tokens.issue(id)).thenReturn(new LoginResult("access-token", 900));
        assertThat(service.login(new LoginCommand("user@example.com", "password123")))
                .isEqualTo(new LoginResult("access-token", 900));
    }

    @Test
    @DisplayName("인증에 실패하면 토큰을 발급하지 않고 실패를 전달한다")
    void propagatesAuthenticationFailureWithoutIssuingToken() {
        var failure = new IllegalArgumentException("인증 실패");
        when(credentials.authenticate("user@example.com", "wrongpassword")).thenThrow(failure);
        assertThatThrownBy(
                        () -> service.login(new LoginCommand("user@example.com", "wrongpassword")))
                .isSameAs(failure);
        verifyNoInteractions(tokens);
    }
}
