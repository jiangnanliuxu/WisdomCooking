package com.ruoyi.cook.ai.domain;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * AI 知识库文档分片，向量正文存入 Elasticsearch，本表保存映射关系。
 */
@Data
public class AiKnowledgeChunk
{
    private Long id;
    private Long documentId;
    private Integer chunkIndex;
    private String vectorId;
    private String content;
    private Integer tokenCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
