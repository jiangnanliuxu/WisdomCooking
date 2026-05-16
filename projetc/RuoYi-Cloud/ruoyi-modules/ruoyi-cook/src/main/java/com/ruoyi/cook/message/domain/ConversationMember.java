package com.ruoyi.cook.message.domain;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 会话成员实体。
 * <p>
 * 群成员关系和私信参与人都复用该表；群聊角色通过 role 字段区分。
 * </p>
 */
@Data
public class ConversationMember
{
    private Long id;
    private Long conversationId;
    private Long userId;
    private String role;
    private String status;
    private Integer unreadCount;
    private Boolean muted;
    private Boolean pinned;
    private Long lastReadMessageId;
    private LocalDateTime joinedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 成员昵称，群成员列表查询时由 cook_users 表带出。 */
    private String nickname;

    /** 成员头像，群成员列表查询时由 cook_users 表带出。 */
    private String avatarUrl;
}
