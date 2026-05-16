package com.ruoyi.cook.operation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端处理反馈请求。
 */
@Data
public class FeedbackHandleRequest
{
    @NotBlank(message = "反馈状态不能为空")
    private String status;

    private String replyContent;
}
