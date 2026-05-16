package com.ruoyi.cook.recipe.domain.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 菜谱审核驳回请求。
 */
@Data
@Schema(description = "菜谱审核驳回请求")
public class RecipeAuditRejectRequest
{
    @NotBlank(message = "驳回原因不能为空")
    @Size(max = 1000, message = "驳回原因不能超过1000个字符")
    @Schema(description = "驳回原因，用户可见", example = "食材清单信息不完整，请补充后重新提交", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

}
