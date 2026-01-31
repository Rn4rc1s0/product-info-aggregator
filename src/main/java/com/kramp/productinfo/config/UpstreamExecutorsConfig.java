package com.kramp.productinfo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class UpstreamExecutorsConfig {

    @Bean(destroyMethod = "close")
    public ExecutorService upstreamExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(destroyMethod = "close")
    public ScheduledExecutorService upstreamScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "upstream-scheduler");
            t.setDaemon(true);
            return t;
        });
    }
}
