package com.lilamaris.lilboard.board.web.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * 기본 접근 규칙 뒤에 모듈 전용 HTTP 접근 규칙을 추가한다. 기존 규칙을 덮어쓰지 않으며, 최종 anyRequest 규칙은 web 보안 설정이 등록한다. 여러 빈이 있으면
 * Spring의 Ordered 및 Order 순서를 따른다.
 */
@FunctionalInterface
public interface HttpAuthorizationCustomizer {
    void customize(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
                    registry);
}
