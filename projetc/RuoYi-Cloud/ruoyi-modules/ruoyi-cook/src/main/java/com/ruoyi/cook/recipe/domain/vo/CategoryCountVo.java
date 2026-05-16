package com.ruoyi.cook.recipe.domain.vo;

import lombok.Data;

/**
 * 分类下菜谱数量聚合结果。
 */
@Data
public class CategoryCountVo
{
    private String categoryCode;
    private Long recipeCount;
}
