package com.sqljudge.auth.oauth;

import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.service.OAuthProfile;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class OAuthProfiles {

    private static final String EMAIL_ATTRIBUTE = "email";
    private static final String EMAIL_VERIFIED_ATTRIBUTE = "email_verified";
    private static final String NAME_ATTRIBUTE = "name";

    private OAuthProfiles() {}

    public static OAuthProfile from(String registrationId, Map<String, Object> attributes) {
        SignupProvider provider = SignupProvider.valueOf(registrationId.toUpperCase(Locale.ROOT));
        return switch (provider) {
            case GITHUB -> fromGithub(attributes);
            case GOOGLE -> fromGoogle(attributes);
            case EMAIL -> throw new IllegalArgumentException("EMAIL is not an OAuth provider");
        };
    }

    private static OAuthProfile fromGithub(Map<String, Object> attributes) {
        String login = asText(attributes.get("login"));
        String name = asText(attributes.get(NAME_ATTRIBUTE));
        return new OAuthProfile(
                SignupProvider.GITHUB,
                Objects.toString(attributes.get("id")),
                asText(attributes.get(EMAIL_ATTRIBUTE)),
                isTrue(attributes.get(EMAIL_VERIFIED_ATTRIBUTE)),
                login,
                name == null ? login : name);
    }

    private static OAuthProfile fromGoogle(Map<String, Object> attributes) {
        String email = asText(attributes.get(EMAIL_ATTRIBUTE));
        String localPart = localPartOf(email);
        String name = asText(attributes.get(NAME_ATTRIBUTE));
        return new OAuthProfile(
                SignupProvider.GOOGLE,
                Objects.toString(attributes.get("sub")),
                email,
                isTrue(attributes.get(EMAIL_VERIFIED_ATTRIBUTE)),
                localPart,
                name == null ? localPart : name);
    }

    private static String localPartOf(String email) {
        if (email == null) {
            return null;
        }
        int separator = email.indexOf('@');
        return separator < 0 ? email : email.substring(0, separator);
    }

    private static boolean isTrue(Object value) {
        return Boolean.TRUE.equals(value) || "true".equals(value);
    }

    private static String asText(Object value) {
        return value == null ? null : value.toString();
    }
}
