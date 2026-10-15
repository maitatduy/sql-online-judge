package com.sqljudge.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.common.persistence.SecurityAuditorAware;
import com.sqljudge.common.web.ApiErrorWriter;
import com.sqljudge.common.web.GlobalExceptionHandler;
import com.sqljudge.common.web.RestAccessDeniedHandler;
import com.sqljudge.common.web.RestAuthenticationEntryPoint;
import java.time.Clock;
import java.util.Optional;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.web.AuthenticationEntryPoint;

@AutoConfiguration(
        afterName = "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration")
public class CommonAutoConfiguration {

    public static final String AUDITOR_AWARE_BEAN = "securityAuditorAware";
    public static final String DATE_TIME_PROVIDER_BEAN = "clockDateTimeProvider";

    @Bean
    @ConditionalOnMissingBean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(AuthenticationEntryPoint.class)
    static class WebConfiguration {

        @Bean
        GlobalExceptionHandler globalExceptionHandler(Clock clock) {
            return new GlobalExceptionHandler(clock);
        }

        @Bean
        ApiErrorWriter apiErrorWriter(ObjectMapper objectMapper) {
            return new ApiErrorWriter(objectMapper);
        }

        @Bean
        RestAuthenticationEntryPoint restAuthenticationEntryPoint(
                ApiErrorWriter apiErrorWriter, Clock clock) {
            return new RestAuthenticationEntryPoint(apiErrorWriter, clock);
        }

        @Bean
        RestAccessDeniedHandler restAccessDeniedHandler(
                ApiErrorWriter apiErrorWriter, Clock clock) {
            return new RestAccessDeniedHandler(apiErrorWriter, clock);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({AuditingEntityListener.class, AuthenticationEntryPoint.class})
    static class JpaAuditingConfiguration {

        @Bean(AUDITOR_AWARE_BEAN)
        SecurityAuditorAware securityAuditorAware() {
            return new SecurityAuditorAware();
        }

        @Bean(DATE_TIME_PROVIDER_BEAN)
        DateTimeProvider clockDateTimeProvider(Clock clock) {
            return () -> Optional.of(clock.instant());
        }

        @Configuration(proxyBeanMethods = false)
        @ConditionalOnBean(type = "jakarta.persistence.EntityManagerFactory")
        @EnableJpaAuditing(
                auditorAwareRef = AUDITOR_AWARE_BEAN,
                dateTimeProviderRef = DATE_TIME_PROVIDER_BEAN)
        static class EnableAuditingConfiguration {}
    }
}
