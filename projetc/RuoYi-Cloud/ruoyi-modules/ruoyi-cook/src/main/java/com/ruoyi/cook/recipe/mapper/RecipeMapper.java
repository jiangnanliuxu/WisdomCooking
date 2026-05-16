package com.ruoyi.cook.recipe.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cook.recipe.domain.Recipe;
import com.ruoyi.cook.recipe.domain.vo.CategoryCountVo;
import com.ruoyi.cook.recipe.domain.vo.RecipeListItemVo;

/**
 * 菜谱主表 Mapper，负责 cook_recipes 表和列表查询。
 */
public interface RecipeMapper
{
    Recipe selectById(Long id);

    Recipe selectByIdForUpdate(Long id);

    List<RecipeListItemVo> selectPublicList(@Param("categoryCode") String categoryCode,
            @Param("difficulty") String difficulty, @Param("keyword") String keyword,
            @Param("preferredCategoryCodes") List<String> preferredCategoryCodes);

    List<RecipeListItemVo> selectMine(@Param("authorId") Long authorId, @Param("status") String status);

    List<RecipeListItemVo> selectAdminList(@Param("reviewStatus") String reviewStatus,
            @Param("publishStatus") String publishStatus, @Param("keyword") String keyword);

    List<RecipeListItemVo> selectAuditList(@Param("status") String status, @Param("keyword") String keyword);

    List<CategoryCountVo> selectCategoryCounts();

    int insertRecipe(Recipe recipe);

    int updateCategoryAndReview(@Param("id") Long id, @Param("categoryCode") String categoryCode,
            @Param("reviewStatus") String reviewStatus);

    int updateReviewStatus(@Param("id") Long id, @Param("reviewStatus") String reviewStatus);

    int updateCurrentVersionAndStatus(@Param("id") Long id, @Param("currentVersionId") Long currentVersionId,
            @Param("reviewStatus") String reviewStatus, @Param("publishStatus") String publishStatus);

    int updatePublishStatus(@Param("id") Long id, @Param("publishStatus") String publishStatus);

    int softDeleteByOwner(@Param("id") Long id, @Param("authorId") Long authorId);

    int softDeleteById(Long id);

    int increaseCounter(@Param("id") Long id, @Param("column") String column);

    int decreaseCounter(@Param("id") Long id, @Param("column") String column);
}
