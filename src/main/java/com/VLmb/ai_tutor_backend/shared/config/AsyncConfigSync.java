package com.VLmb.ai_tutor_backend.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@Configuration
@Profile("sync")
public class AsyncConfigSync {

    @Bean("ragExecutor")
    public TaskExecutor ragExecutor() {
        return new SyncTaskExecutor();
    }

    @Bean("dbExecutor")
    public TaskExecutor dbExecutor() {
        return new SyncTaskExecutor();
    }
}
