package com.ruoyi.cook.operation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 轮播排序调整请求。
 */
@Data
public class BannerMoveRequest
{
    @NotNull(message = "排序值不能为空")
    private Integer sortNo;
}
