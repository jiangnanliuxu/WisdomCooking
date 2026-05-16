package com.ruoyi.cook.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 饮食问答请求。
 */
@Data
@Schema(description = "AI饮食问答请求")
public class AiChatRequest
{
    @Schema(description = "已有会话ID，不传则自动创建新会话")
    private Long conversationId;

    @NotBlank(message = "问题不能为空")
    @Size(max = 1000, message = "问题不能超过1000个字符")
    @Schema(description = "用户问题", example = "请推荐一道减脂晚餐")
    private String question;

    @Size(max = 30, message = "对话类型不能超过30个字符")
    @Schema(description = "对话类型：diet_advice/calorie_query/nutrition_analysis", example = "diet_advice")
    private String conversationType;
}
