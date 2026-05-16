package com.ruoyi.cook.ai.service;

import com.ruoyi.cook.ai.dto.AiKnowledgeTextRequest;
import com.ruoyi.cook.ai.vo.AiKnowledgeDocumentVo;
import com.ruoyi.cook.ai.vo.AiKnowledgeIndexJobVo;
import com.ruoyi.cook.ai.vo.AiRagRetrieveResult;
import com.ruoyi.cook.common.domain.vo.PageVo;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI 知识库服务接口。
 */
public interface IAiKnowledgeService
{
    PageVo<AiKnowledgeDocumentVo> listDocuments(String status, String keyword, Integer page, Integer pageSize);

    AiKnowledgeDocumentVo createTextDocument(AiKnowledgeTextRequest request);

    AiKnowledgeDocumentVo uploadDocument(MultipartFile file);

    AiKnowledgeIndexJobVo scanDocuments();

    AiKnowledgeIndexJobVo reindexDocument(Long id);

    AiKnowledgeDocumentVo offlineDocument(Long id);

    PageVo<AiKnowledgeIndexJobVo> listJobs(Long documentId, Integer page, Integer pageSize);

    void scanDocumentsOnStartup();

    AiRagRetrieveResult retrieve(String question);
}
