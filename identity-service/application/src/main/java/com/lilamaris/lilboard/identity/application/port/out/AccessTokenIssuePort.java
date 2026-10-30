package com.lilamaris.lilboard.identity.application.port.out;

import com.lilamaris.lilboard.identity.application.port.in.result.LoginResult;
import java.util.UUID;

public interface AccessTokenIssuePort {
    LoginResult issue(UUID userId);
}
