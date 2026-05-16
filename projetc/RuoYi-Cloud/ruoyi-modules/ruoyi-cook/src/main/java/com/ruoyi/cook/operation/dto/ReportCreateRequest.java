package com.ruoyi.cook.operation.dto;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交举报请求。
 */
@Data
public class ReportCreateRequest
{
    @NotBlank(message = "举报对象类型不能为空")
    private String targetType;

    @NotNull(message = "举报对象ID不能为空")
    private Long targetId;

    @NotBlank(message = "举报原因类型不能为空")
    private String reasonType;

    private String reason;

    private List<Long> mediaIds;
}
