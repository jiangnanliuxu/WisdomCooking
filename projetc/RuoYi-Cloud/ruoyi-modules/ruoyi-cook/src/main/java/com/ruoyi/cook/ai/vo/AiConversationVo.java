package com.ruoyi.cook.ai.vo;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 会话摘要，用于用户端和管理端列表。
 */
@Data
@Schema(description = "AI会话摘要")
public class AiConversationVo
{
    private Long conversationId;
    private Long userId;
    private String userNickname;
    private String userAvatarUrl;
    private Long modelId;
    private String modelName;
    private String modelType;
    private String title;
    private String lastMessage;
    private Integer rounds;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private Integer responseTimeMs;
    private String flag;
    private String flagReason;
    private Boolean ragHit;
    private String fallbackReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
