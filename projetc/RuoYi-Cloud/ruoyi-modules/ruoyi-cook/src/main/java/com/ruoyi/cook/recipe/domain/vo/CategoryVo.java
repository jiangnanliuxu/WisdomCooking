package com.ruoyi.cook.recipe.domain.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 菜谱分类项。
 */
@Data
@NoArgsConstructor
public class CategoryVo
{
    private Long id;
    private String code;
    private String name;
    private String icon;
    private String color;
    private String description;
    private String groupCode;
    private Integer sortNo;
    private String status;
    private Boolean readonly;
    private Long recipeCount;

    public CategoryVo(String code, String name)
    {
        this.code = code;
        this.name = name;
    }

    public CategoryVo(String code, String name, Long recipeCount)
    {
        this.code = code;
        this.name = name;
        this.recipeCount = recipeCount;
    }

}
