package com.ruoyi.cook.operation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端处理举报请求。
 */
@Data
public class ReportHandleRequest
{
    @NotBlank(message = "处理状态不能为空")
    private String status;

    private String handleResult;
}
