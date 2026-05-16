package com.ruoyi.cook.operation.domain;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 举报实体。
 */
@Data
public class Report
{
    private Long id;
    private Long reporterId;
    private String targetType;
    private Long targetId;
    private String reasonType;
    private String reason;
    private String mediaIdsJson;
    private String status;
    private Long handlerId;
    private String handleResult;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
