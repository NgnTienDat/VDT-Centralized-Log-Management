package com.vdt.log_monitor.alert;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class AlertSchedulerConfig {

    @Bean
    public ThreadPoolTaskScheduler alertTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(30);
        scheduler.setThreadNamePrefix("AlertTimer-");
        scheduler.initialize();
        return scheduler;
    }
}