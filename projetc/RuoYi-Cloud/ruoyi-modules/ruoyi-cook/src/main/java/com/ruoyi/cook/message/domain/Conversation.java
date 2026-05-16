package com.ruoyi.cook.message.domain;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 会话实体，对应 cook_conversations 表。
 * <p>
 * 私信、群聊、通知三类入口统一抽象为会话，消息中心按 type 切换 Tab。
 * </p>
 */
@Data
public class Conversation
{
    private Long id;
    private String type;
    private Long targetId;
    private Long lastMessageId;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    /** 当前登录用户在该会话中的未读数。 */
    private Integer unreadCount;

    /** 当前登录用户是否免打扰。 */
    private Boolean muted;

    /** 当前登录用户是否置顶。 */
    private Boolean pinned;

    /** 会话展示标题：私信为对方昵称，群聊为群名，通知为系统通知。 */
    private String title;

    /** 会话头像：私信为对方头像，群聊为群头像资源占位。 */
    private String avatarUrl;
}
