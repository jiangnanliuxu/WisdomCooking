package com.ruoyi.cook.community.domain;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 社区动态主实体，对应 cook_posts 表。
 * <p>
 * 第三阶段采用压缩表设计：图片、话题等扩展信息用 JSON 字段保存，
 * 避免在首版引入 post_media、post_topics 等过多明细表。
 * </p>
 */
@Data
public class Post
{
    private Long id;
    private Long userId;
    private String content;
    private String visibility;
    private String mediaIdsJson;
    private String topicCodesJson;
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
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    /** 列表查询时冗余返回作者昵称，避免业务层再逐条查用户。 */
    private String nickname;

    /** 列表查询时冗余返回作者头像。 */
    private String avatarUrl;
}
