package com.ruoyi.cook.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI RAG 回答来源片段。
 */
@Data
@Schema(description = "AI RAG回答来源")
public class AiRagSourceVo
{
    private Long documentId;
    private Long chunkId;
    private String fileName;
    private String title;
    private Double score;
    private String snippet;
}
