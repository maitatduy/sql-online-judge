package com.sqljudge.auth.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequiredArgsConstructor
public class JwksController {

    private final RSAKey jwtSigningKey;

    @GetMapping("/internal/jwks")
    public Map<String, Object> jwks() {
        return new JWKSet(jwtSigningKey.toPublicJWK()).toJSONObject();
    }
}
