package com.ruoyi.cook.message.domain.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 会话设置请求。
 */
@Data
@Schema(description = "会话设置请求")
public class ConversationSettingsRequest
{
    @Schema(description = "是否免打扰")
    private Boolean muted;

    @Schema(description = "是否置顶")
    private Boolean pinned;
}
