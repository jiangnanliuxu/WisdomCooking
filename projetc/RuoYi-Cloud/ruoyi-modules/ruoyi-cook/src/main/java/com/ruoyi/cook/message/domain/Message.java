package com.ruoyi.cook.message.domain;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息实体，对应 cook_messages 表。
 * <p>
 * contentJson 保存消息主体，便于后续扩展图片、语音、系统通知等不同类型。
 * </p>
 */
@Data
public class Message
{
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String messageType;
    private String contentJson;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    /** 发送者昵称，列表查询时由 cook_users 表带出。 */
    private String senderNickname;

    /** 发送者头像，列表查询时由 cook_users 表带出。 */
    private String senderAvatarUrl;
}
