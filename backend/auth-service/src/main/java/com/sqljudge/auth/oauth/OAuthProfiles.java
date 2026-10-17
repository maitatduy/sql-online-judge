package com.sqljudge.auth.oauth;

import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.service.OAuthProfile;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

final class OAuthProfiles {

    private OAuthProfiles() {}

    static OAuthProfile from(String registrationId, Map<String, Object> attributes) {
        SignupProvider provider = SignupProvider.valueOf(registrationId.toUpperCase(Locale.ROOT));
        String login = asText(attributes.get("login"));
        String name = asText(attributes.get("name"));
        return new OAuthProfile(
                provider,
                Objects.toString(attributes.get("id")),
                asText(attributes.get(GithubOAuth2UserService.EMAIL_ATTRIBUTE)),
                Boolean.TRUE.equals(
                        attributes.get(GithubOAuth2UserService.EMAIL_VERIFIED_ATTRIBUTE)),
                login,
                name == null ? login : name);
    }

    private static String asText(Object value) {
        return value == null ? null : value.toString();
    }
}
