package com.lilamaris.lilboard.identity.web.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.tags.Tag(name = "공개키", description = "JWT 서명 검증용 공개키 API")
@RestController
@RequiredArgsConstructor
public class JwkSetController {
    private final RSAKey key;

    @io.swagger.v3.oas.annotations.Operation(
            summary = "JWKS 조회",
            description = "JWT 서명 검증용 RSA 공개키를 반환합니다. 개인키는 포함하지 않습니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "JWKS 공개키 조회 성공")
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> keys() {
        return new JWKSet(key.toPublicJWK()).toJSONObject();
    }
}
