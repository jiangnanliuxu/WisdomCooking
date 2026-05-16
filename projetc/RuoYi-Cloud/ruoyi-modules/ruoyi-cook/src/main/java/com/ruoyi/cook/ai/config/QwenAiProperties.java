package com.ruoyi.cook.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

/**
 * 阿里云百炼通义千问配置。
 * <p>
 * 配置来源为独立的 cook-ai.yml。对话模型切换后优先读取数据库配置，
 * 这里仅作为阿里云千问的默认 Key/Base URL 兜底；视觉模型仍使用本配置控制默认千问模型。
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "cook.ai.qwen")
public class QwenAiProperties
{
    /** 是否启用千问兜底配置；对话模型仍以数据库默认 chat 模型为准。 */
    private boolean enabled;

    /** OpenAI 兼容接口 Base URL，例如 https://dashscope.aliyuncs.com/compatible-mode/v1。 */
    private String baseUrl;

    /** 阿里云百炼 API Key，建议由 DASHSCOPE_API_KEY 环境变量注入。 */
    private String apiKey;

    /** HTTP 请求超时时间，单位秒。 */
    private int timeoutSeconds = 60;

    /** AI 问答模型配置。 */
    private Model chat = new Model();

    /** AI 识图模型配置。 */
    private Model vision = new Model();

    @Data
    public static class Model
    {
        /** 百炼模型名称，例如 qwen-plus、qwen-vl-plus。 */
        private String modelName;
    }
}
