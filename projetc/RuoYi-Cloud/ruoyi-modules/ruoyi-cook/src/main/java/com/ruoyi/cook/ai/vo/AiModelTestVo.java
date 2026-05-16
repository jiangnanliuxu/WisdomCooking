package com.ruoyi.cook.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 模型连通测试结果。
 */
@Data
@Schema(description = "AI模型连通测试结果")
public class AiModelTestVo
{
    private Long modelId;
    private String status;
    private Integer latencyMs;
    private String message;
}
