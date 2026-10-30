package com.lilamaris.lilboard.identity.application.port.in;

import com.lilamaris.lilboard.identity.application.port.in.command.RegisterAccountCommand;
import com.lilamaris.lilboard.identity.application.port.in.result.RegisterAccountResult;

public interface RegisterAccountUseCase {
    RegisterAccountResult register(RegisterAccountCommand command);
}
