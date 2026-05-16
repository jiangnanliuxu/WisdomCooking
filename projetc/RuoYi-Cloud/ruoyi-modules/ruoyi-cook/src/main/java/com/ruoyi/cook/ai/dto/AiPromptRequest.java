package com.ruoyi.cook.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端保存 Prompt 和 Few-shot 请求。
 */
@Data
@Schema(description = "AI模型Prompt保存请求")
public class AiPromptRequest
{
    @Schema(description = "系统提示词")
    private String systemPrompt;

    @Schema(description = "Few-shot预设问答JSON，将写入configJson.fewShotExamples")
    private String fewShotExamplesJson;
}
