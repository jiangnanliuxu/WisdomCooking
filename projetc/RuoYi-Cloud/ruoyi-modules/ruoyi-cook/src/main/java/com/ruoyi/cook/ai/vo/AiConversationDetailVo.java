package com.ruoyi.cook.ai.vo;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 会话详情，包含摘要和完整消息列表。
 */
@Data
@Schema(description = "AI会话详情")
public class AiConversationDetailVo
{
    private AiConversationVo summary;
    private List<AiMessageVo> messages;
}
