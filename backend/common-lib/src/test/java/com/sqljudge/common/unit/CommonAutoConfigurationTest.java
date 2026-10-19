package com.sqljudge.common.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.common.web.GlobalExceptionHandler;
import com.sqljudge.common.web.OptimisticLockExceptionHandler;
import com.sqljudge.common.web.RestAccessDeniedHandler;
import com.sqljudge.common.web.RestAuthenticationEntryPoint;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.data.auditing.DateTimeProvider;

class CommonAutoConfigurationTest {

    private static final AutoConfigurations CONFIGURATIONS =
            AutoConfigurations.of(JacksonAutoConfiguration.class, CommonAutoConfiguration.class);

    @Test
    @DisplayName("Ứng dụng web có sẵn handler lỗi, entry point, auditor và đồng hồ UTC")
    void webApplication_registersCommonBeans() {
        new WebApplicationContextRunner()
                .withConfiguration(CONFIGURATIONS)
                .run(
                        context -> {
                            assertThat(context)
                                    .hasSingleBean(GlobalExceptionHandler.class)
                                    .hasSingleBean(OptimisticLockExceptionHandler.class)
                                    .hasSingleBean(RestAuthenticationEntryPoint.class)
                                    .hasSingleBean(RestAccessDeniedHandler.class)
                                    .hasBean(CommonAutoConfiguration.AUDITOR_AWARE_BEAN)
                                    .hasBean(CommonAutoConfiguration.DATE_TIME_PROVIDER_BEAN);
                            assertThat(context.getBean(Clock.class).getZone())
                                    .isEqualTo(ZoneOffset.UTC);
                        });
    }

    @Test
    @DisplayName("Ứng dụng không phải web thì không đăng ký handler HTTP")
    void nonWebApplication_skipsWebBeans() {
        new ApplicationContextRunner()
                .withConfiguration(CONFIGURATIONS)
                .run(context -> assertThat(context).doesNotHaveBean(GlobalExceptionHandler.class));
    }

    @Test
    @DisplayName("Service tự khai báo Clock thì dùng Clock đó cho thời điểm audit")
    void customClock_isUsedByDateTimeProvider() {
        Instant fixedNow = Instant.parse("2026-10-11T13:05:22Z");
        new ApplicationContextRunner()
                .withConfiguration(CONFIGURATIONS)
                .withBean(Clock.class, () -> Clock.fixed(fixedNow, ZoneOffset.UTC))
                .run(
                        context ->
                                assertThat(
                                                context.getBean(
                                                                CommonAutoConfiguration
                                                                        .DATE_TIME_PROVIDER_BEAN,
                                                                DateTimeProvider.class)
                                                        .getNow())
                                        .contains(fixedNow));
    }
}
