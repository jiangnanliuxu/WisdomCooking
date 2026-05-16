package com.ruoyi.cook.operation.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 轮播图视图对象。
 */
@Data
public class BannerVo
{
    private Long id;
    private String title;
    private String subtitle;
    private Long imageMediaId;
    private String imageUrl;
    private String jumpType;
    private String jumpTarget;
    private Integer sortNo;
    private String status;
    private Integer clickCount;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime createdAt;
}
