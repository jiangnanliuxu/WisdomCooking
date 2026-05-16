package com.ruoyi.cook.ai.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cook.ai.domain.AiKnowledgeDocument;

/**
 * AI 知识库文档 Mapper。
 */
public interface AiKnowledgeDocumentMapper
{
    AiKnowledgeDocument selectById(Long id);

    AiKnowledgeDocument selectByRelativePath(String relativePath);

    List<AiKnowledgeDocument> selectList(@Param("status") String status, @Param("keyword") String keyword);

    int insertDocument(AiKnowledgeDocument document);

    int updateFileSnapshot(AiKnowledgeDocument document);

    int updateIndexing(@Param("id") Long id);

    int updateIndexed(@Param("id") Long id, @Param("chunkCount") Integer chunkCount);

    int updateFailed(@Param("id") Long id, @Param("errorMessage") String errorMessage);

    int updateOffline(@Param("id") Long id);
}
