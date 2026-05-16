package com.ruoyi.cook.operation.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 举报视图对象。
 */
@Data
public class ReportVo
{
    private Long id;
    private Long reporterId;
    private String reporterNickname;
    private String targetType;
    private Long targetId;
    private String reasonType;
    private String reason;
    private List<Long> mediaIds;
    private String status;
    private Long handlerId;
    private String handleResult;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}
