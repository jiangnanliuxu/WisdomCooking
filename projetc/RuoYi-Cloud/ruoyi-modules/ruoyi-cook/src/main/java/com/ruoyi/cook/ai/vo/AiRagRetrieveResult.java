package com.ruoyi.cook.ai.vo;

import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * RAG 检索结果，供 AI 对话服务组装提示词和日志。
 */
@Data
@AllArgsConstructor
public class AiRagRetrieveResult
{
    private boolean hit;
    private String context;
    private List<AiRagSourceVo> sources;
    private String fallbackReason;

    public static AiRagRetrieveResult miss(String fallbackReason)
    {
        return new AiRagRetrieveResult(false, "", Collections.emptyList(), fallbackReason);
    }
}
