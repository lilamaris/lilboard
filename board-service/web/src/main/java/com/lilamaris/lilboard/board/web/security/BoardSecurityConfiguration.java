package com.lilamaris.lilboard.board.web.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication(
        type =
                org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type
                        .SERVLET)
@Configuration
public class BoardSecurityConfiguration {
    @Bean
    SecurityFilterChain boardSecurity(
            HttpSecurity http, ObjectProvider<HttpAuthorizationCustomizer> customizers)
            throws Exception {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return http.csrf(
                        csrf ->
                                csrf.ignoringRequestMatchers(
                                        this::isJsonRequest, this::hasBearerToken))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth -> {
                            auth.dispatcherTypeMatchers(DispatcherType.ERROR)
                                    .permitAll()
                                    .requestMatchers(
                                            HttpMethod.GET,
                                            "/api/boards",
                                            "/api/boards/{id}",
                                            "/api/posts",
                                            "/api/posts/{id}")
                                    .permitAll()
                                    .requestMatchers(
                                            HttpMethod.GET,
                                            "/actuator/health",
                                            "/actuator/metrics",
                                            "/actuator/metrics/**",
                                            "/actuator/prometheus",
                                            "/v3/api-docs",
                                            "/v3/api-docs/**",
                                            "/swagger-ui.html",
                                            "/swagger-ui/**")
                                    .permitAll()
                                    .requestMatchers(HttpMethod.POST, "/api/boards", "/api/posts")
                                    .hasRole("MEMBER")
                                    .requestMatchers(
                                            HttpMethod.PUT, "/api/boards/{id}", "/api/posts/{id}")
                                    .hasRole("MEMBER")
                                    .requestMatchers(HttpMethod.DELETE, "/api/boards/{id}")
                                    .hasRole("MEMBER");
                            customizers
                                    .orderedStream()
                                    .forEach(customizer -> customizer.customize(auth));
                            auth.anyRequest().denyAll();
                        })
                .oauth2ResourceServer(
                        resource -> resource.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .build();
    }

    // JSON is not a browser form content type. Form and text/plain requests retain CSRF checks.
    private boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null
                && contentType.split(";", 2)[0].strip().equalsIgnoreCase("application/json");
    }

    // Only explicit Bearer headers qualify; cookies and other authentication schemes do not.
    private boolean hasBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        return authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7);
    }
}
