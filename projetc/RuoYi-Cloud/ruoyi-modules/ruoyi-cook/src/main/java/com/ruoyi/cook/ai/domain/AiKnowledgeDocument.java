package com.ruoyi.cook.ai.domain;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * AI 知识库文档元数据，对应本地 embeddingDocs 下的原始文件。
 */
@Data
public class AiKnowledgeDocument
{
    private Long id;
    private String fileName;
    private String originalName;
    private String relativePath;
    private String filePath;
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
    private LocalDateTime deletedAt;
}
