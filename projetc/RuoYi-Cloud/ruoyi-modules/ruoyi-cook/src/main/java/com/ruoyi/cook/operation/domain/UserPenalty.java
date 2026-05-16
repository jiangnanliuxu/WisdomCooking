package com.ruoyi.cook.operation.domain;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户处罚记录。
 */
@Data
public class UserPenalty
{
    private Long id;
    private Long userId;
    private String penaltyType;
    private String reason;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Long operatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
