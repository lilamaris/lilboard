package com.lilamaris.lilboard.identity.application.port.in.result;

public record LoginResult(String accessToken, long expiresIn) {
    @Override
    public String toString() {
        return "LoginResult[accessToken=REDACTED]";
    }
}
