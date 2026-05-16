package com.ruoyi.cook.ai.config;

import java.util.List;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.embedding.text.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.text.DashScopeEmbeddingOptions;
import com.alibaba.cloud.ai.tool.validator.DefaultToolCallValidator;

import io.micrometer.observation.ObservationRegistry;

/**
 * 显式装配 Spring AI Alibaba DashScope，避开当前 starter 中缺失的自动配置类。
 */
@Configuration
public class DashScopeAiConfiguration
{
    @Bean
    @ConditionalOnMissingBean
    public DashScopeApi dashScopeApi(
            @Value("${spring.ai.dashscope.api-key:}") String apiKey,
            @Value("${spring.ai.dashscope.base-url:https://dashscope.aliyuncs.com}") String baseUrl)
    {
        return DashScopeApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .restClientBuilder(RestClient.builder())
                .webClientBuilder(WebClient.builder())
                .responseErrorHandler(RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    public ChatModel dashScopeChatModel(DashScopeApi dashScopeApi,
            @Value("${spring.ai.dashscope.chat.options.model:qwen-plus}") String model)
    {
        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder().model(model).build())
                .retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
                .observationRegistry(ObservationRegistry.NOOP)
                .toolCallingManager(DefaultToolCallingManager.builder()
                        .observationRegistry(ObservationRegistry.NOOP)
                        .toolCallbackResolver(new StaticToolCallbackResolver(List.of()))
                        .toolExecutionExceptionProcessor(DefaultToolExecutionExceptionProcessor.builder()
                                .alwaysThrow(true)
                                .build())
                        .build())
                .toolExecutionEligibilityPredicate(new DefaultToolExecutionEligibilityPredicate())
                .toolCallValidator(new DefaultToolCallValidator())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel dashScopeEmbeddingModel(DashScopeApi dashScopeApi,
            @Value("${spring.ai.dashscope.embedding.options.model:text-embedding-v3}") String model,
            @Value("${spring.ai.dashscope.embedding.options.dimensions:1024}") Integer dimensions)
    {
        return DashScopeEmbeddingModel.builder()
                .dashScopeApi(dashScopeApi)
                .metadataMode(MetadataMode.EMBED)
                .defaultOptions(DashScopeEmbeddingOptions.builder()
                        .model(model)
                        .dimensions(dimensions)
                        .build())
                .retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
    }
}
