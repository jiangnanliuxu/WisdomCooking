package com.ruoyi.cook.operation.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 轮播图新增 / 编辑请求。
 */
@Data
public class BannerSaveRequest
{
    @NotBlank(message = "标题不能为空")
    private String title;

    private String subtitle;

    @NotNull(message = "轮播图片资源不能为空")
    private Long imageMediaId;

    @NotBlank(message = "跳转类型不能为空")
    private String jumpType;

    private String jumpTarget;

    private Integer sortNo;

    private String status;

    private LocalDateTime startAt;

    private LocalDateTime endAt;
}
