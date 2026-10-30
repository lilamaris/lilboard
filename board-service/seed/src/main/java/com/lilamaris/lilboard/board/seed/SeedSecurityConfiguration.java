package com.lilamaris.lilboard.board.seed;

import com.lilamaris.lilboard.board.web.security.HttpAuthorizationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;

/** loadtest에서만 시딩 POST 요청을 비로그인 사용자에게 허용한다. */
@Profile("loadtest")
@Configuration
public class SeedSecurityConfiguration {
    @Bean
    public HttpAuthorizationCustomizer seedAuthorizationCustomizer() {
        return registry -> registry.requestMatchers(HttpMethod.POST, "/api/seed").permitAll();
    }
}
