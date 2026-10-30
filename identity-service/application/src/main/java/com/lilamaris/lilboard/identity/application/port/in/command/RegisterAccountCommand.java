package com.lilamaris.lilboard.identity.application.port.in.command;

public record RegisterAccountCommand(String email, String password) {
    @Override
    public String toString() {
        return "RegisterAccountCommand[credentials=REDACTED]";
    }
}
