package com.sqljudge.common.security;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

public class JwtCurrentUserConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    public static final String USERNAME_CLAIM = "username";
    public static final String ROLE_CLAIM = "role";

    private static final String INVALID_CLAIMS = "Access token has invalid user claims";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        CurrentUser user = toCurrentUser(jwt);
        List<SimpleGrantedAuthority> authorities =
                user.roles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.authority()))
                        .toList();
        return UsernamePasswordAuthenticationToken.authenticated(user, null, authorities);
    }

    private static CurrentUser toCurrentUser(Jwt jwt) {
        String role = jwt.getClaimAsString(ROLE_CLAIM);
        String username = jwt.getClaimAsString(USERNAME_CLAIM);
        if (role == null || username == null || jwt.getSubject() == null) {
            throw new InvalidBearerTokenException(INVALID_CLAIMS);
        }
        try {
            return new CurrentUser(
                    UUID.fromString(jwt.getSubject()), username, Set.of(Role.valueOf(role)));
        } catch (IllegalArgumentException exception) {
            throw new InvalidBearerTokenException(INVALID_CLAIMS, exception);
        }
    }
}
