package com.sqljudge.gateway.config;

import com.sqljudge.gateway.security.ApiErrorAuthenticationEntryPoint;
import java.time.Clock;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            GlobalCorsProperties corsProperties,
            ApiErrorAuthenticationEntryPoint authenticationEntryPoint) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .requestCache(cache -> cache.requestCache(NoOpServerRequestCache.getInstance()))
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .cors(cors -> cors.configurationSource(corsConfigurationSource(corsProperties)))
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .oauth2ResourceServer(
                        resourceServer ->
                                resourceServer
                                        .authenticationEntryPoint(authenticationEntryPoint)
                                        .jwt(Customizer.withDefaults()))
                .build();
    }

    private static CorsConfigurationSource corsConfigurationSource(
            GlobalCorsProperties corsProperties) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.setCorsConfigurations(corsProperties.getCorsConfigurations());
        return source;
    }
}
