package com.ruoyi.cook.ai.mapper;

import java.util.List;
import com.ruoyi.cook.ai.domain.AiKnowledgeChunk;

/**
 * AI 知识库分片 Mapper。
 */
public interface AiKnowledgeChunkMapper
{
    List<AiKnowledgeChunk> selectByDocumentId(Long documentId);

    int countByDocumentId(Long documentId);

    int insertChunk(AiKnowledgeChunk chunk);

    int deleteByDocumentId(Long documentId);
}
