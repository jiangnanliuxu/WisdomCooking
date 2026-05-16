package com.ruoyi.cook.operation.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 视频转码任务线程池，避免 FFmpeg 进程挤占 Web 请求线程。
 */
@Configuration
public class CookMediaTaskConfig
{
    @Autowired
    private CookMediaProperties mediaProperties;

    @Bean("cookMediaTaskExecutor")
    public ThreadPoolTaskExecutor cookMediaTaskExecutor()
    {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int threads = Math.max(1, mediaProperties.getTranscodeThreads());
        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cook-media-");
        executor.initialize();
        return executor;
    }
}
