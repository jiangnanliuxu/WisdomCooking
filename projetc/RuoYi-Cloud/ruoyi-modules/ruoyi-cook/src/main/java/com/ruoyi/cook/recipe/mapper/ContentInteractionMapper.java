package com.ruoyi.cook.recipe.mapper;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.cook.recipe.domain.ContentInteraction;

/**
 * 统一互动 Mapper，点赞、收藏、分享都落到 cook_content_interactions。
 */
public interface ContentInteractionMapper
{
    ContentInteraction selectOne(@Param("userId") Long userId, @Param("targetType") String targetType,
            @Param("targetId") Long targetId, @Param("actionType") String actionType);

    int insertInteraction(ContentInteraction interaction);

    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
