package com.ruoyi.cook.ai.vo;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 知识库索引任务展示对象。
 */
@Data
@Schema(description = "AI知识库索引任务")
public class AiKnowledgeIndexJobVo
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
