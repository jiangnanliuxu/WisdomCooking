package com.ruoyi.cook.recipe.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.cook.recipe.domain.Recipe;
import com.ruoyi.cook.recipe.domain.RecipeVersion;

/**
 * 菜谱详情，JSON 字段在返回前转换成结构化对象。
 */
@Data
public class RecipeDetailVo
{
    private Long id;
    private Long authorId;
    private String authorNickname;
    private Long currentVersionId;
    private Long versionId;
    private Integer versionNo;
    private String title;
    private Long coverMediaId;
    private String intro;
    private String categoryCode;
    private String difficulty;
    private String cookTime;
    private String serving;
    private List<Object> ingredients;
    private List<Object> steps;
    private List<Object> tips;
    private Map<String, Object> video;
    private String reviewStatus;
    private String publishStatus;
    private String versionStatus;
    private String rejectReason;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private Boolean liked = false;
    private Boolean favorited = false;
    private Boolean authorFollowed = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RecipeDetailVo from(Recipe recipe, RecipeVersion version, String authorNickname)
    {
        RecipeDetailVo vo = new RecipeDetailVo();
        vo.setId(recipe.getId());
        vo.setAuthorId(recipe.getAuthorId());
        vo.setAuthorNickname(authorNickname);
        vo.setCurrentVersionId(recipe.getCurrentVersionId());
        vo.setVersionId(version.getId());
        vo.setVersionNo(version.getVersionNo());
        vo.setTitle(version.getTitle());
        vo.setCoverMediaId(version.getCoverMediaId());
        vo.setIntro(version.getIntro());
        vo.setCategoryCode(recipe.getCategoryCode());
        vo.setDifficulty(version.getDifficulty());
        vo.setCookTime(version.getCookTime());
        vo.setServing(version.getServing());
        vo.setIngredients(parseList(version.getIngredientsJson()));
        vo.setSteps(parseList(version.getStepsJson()));
        vo.setTips(parseList(version.getTipsJson()));
        vo.setVideo(parseMap(version.getVideoJson()));
        vo.setReviewStatus(recipe.getReviewStatus());
        vo.setPublishStatus(recipe.getPublishStatus());
        vo.setVersionStatus(version.getStatus());
        vo.setRejectReason(version.getRejectReason());
        vo.setLikeCount(recipe.getLikeCount());
        vo.setFavoriteCount(recipe.getFavoriteCount());
        vo.setCommentCount(recipe.getCommentCount());
        vo.setCreatedAt(recipe.getCreatedAt());
        vo.setUpdatedAt(recipe.getUpdatedAt());
        return vo;
    }

    private static List<Object> parseList(String json)
    {
        if (json == null || json.isBlank())
        {
            return Collections.emptyList();
        }
        return JSON.parseArray(json, Object.class);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseMap(String json)
    {
        if (json == null || json.isBlank())
        {
            return Collections.emptyMap();
        }
        return JSON.parseObject(json, Map.class);
    }

}
