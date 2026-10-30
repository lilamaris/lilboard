package com.lilamaris.lilboard.identity.web.security;

import com.lilamaris.lilboard.identity.application.CredentialService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class IdentitySecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    AuthenticationManager authenticationManager(
            CredentialService credentials, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(credentials);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    RSAKey signingKey(
            @Value("${auth.jwt.public-key}") Resource publicResource,
            @Value("${auth.jwt.private-key}") Resource privateResource)
            throws IOException {
        RSAPublicKey publicKey;
        RSAPrivateKey privateKey;
        try (var input = publicResource.getInputStream()) {
            publicKey = RsaKeyConverters.x509().convert(input);
        }
        try (var input = privateResource.getInputStream()) {
            privateKey = RsaKeyConverters.pkcs8().convert(input);
        }
        if (!publicKey.getModulus().equals(privateKey.getModulus()))
            throw new IllegalArgumentException("서명 키 쌍이 일치하지 않습니다.");
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("lilboard-signing-key")
                .build();
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey key) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.ignoringRequestMatchers(this::isJsonRequest))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR)
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/auth/register",
                                                "/api/auth/login")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/.well-known/jwks.json",
                                                "/v3/api-docs",
                                                "/v3/api-docs/**",
                                                "/swagger-ui.html",
                                                "/swagger-ui/**")
                                        .permitAll()
                                        .anyRequest()
                                        .denyAll())
                .exceptionHandling(
                        errors ->
                                errors.authenticationEntryPoint(
                                        (request, response, exception) -> response.sendError(401)))
                .build();
    }

    // JSON is not a browser form content type. Form and text/plain requests retain CSRF checks.
    private boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null
                && contentType.split(";", 2)[0].strip().equalsIgnoreCase("application/json");
    }
}
