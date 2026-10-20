package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MvcAsyncConfigIntegrationTest extends ContainerSupport {

    @Autowired private RequestMappingHandlerAdapter handlerAdapter;

    @Autowired
    @Qualifier("applicationTaskExecutor") private Executor applicationTaskExecutor;

    @Test
    @DisplayName(
            "Response stream như xuất CSV có thời hạn 5 phút và chạy trên executor riêng có giới"
                    + " hạn, không dùng chung executor gửi email")
    void asyncResponses_useDedicatedBoundedExecutorAndFiveMinuteTimeout() {
        Object executor = ReflectionTestUtils.getField(handlerAdapter, "taskExecutor");
        Object timeout = ReflectionTestUtils.getField(handlerAdapter, "asyncRequestTimeout");

        assertThat(timeout).isEqualTo(Duration.ofMinutes(5).toMillis());
        assertThat(executor)
                .isInstanceOfSatisfying(
                        ThreadPoolTaskExecutor.class,
                        pool -> {
                            assertThat(pool.getThreadNamePrefix()).isEqualTo("mvc-async-");
                            assertThat(pool.getMaxPoolSize()).isEqualTo(4);
                            assertThat(pool.getQueueCapacity()).isEqualTo(16);
                        })
                .isNotSameAs(applicationTaskExecutor);
    }
}
