package com.sqljudge.auth.oauth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GithubOAuth2UserService extends DefaultOAuth2UserService {

    static final String EMAIL_ATTRIBUTE = "email";
    static final String EMAIL_VERIFIED_ATTRIBUTE = "email_verified";
    private static final String ID_ATTRIBUTE = "id";

    private final RestClient restClient;
    private final String emailsUri;

    public GithubOAuth2UserService(
            RestClient.Builder restClientBuilder,
            @Value("${sqljudge.oauth.github.emails-uri}") String emailsUri) {
        this.restClient = restClientBuilder.build();
        this.emailsUri = emailsUri;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User user = super.loadUser(userRequest);
        Map<String, Object> attributes = new HashMap<>(user.getAttributes());
        primaryEmail(userRequest.getAccessToken().getTokenValue())
                .ifPresentOrElse(
                        email -> {
                            attributes.put(EMAIL_ATTRIBUTE, email.email());
                            attributes.put(EMAIL_VERIFIED_ATTRIBUTE, email.verified());
                        },
                        () -> attributes.put(EMAIL_VERIFIED_ATTRIBUTE, false));
        return new DefaultOAuth2User(user.getAuthorities(), attributes, ID_ATTRIBUTE);
    }

    private Optional<GithubEmail> primaryEmail(String accessToken) {
        List<GithubEmail> emails =
                restClient
                        .get()
                        .uri(emailsUri)
                        .headers(headers -> headers.setBearerAuth(accessToken))
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<GithubEmail>>() {});
        if (emails == null) {
            return Optional.empty();
        }
        return emails.stream().filter(GithubEmail::primary).findFirst();
    }

    private record GithubEmail(String email, boolean primary, boolean verified) {}
}
