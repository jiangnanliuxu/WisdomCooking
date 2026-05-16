package com.ruoyi.cook.recipe.mapper;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.cook.recipe.domain.RecipeVersion;

/**
 * 菜谱版本 Mapper，版本表承载草稿、待审核版本和历史版本。
 */
public interface RecipeVersionMapper
{
    RecipeVersion selectById(Long id);

    RecipeVersion selectLatestByRecipeId(Long recipeId);

    RecipeVersion selectLatestByRecipeIdForUpdate(Long recipeId);

    RecipeVersion selectCurrentByRecipeId(Long recipeId);

    RecipeVersion selectPendingByRecipeId(Long recipeId);

    Integer selectMaxVersionNo(Long recipeId);

    int insertVersion(RecipeVersion version);

    int updateVersion(RecipeVersion version);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int updateReject(@Param("id") Long id, @Param("status") String status, @Param("rejectReason") String rejectReason);

    int updateVideoJson(@Param("id") Long id, @Param("videoJson") String videoJson);
}
