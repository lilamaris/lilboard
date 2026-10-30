package com.lilamaris.lilboard.identity.web.security;

import com.lilamaris.lilboard.identity.application.port.in.result.LoginResult;
import com.lilamaris.lilboard.identity.application.port.out.AccessTokenIssuePort;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class JwtAccessTokenAdapter implements AccessTokenIssuePort {
    private final JwtEncoder encoder;
    private final Clock clock;
    private final String issuer;
    private final Duration ttl;

    public JwtAccessTokenAdapter(
            JwtEncoder encoder,
            Clock clock,
            @Value("${auth.jwt.issuer}") String issuer,
            @Value("${auth.jwt.access-token-ttl:PT15M}") Duration ttl) {
        if (ttl.isNegative() || ttl.isZero())
            throw new IllegalArgumentException("토큰 수명은 양수여야 합니다.");
        this.encoder = encoder;
        this.clock = clock;
        this.issuer = issuer;
        this.ttl = ttl;
    }

    @Override
    public LoginResult issue(UUID userId) {
        var now = clock.instant();
        var claims =
                JwtClaimsSet.builder()
                        .issuer(issuer)
                        .subject(userId.toString())
                        .audience(java.util.List.of("lilboard-api"))
                        .issuedAt(now)
                        .expiresAt(now.plus(ttl))
                        .claim("roles", java.util.List.of("ROLE_MEMBER"))
                        .build();
        String token =
                encoder.encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
                        .getTokenValue();
        return new LoginResult(token, ttl.toSeconds());
    }
}
