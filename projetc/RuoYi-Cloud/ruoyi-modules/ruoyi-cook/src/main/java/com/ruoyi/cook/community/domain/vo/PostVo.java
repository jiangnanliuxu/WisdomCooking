package com.ruoyi.cook.community.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 动态展示 VO。
 * <p>
 * mediaIds、topicCodes 由 cook_posts 表中的 JSON 字段解析而来，
 * liked/favorited 则由当前登录用户的互动记录补充。
 * </p>
 */
@Data
@Schema(description = "社区动态展示对象")
public class PostVo
{
    private Long id;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String content;
    private String visibility;
    private List<Long> mediaIds;
    private List<String> topicCodes;
    private String location;
    private Long relatedRecipeId;
    private String relatedRecipeTitle;
    private String relatedRecipeCategoryCode;
    private String sourceType;
    private String status;
    private String rejectReason;
    private String blockReason;
    private String blockAction;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private Boolean liked = false;
    private Boolean favorited = false;
    private Boolean authorFollowed = false;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
