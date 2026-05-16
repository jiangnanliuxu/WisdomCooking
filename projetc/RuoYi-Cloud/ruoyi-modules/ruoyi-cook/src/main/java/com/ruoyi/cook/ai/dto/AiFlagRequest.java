package com.ruoyi.cook.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端 AI 内容标记请求。
 */
@Data
@Schema(description = "AI内容标记请求")
public class AiFlagRequest
{
    @NotBlank(message = "标记不能为空")
    @Size(max = 30, message = "标记不能超过30个字符")
    @Schema(description = "标记：normal/warning/violation", example = "warning")
    private String flag;

    @Size(max = 500, message = "标记原因不能超过500个字符")
    @Schema(description = "标记原因")
    private String reason;
}
