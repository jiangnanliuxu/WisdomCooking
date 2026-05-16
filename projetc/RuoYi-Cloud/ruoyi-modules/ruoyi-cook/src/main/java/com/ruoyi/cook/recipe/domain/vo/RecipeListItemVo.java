package com.ruoyi.cook.recipe.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 菜谱列表项，公开列表、我的菜谱和后台列表复用。
 */
@Data
public class RecipeListItemVo
{
    private Long id;
    private Long authorId;
    private String authorNickname;
    private Long versionId;
    private Integer versionNo;
    private String title;
    private Long coverMediaId;
    private String intro;
    private String categoryCode;
    private String difficulty;
    private String cookTime;
    private String serving;
    private String reviewStatus;
    private String publishStatus;
    private String versionStatus;
    private String rejectReason;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private Boolean liked = false;
    private Boolean favorited = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
