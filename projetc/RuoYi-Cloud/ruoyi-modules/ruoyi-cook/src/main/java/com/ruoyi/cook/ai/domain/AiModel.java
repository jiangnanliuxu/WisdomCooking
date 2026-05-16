package com.ruoyi.cook.ai.domain;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * AI 模型配置实体，对应 cook_ai_models 表。
 * <p>
 * 第五阶段要求对话模型和视觉模型分开维护，因此 modelType 必须明确区分 chat/vision。
 * configJson 用于保存温度、最大 Token、Top-P、Few-shot 等可变配置，避免继续拆表。
 * </p>
 */
@Data
public class AiModel
{
    private Long id;
    private String name;
    private String modelType;
    private String provider;
    private String modelCode;
    private String encryptedApiKey;
    private String apiBaseUrl;
    private String configJson;
    private String systemPrompt;
    private Boolean isDefault;
    private String status;
    private String lastTestStatus;
    private Integer lastTestLatencyMs;
    private String lastTestMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
