package com.ruoyi.cook.recipe.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评论展示对象，附带评论用户的基础资料。
 */
@Data
public class CommentVo
{
    private Long id;
    private String targetType;
    private Long targetId;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Long parentId;
    private String content;
    private Integer likeCount;
    private Boolean liked = false;
    private LocalDateTime createdAt;
}
