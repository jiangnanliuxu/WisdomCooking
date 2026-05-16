package com.ruoyi.cook.message.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 消息中心会话列表展示对象。
 */
@Data
@Schema(description = "会话展示对象")
public class ConversationVo
{
    private Long id;
    private String type;
    private Long targetId;
    private String title;
    private String avatarUrl;
    private Long lastMessageId;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private Integer unreadCount;
    private Boolean muted;
    private Boolean pinned;
}
