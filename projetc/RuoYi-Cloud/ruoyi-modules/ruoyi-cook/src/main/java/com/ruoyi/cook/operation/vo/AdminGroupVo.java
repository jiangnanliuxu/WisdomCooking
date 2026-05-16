package com.ruoyi.cook.operation.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 管理端群组视图对象。
 */
@Data
public class AdminGroupVo
{
    private Long id;
    private Long ownerId;
    private String ownerNickname;
    private Long conversationId;
    private String name;
    private Long avatarMediaId;
    private String avatarUrl;
    private String intro;
    private String notice;
    private String status;
    private Long memberCount;
    private Long messageCount;
    private LocalDateTime createdAt;
}
