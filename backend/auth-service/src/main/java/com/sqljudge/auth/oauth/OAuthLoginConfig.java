package com.sqljudge.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class OAuthLoginConfig {

    static final String AUTHORIZATION_BASE_URI = "/api/auth/oauth2/authorization";
    static final String CALLBACK_BASE_URI = "/api/auth/login/oauth2/code/*";

    @Bean
    @Order(1)
    SecurityFilterChain oauthLoginFilterChain(
            HttpSecurity http,
            RedisOAuth2AuthorizationRequestRepository authorizationRequestRepository,
            ProviderOAuth2UserService providerUserService,
            OAuthLoginHandlers oauthLoginHandlers)
            throws Exception {
        return http.securityMatcher("/api/auth/oauth2/**", "/api/auth/login/oauth2/**")
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .oauth2Login(
                        oauth ->
                                oauth.authorizationEndpoint(
                                                authorization ->
                                                        authorization
                                                                .baseUri(AUTHORIZATION_BASE_URI)
                                                                .authorizationRequestRepository(
                                                                        authorizationRequestRepository))
                                        .redirectionEndpoint(
                                                redirection ->
                                                        redirection.baseUri(CALLBACK_BASE_URI))
                                        .userInfoEndpoint(
                                                userInfo ->
                                                        userInfo.userService(providerUserService))
                                        .authorizedClientRepository(new NoStoredAuthorizedClients())
                                        .successHandler(oauthLoginHandlers)
                                        .failureHandler(oauthLoginHandlers)
                                        .withObjectPostProcessor(
                                                redirectToLoginOnStartFailure(oauthLoginHandlers)))
                .build();
    }

    private static ObjectPostProcessor<OAuth2AuthorizationRequestRedirectFilter>
            redirectToLoginOnStartFailure(OAuthLoginHandlers oauthLoginHandlers) {
        return new ObjectPostProcessor<>() {
            @Override
            public <O extends OAuth2AuthorizationRequestRedirectFilter> O postProcess(O filter) {
                filter.setAuthenticationFailureHandler(oauthLoginHandlers);
                return filter;
            }
        };
    }

    private static final class NoStoredAuthorizedClients
            implements OAuth2AuthorizedClientRepository {

        @Override
        public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
                String clientRegistrationId, Authentication principal, HttpServletRequest request) {
            return null;
        }

        @Override
        public void saveAuthorizedClient(
                OAuth2AuthorizedClient authorizedClient,
                Authentication principal,
                HttpServletRequest request,
                HttpServletResponse response) {}

        @Override
        public void removeAuthorizedClient(
                String clientRegistrationId,
                Authentication principal,
                HttpServletRequest request,
                HttpServletResponse response) {}
    }
}
