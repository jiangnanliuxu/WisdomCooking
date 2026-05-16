package com.ruoyi.cook.operation.dto;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 提交反馈请求。
 */
@Data
public class FeedbackCreateRequest
{
    @NotBlank(message = "反馈类型不能为空")
    private String type;

    @NotBlank(message = "反馈内容不能为空")
    private String content;

    private List<Long> mediaIds;

    private String contact;
}
