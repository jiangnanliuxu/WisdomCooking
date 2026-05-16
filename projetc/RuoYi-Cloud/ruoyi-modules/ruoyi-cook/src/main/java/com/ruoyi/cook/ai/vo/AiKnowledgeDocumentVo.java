package com.ruoyi.cook.ai.vo;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 知识库文档展示对象。
 */
@Data
@Schema(description = "AI知识库文档")
public class AiKnowledgeDocumentVo
{
    private Long id;
    private String fileName;
    private String originalName;
    private String relativePath;
    private String fileHash;
    private Long fileSize;
    private String fileType;
    private String title;
    private String status;
    private Integer chunkCount;
    private LocalDateTime lastIndexedAt;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
