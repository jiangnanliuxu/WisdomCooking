package com.ruoyi.cook.ai.domain;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * AI 知识库索引任务，用于记录启动扫描、手动扫描和重建索引结果。
 */
@Data
public class AiKnowledgeIndexJob
{
    private Long id;
    private Long documentId;
    private String jobType;
    private String status;
    private Integer totalDocuments;
    private Integer indexedDocuments;
    private Integer failedDocuments;
    private String message;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
