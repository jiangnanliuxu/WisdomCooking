package com.ruoyi.cook.message.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 消息展示对象。
 */
@Data
@Schema(description = "消息展示对象")
public class MessageVo
{
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderNickname;
    private String senderAvatarUrl;
    private String messageType;
    private String content;
    private String mediaUrl;
    private String status;
    private LocalDateTime createdAt;
}
