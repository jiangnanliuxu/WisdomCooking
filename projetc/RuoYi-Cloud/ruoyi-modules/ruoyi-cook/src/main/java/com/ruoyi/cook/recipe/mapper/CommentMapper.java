package com.ruoyi.cook.recipe.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cook.recipe.domain.Comment;

/**
 * 评论 Mapper，第二阶段主要服务菜谱评论。
 */
public interface CommentMapper
{
    Comment selectById(Long id);

    List<Comment> selectList(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    int insertComment(Comment comment);

    int softDeleteByOwner(@Param("id") Long id, @Param("userId") Long userId);

    int increaseLikeCount(Long id);

    int decreaseLikeCount(Long id);
}
