package com.ruoyi.cook.operation.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 最近操作日志视图对象。
 */
@Data
public class RecentOperationLogVo
{
    private Long id;
    private Long adminId;
    private String bizType;
    private Long bizId;
    private String action;
    private String remark;
    private LocalDateTime createdAt;
}
