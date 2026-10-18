package com.sqljudge.auth.unit;

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

final class TestJwts {

    static final UUID USER_ID = UUID.fromString("0192f3a0-0000-7000-8000-000000000001");
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

    private TestJwts() {}

    static RSAKey newKey() {
        try {
            return new RSAKeyGenerator(2048).keyIDFromThumbprint(true).generate();
        } catch (JOSEException exception) {
            throw new IllegalStateException(exception);
        }
    }

    static String validToken(RSAKey key, String role) {
        return sign(key, role, Instant.now().plus(ACCESS_TOKEN_TTL));
    }

    static String expiredToken(RSAKey key) {
        return sign(key, "LEARNER", Instant.now().minus(Duration.ofMinutes(5)));
    }

    private static String sign(RSAKey key, String role, Instant expiresAt) {
        JWTClaimsSet claims =
                new JWTClaimsSet.Builder()
                        .subject(USER_ID.toString())
                        .claim("username", "minh.anh")
                        .claim("role", role)
                        .issueTime(Date.from(expiresAt.minus(ACCESS_TOKEN_TTL)))
                        .expirationTime(Date.from(expiresAt))
                        .build();
        SignedJWT jwt =
                new SignedJWT(
                        new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),
                        claims);
        try {
            jwt.sign(new RSASSASigner(key));
        } catch (JOSEException exception) {
            throw new IllegalStateException(exception);
        }
        return jwt.serialize();
    }
}
