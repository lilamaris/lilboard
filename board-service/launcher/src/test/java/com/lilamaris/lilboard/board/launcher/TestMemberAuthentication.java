package com.lilamaris.lilboard.board.launcher;

import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

@TestConfiguration
public class TestMemberAuthentication {
    @Bean
    JwtDecoder testJwtDecoder() throws Exception {
        var decoder = NimbusJwtDecoder.withPublicKey(publicKey()).build();
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer("http://localhost:8081"),
                        new JwtClaimValidator<List<String>>(
                                "aud",
                                audiences ->
                                        audiences != null && audiences.contains("lilboard-api"))));
        return decoder;
    }

    static java.security.interfaces.RSAPublicKey publicKey() throws Exception {
        try (var stream = new ClassPathResource("test-public.pem").getInputStream()) {
            return RsaKeyConverters.x509().convert(stream);
        }
    }

    static HttpRequest.Builder memberRequest(URI uri) throws Exception {
        return HttpRequest.newBuilder(uri)
                .header(
                        "Authorization",
                        "Bearer "
                                + token(
                                        "ROLE_MEMBER",
                                        "http://localhost:8081",
                                        "lilboard-api",
                                        Instant.now().plusSeconds(300)));
    }

    static String token(String role, String issuer, String audience, Instant expiresAt)
            throws Exception {
        java.security.interfaces.RSAPrivateKey privateKey;
        try (var stream = new ClassPathResource("test-private.pem").getInputStream()) {
            privateKey = RsaKeyConverters.pkcs8().convert(stream);
        }
        var key = new RSAKey.Builder(publicKey()).privateKey(privateKey).build();
        var encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
        return encoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(SignatureAlgorithm.RS256).build(),
                                JwtClaimsSet.builder()
                                        .subject("00000000-0000-0000-0000-000000000001")
                                        .issuer(issuer)
                                        .audience(List.of(audience))
                                        .issuedAt(expiresAt.minusSeconds(300))
                                        .expiresAt(expiresAt)
                                        .claim("roles", List.of(role))
                                        .build()))
                .getTokenValue();
    }
}
