package com.ruoyi.cook.ai.domain;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * AI 对话消息实体，对应 cook_ai_messages 表。
 * <p>
 * 当前压缩表设计不单独创建 ai_conversations，会话通过 conversationId 分组；
 * 每轮问答写入 user 与 assistant 两条消息，便于管理端按会话回放上下文。
 * </p>
 */
@Data
public class AiMessage
{
    private Long id;
    private Long conversationId;
    private Long userId;
    private Long modelId;
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
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    /** 管理端列表展示用：用户昵称。 */
    private String userNickname;

    /** 管理端列表展示用：用户头像。 */
    private String userAvatarUrl;

    /** 管理端列表展示用：模型名称。 */
    private String modelName;

    /** 管理端列表展示用：模型类型。 */
    private String modelType;
}
