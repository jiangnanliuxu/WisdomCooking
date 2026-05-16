package com.ruoyi.cook.ai.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cook.ai.domain.AiKnowledgeIndexJob;

/**
 * AI 知识库索引任务 Mapper。
 */
public interface AiKnowledgeIndexJobMapper
{
    AiKnowledgeIndexJob selectById(Long id);

    List<AiKnowledgeIndexJob> selectList(@Param("documentId") Long documentId);

    int insertJob(AiKnowledgeIndexJob job);

    int updateProgress(AiKnowledgeIndexJob job);

    int finishJob(AiKnowledgeIndexJob job);
}
