package com.ruoyi.cook.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端 AI 模型保存请求。
 */
@Data
@Schema(description = "AI模型保存请求")
public class AiModelRequest
{
    @NotBlank(message = "模型名称不能为空")
    @Size(max = 100, message = "模型名称不能超过100个字符")
    @Schema(description = "模型展示名称", example = "通义千问 Plus")
    private String name;

    @NotBlank(message = "模型类型不能为空")
    @Size(max = 30, message = "模型类型不能超过30个字符")
    @Schema(description = "模型类型：chat/vision", example = "chat")
    private String modelType;

    @NotBlank(message = "供应商不能为空")
    @Size(max = 60, message = "供应商不能超过60个字符")
    @Schema(description = "供应商：aliyun-qwen/deepseek/zhipu/baidu-qianfan/openai-compatible", example = "aliyun-qwen")
    private String provider;

    @NotBlank(message = "模型标识不能为空")
    @Size(max = 120, message = "模型标识不能超过120个字符")
    @Schema(description = "模型标识", example = "qwen-plus")
    private String modelCode;

    @Size(max = 1000, message = "API Key密文不能超过1000个字符")
    @Schema(description = "API Key密文，编辑时不传或传空表示保持原Key不变")
    private String encryptedApiKey;

    @Size(max = 500, message = "API地址不能超过500个字符")
    @Schema(description = "API Base URL")
    private String apiBaseUrl;

    @Schema(description = "模型参数JSON，例如 temperature/max_tokens/top_p/fewShotExamples")
    private String configJson;

    @Schema(description = "系统提示词")
    private String systemPrompt;

    @Schema(description = "是否设为该类型默认模型")
    private Boolean isDefault;

    @Size(max = 30, message = "状态不能超过30个字符")
    @Schema(description = "状态：enabled/disabled", example = "enabled")
    private String status;
}
