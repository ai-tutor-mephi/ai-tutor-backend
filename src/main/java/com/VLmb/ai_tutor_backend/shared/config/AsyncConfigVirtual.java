package com.VLmb.ai_tutor_backend.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;

@Configuration
@Profile("virtual")
public class AsyncConfigVirtual {

    @Bean("ragExecutor")
    public TaskExecutor ragExecutor() {
        return new VirtualThreadTaskExecutor("rag-vt-");
    }

    @Bean("dbExecutor")
    public TaskExecutor dbExecutor() {
        return new VirtualThreadTaskExecutor("db-vt-");
    }
}
