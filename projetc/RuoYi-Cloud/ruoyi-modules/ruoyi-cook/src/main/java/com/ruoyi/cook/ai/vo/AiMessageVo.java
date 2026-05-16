package com.ruoyi.cook.ai.vo;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 对话消息展示对象。
 */
@Data
@Schema(description = "AI对话消息")
public class AiMessageVo
{
    private Long id;
    private Long conversationId;
    private Long userId;
    private String userNickname;
    private String userAvatarUrl;
    private Long modelId;
    private String modelName;
    private String role;
    private String content;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer responseTimeMs;
    private String flag;
    private String flagReason;
    private Boolean ragHit;
    private String ragSourcesJson;
    private String fallbackReason;
    private LocalDateTime createdAt;
}
