package com.ruoyi.cook.user.domain.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 我的点赞 / 我的收藏统一视图。
 */
@Data
public class UserInteractionVo
{
    private String targetType;

    private Long targetId;

    private String actionType;

    private String title;

    private String content;

    private String coverUrl;

    private String authorNickname;

    private LocalDateTime interactedAt;
}
