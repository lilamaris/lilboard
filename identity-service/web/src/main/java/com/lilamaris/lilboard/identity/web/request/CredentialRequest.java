package com.lilamaris.lilboard.identity.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CredentialRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 128) String password) {
    @Override
    public String toString() {
        return "CredentialRequest[credentials=REDACTED]";
    }
}
