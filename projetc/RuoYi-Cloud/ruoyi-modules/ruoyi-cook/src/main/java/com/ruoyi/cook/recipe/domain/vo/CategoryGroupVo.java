package com.ruoyi.cook.recipe.domain.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * 固定分类分组，前端按组渲染分类入口。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryGroupVo
{
    private String code;
    private String name;
    private List<CategoryVo> children;

}
