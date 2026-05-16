package com.ruoyi.cook.recipe.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cook.recipe.domain.RecipeCategory;

/**
 * 菜谱分类 Mapper。
 */
public interface CategoryMapper
{
    List<RecipeCategory> selectList(@Param("status") String status);

    RecipeCategory selectById(Long id);

    RecipeCategory selectByCode(@Param("categoryCode") String categoryCode);

    int countByCode(@Param("categoryCode") String categoryCode);

    int countEnabledByCode(@Param("categoryCode") String categoryCode);

    int insertCategory(RecipeCategory category);

    int updateCategory(RecipeCategory category);

    int softDeleteById(Long id);
}
