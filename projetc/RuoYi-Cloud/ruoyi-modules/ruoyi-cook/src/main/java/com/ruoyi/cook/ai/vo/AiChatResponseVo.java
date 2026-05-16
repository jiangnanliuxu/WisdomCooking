package com.ruoyi.cook.ai.vo;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 问答响应对象。
 */
@Data
@Schema(description = "AI问答响应")
public class AiChatResponseVo
{
    private Long conversationId;
    private Long userMessageId;
    private Long assistantMessageId;
    private Long modelId;
    private String modelName;
    private String answer;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer latencyMs;
    private String flag;
    private String disclaimer;
    private Boolean ragHit;
    private List<AiRagSourceVo> sources;
    private String fallbackReason;
}
