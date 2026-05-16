package com.ruoyi.cook.recipe.domain;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 菜谱分类实体，对应 cook_categories。
 */
@Data
public class RecipeCategory
{
    private Long id;
    private String categoryCode;
    private String name;
    private String icon;
    private String color;
    private String description;
    private String groupCode;
    private Integer sortNo;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
