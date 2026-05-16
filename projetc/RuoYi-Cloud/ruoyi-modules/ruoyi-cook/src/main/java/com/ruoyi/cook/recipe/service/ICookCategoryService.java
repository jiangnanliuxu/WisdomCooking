package com.ruoyi.cook.recipe.service;

import java.util.List;
import com.ruoyi.cook.recipe.domain.dto.CategorySaveRequest;
import com.ruoyi.cook.recipe.domain.vo.CategoryGroupVo;
import com.ruoyi.cook.recipe.domain.vo.CategoryVo;

/**
 * 菜谱分类服务。
 */
public interface ICookCategoryService
{
    List<CategoryGroupVo> listEnabledCategoryGroups();

    List<CategoryGroupVo> listAdminCategoryGroups();

    CategoryVo createCategory(CategorySaveRequest request);

    CategoryVo updateCategory(Long id, CategorySaveRequest request);

    void deleteCategory(Long id);

    void assertEnabledCategory(String categoryCode);
}
