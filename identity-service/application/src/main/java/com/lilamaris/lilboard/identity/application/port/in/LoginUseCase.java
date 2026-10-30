package com.lilamaris.lilboard.identity.application.port.in;

import com.lilamaris.lilboard.identity.application.port.in.command.LoginCommand;
import com.lilamaris.lilboard.identity.application.port.in.result.LoginResult;

public interface LoginUseCase {
    LoginResult login(LoginCommand command);
}
