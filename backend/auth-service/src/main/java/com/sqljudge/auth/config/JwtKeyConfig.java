package com.sqljudge.auth.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class JwtKeyConfig {

    @Bean
    RSAKey jwtSigningKey(JwtProperties properties) {
        return JwtSigningKeyFactory.create(properties);
    }

    @Bean
    JwtDecoder jwtDecoder(RSAKey jwtSigningKey) throws JOSEException {
        return NimbusJwtDecoder.withPublicKey(jwtSigningKey.toRSAPublicKey()).build();
    }
}
