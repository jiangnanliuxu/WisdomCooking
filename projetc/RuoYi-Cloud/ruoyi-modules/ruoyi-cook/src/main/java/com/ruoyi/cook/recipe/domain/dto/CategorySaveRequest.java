package com.ruoyi.cook.recipe.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 菜谱分类新增 / 编辑请求。
 */
@Data
public class CategorySaveRequest
{
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 32, message = "分类名称不能超过32个字符")
    private String name;

    @Size(max = 64, message = "分类图标不能超过64个字符")
    private String icon;

    @Size(max = 32, message = "图标颜色不能超过32个字符")
    private String color;

    @Size(max = 255, message = "分类描述不能超过255个字符")
    private String description;

    private Integer sortNo;

    private String status;
}
