package com.urlshortener.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Backs @Async methods (currently just ClickEventProducer.publish) with a small, bounded
 * thread pool instead of Spring's default — which, with no executor bean defined, spins up
 * a brand new unbounded thread per @Async call. Click-event publishing is lightweight and
 * bursty rather than sustained, so a modest core size with a bounded queue is enough to
 * smooth over traffic spikes without ever letting this background work consume unbounded
 * resources.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-task-");
        // If the queue and pool are both saturated, run on the caller thread rather than
        // reject outright — a slow click-event publish degrading gracefully is preferable
        // to a silently dropped analytics event.
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
