package com.ruoyi.cook.operation.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 反馈视图对象。
 */
@Data
public class FeedbackVo
{
    private Long id;
    private Long userId;
    private String userNickname;
    private String type;
    private String content;
    private List<Long> mediaIds;
    private String contact;
    private String status;
    private String replyContent;
    private LocalDateTime repliedAt;
    private LocalDateTime createdAt;
}
