package com.ruoyi.cook.recipe.domain;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Comment implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String targetType;
    private Long targetId;
    private Long userId;
    private Long parentId;
    private String content;
    private Integer likeCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private String nickname;
    private String avatarUrl;

}
