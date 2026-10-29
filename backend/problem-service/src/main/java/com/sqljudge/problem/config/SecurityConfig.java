package com.sqljudge.problem.config;

import com.sqljudge.common.security.JwtCurrentUserConverter;
import com.sqljudge.common.web.RestAccessDeniedHandler;
import com.sqljudge.common.web.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.HeaderWriterFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
        "/actuator/health/**",
        "/actuator/info",
        "/actuator/prometheus",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    };

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler)
            throws Exception {
        return http.headers(headers -> headers.withObjectPostProcessor(writeHeadersEagerly()))
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
                .authorizeHttpRequests(
                        requests ->
                                requests.requestMatchers(PUBLIC_PATHS)
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.GET, "/api/tags", "/api/problems")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(
                        resourceServer ->
                                resourceServer
                                        .authenticationEntryPoint(authenticationEntryPoint)
                                        .jwt(
                                                jwt ->
                                                        jwt.jwtAuthenticationConverter(
                                                                new JwtCurrentUserConverter())))
                .build();
    }

    private static ObjectPostProcessor<HeaderWriterFilter> writeHeadersEagerly() {
        return new ObjectPostProcessor<>() {
            @Override
            public <O extends HeaderWriterFilter> O postProcess(O filter) {
                filter.setShouldWriteHeadersEagerly(true);
                return filter;
            }
        };
    }
}
