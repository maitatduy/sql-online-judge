package com.sqljudge.auth.service;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.sqljudge.auth.entity.User;
import com.sqljudge.common.security.JwtCurrentUserConverter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenIssuer {

    static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

    private final JwtEncoder jwtEncoder;
    private final String keyId;
    private final Clock clock;

    public AccessTokenIssuer(RSAKey jwtSigningKey, Clock clock) {
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwtSigningKey)));
        this.keyId = jwtSigningKey.getKeyID();
        this.clock = clock;
    }

    public String issue(User user) {
        Instant now = clock.instant();
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .id(UUID.randomUUID().toString())
                        .subject(user.getId().toString())
                        .claim(JwtCurrentUserConverter.USERNAME_CLAIM, user.getUsername())
                        .claim(JwtCurrentUserConverter.ROLE_CLAIM, user.getRole().name())
                        .issuedAt(now)
                        .expiresAt(now.plus(ACCESS_TOKEN_TTL))
                        .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(keyId).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
