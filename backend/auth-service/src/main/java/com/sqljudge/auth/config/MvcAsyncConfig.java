package com.sqljudge.auth.config;

import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class MvcAsyncConfig implements WebMvcConfigurer {

    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 4;
    private static final int QUEUE_CAPACITY = 16;
    private static final int SHUTDOWN_WAIT_SECONDS = 30;

    private final ThreadPoolTaskExecutor executor = createExecutor();

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(executor);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }

    private static ThreadPoolTaskExecutor createExecutor() {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setThreadNamePrefix("mvc-async-");
        pool.setCorePoolSize(CORE_POOL_SIZE);
        pool.setMaxPoolSize(MAX_POOL_SIZE);
        pool.setQueueCapacity(QUEUE_CAPACITY);
        pool.setWaitForTasksToCompleteOnShutdown(true);
        pool.setAwaitTerminationSeconds(SHUTDOWN_WAIT_SECONDS);
        pool.initialize();
        return pool;
    }
}
