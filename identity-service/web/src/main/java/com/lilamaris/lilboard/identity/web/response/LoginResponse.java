package com.lilamaris.lilboard.identity.web.response;

import com.lilamaris.lilboard.identity.application.port.in.result.LoginResult;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
    public static LoginResponse from(LoginResult result) {
        return new LoginResponse(result.accessToken(), "Bearer", result.expiresIn());
    }

    @Override
    public String toString() {
        return "LoginResponse[accessToken=REDACTED]";
    }
}
