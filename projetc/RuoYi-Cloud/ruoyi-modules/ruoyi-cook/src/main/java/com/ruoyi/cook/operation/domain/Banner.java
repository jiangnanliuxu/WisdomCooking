package com.ruoyi.cook.operation.domain;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 首页轮播图实体。
 */
@Data
public class Banner
{
    private Long id;
    private String title;
    private String subtitle;
    private Long imageMediaId;
    private String jumpType;
    private String jumpTarget;
    private Integer sortNo;
    private String status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer exposureCount;
    private Integer clickCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
