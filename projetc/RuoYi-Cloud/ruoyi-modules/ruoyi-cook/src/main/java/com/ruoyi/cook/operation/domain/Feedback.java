package com.ruoyi.cook.operation.domain;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户反馈实体。
 */
@Data
public class Feedback
{
    private Long id;
    private Long userId;
    private String type;
    private String content;
    private String mediaIdsJson;
    private String contact;
    private String status;
    private String replyContent;
    private LocalDateTime repliedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
