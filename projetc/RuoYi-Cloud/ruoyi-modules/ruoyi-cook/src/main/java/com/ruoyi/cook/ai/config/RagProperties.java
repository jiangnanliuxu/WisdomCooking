package com.ruoyi.cook.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

/**
 * 本地知识库与 RAG 检索配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "cook.ai.rag")
public class RagProperties
{
    private boolean enabled = true;
    private boolean ingestOnStartup = true;
    private String docsDir = "ruoyi-modules/ruoyi-cook/storage/embeddingDocs";
    private int topK = 5;
    private double similarityThreshold = 0.55D;
    private int chunkSize = 800;
    private int chunkOverlap = 120;
    private boolean allowGeneralFallback = true;
}
