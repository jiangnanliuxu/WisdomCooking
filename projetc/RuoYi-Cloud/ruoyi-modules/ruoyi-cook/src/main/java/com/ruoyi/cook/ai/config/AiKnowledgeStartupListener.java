package com.ruoyi.cook.ai.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.ruoyi.cook.ai.service.IAiKnowledgeService;

/**
 * 服务启动后异步扫描本地 embeddingDocs 目录，自动向量化手动放入的文档。
 */
@Component
public class AiKnowledgeStartupListener
{
    @Autowired
    private IAiKnowledgeService knowledgeService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady()
    {
        knowledgeService.scanDocumentsOnStartup();
    }
}
