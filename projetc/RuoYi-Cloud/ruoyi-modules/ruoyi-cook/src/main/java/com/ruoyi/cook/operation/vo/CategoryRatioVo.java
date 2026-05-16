package com.ruoyi.cook.operation.vo;

import lombok.Data;

/**
 * 菜谱分类占比。
 */
@Data
public class CategoryRatioVo
{
    private String categoryCode;
    private String categoryName;
    private Long count;
}
