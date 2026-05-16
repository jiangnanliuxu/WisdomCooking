package com.ruoyi.cook.message.domain.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 创建或获取私信会话请求。
 */
@Data
@Schema(description = "创建或获取私信会话请求")
public class PrivateConversationRequest
{
    @NotNull(message = "私信对象不能为空")
    @Schema(description = "对方用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long targetUserId;
}
