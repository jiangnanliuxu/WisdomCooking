package com.ruoyi.cook.recipe.domain;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Recipe implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long authorId;
    private Long currentVersionId;
    private String categoryCode;
    private String reviewStatus;
    private String publishStatus;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

}
