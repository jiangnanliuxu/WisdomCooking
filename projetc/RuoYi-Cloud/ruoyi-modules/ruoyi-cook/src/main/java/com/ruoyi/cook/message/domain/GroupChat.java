package com.ruoyi.cook.message.domain;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 群组实体，对应数据库 cook_groups 表。
 * <p>
 * 类名使用 GroupChat，避免与 Java/SQL 中的 group 概念混淆。
 * </p>
 */
@Data
public class GroupChat
{
    private Long id;
    private Long ownerId;
    private Long conversationId;
    private String name;
    private Long avatarMediaId;
    private String intro;
    private String notice;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    /** 群主昵称，详情查询时由 cook_users 表带出。 */
    private String ownerNickname;

    /** 当前群成员数，详情查询时实时聚合。 */
    private Integer memberCount;

    /** 当前群消息数，详情查询时实时聚合。 */
    private Integer messageCount;
}
