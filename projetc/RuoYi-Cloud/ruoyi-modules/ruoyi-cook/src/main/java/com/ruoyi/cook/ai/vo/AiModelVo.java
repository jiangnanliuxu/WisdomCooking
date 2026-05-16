package com.ruoyi.cook.ai.vo;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 模型配置展示对象。
 */
@Data
@Schema(description = "AI模型配置")
public class AiModelVo
{
    private Long id;
    private String name;
    private String modelType;
    private String provider;
    private String modelCode;
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
}
