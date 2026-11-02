package com.sqljudge.problem.config;

import com.sqljudge.common.web.RestAccessDeniedHandler;
import com.sqljudge.common.web.RestAuthenticationEntryPoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
@EnableConfigurationProperties(InternalApiProperties.class)
public class InternalSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain internalSecurityFilterChain(
            HttpSecurity http,
            InternalApiProperties properties,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler)
            throws Exception {
        return http.securityMatcher("/internal/**")
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(
                        exceptions ->
                                exceptions
                                        .authenticationEntryPoint(authenticationEntryPoint)
                                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(
                        new InternalTokenFilter(properties.apiToken()),
                        AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(
                        requests -> requests.anyRequest().hasRole(InternalTokenFilter.ROLE))
                .build();
    }
}
