package com.sqljudge.problem.integration;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

final class TestJwts {

    static final UUID LEARNER_ID = UUID.fromString("0192f3a0-0000-7000-8000-000000000001");
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    private static final RSAKey SIGNING_KEY = newKey();

    private TestJwts() {}

    static String learnerToken() {
        return token(LEARNER_ID, "LEARNER");
    }

    static String token(UUID userId, String role) {
        JWTClaimsSet claims =
                new JWTClaimsSet.Builder()
                        .subject(userId.toString())
                        .claim("username", "user-" + userId.toString().substring(30))
                        .claim("role", role)
                        .issueTime(new Date())
                        .expirationTime(Date.from(Instant.now().plus(ACCESS_TOKEN_TTL)))
                        .build();
        SignedJWT jwt =
                new SignedJWT(
                        new JWSHeader.Builder(JWSAlgorithm.RS256)
                                .keyID(SIGNING_KEY.getKeyID())
                                .build(),
                        claims);
        try {
            jwt.sign(new RSASSASigner(SIGNING_KEY));
        } catch (JOSEException exception) {
            throw new IllegalStateException(exception);
        }
        return jwt.serialize();
    }

    private static RSAKey newKey() {
        try {
            return new RSAKeyGenerator(2048).keyIDFromThumbprint(true).generate();
        } catch (JOSEException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DecoderConfig {

        @Bean
        JwtDecoder jwtDecoder() throws JOSEException {
            return NimbusJwtDecoder.withPublicKey(SIGNING_KEY.toRSAPublicKey()).build();
        }
    }
}
