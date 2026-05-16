package com.ruoyi.cook.ai.controller;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.cook.ai.dto.AiKnowledgeTextRequest;
import com.ruoyi.cook.ai.service.IAiKnowledgeService;
import com.ruoyi.cook.ai.vo.AiKnowledgeDocumentVo;
import com.ruoyi.cook.ai.vo.AiKnowledgeIndexJobVo;
import com.ruoyi.cook.common.domain.vo.PageVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理端 AI 知识库接口。
 */
@Tag(name = "管理端-AI知识库", description = "本地知识库文档、启动扫描和向量索引任务")
@RestController
@RequestMapping("/api/admin/v1/ai/knowledge")
public class CookAiKnowledgeAdminController
{
    @Autowired
    private IAiKnowledgeService knowledgeService;

    @Operation(summary = "知识库文档列表", description = "查看本地embeddingDocs目录已纳入管理的文档和索引状态")
    @RequiresPermissions("cook:ai:knowledge:list")
    @GetMapping("/documents")
    public R<PageVo<AiKnowledgeDocumentVo>> listDocuments(
            @Parameter(description = "状态：pending/indexing/indexed/failed/offline") @RequestParam(required = false) String status,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(knowledgeService.listDocuments(status, keyword, page, pageSize));
    }

    @Operation(summary = "录入知识库文本", description = "将后台录入的文本保存为本地.md/.txt文件并立即向量化")
    @RequiresPermissions("cook:ai:knowledge:add")
    @PostMapping("/documents/text")
    public R<AiKnowledgeDocumentVo> createTextDocument(@Valid @RequestBody AiKnowledgeTextRequest request)
    {
        return R.ok(knowledgeService.createTextDocument(request));
    }

    @Operation(summary = "上传知识库文件", description = "上传.md或.txt文件到本地embeddingDocs目录并立即向量化")
    @RequiresPermissions("cook:ai:knowledge:add")
    @PostMapping("/documents/upload")
    public R<AiKnowledgeDocumentVo> uploadDocument(@RequestPart("file") MultipartFile file)
    {
        return R.ok(knowledgeService.uploadDocument(file));
    }

    @Operation(summary = "扫描本地知识库目录", description = "异步扫描embeddingDocs目录，新增或变更文件会自动向量化")
    @RequiresPermissions("cook:ai:knowledge:index")
    @PostMapping("/documents/scan")
    public R<AiKnowledgeIndexJobVo> scanDocuments()
    {
        return R.ok(knowledgeService.scanDocuments());
    }

    @Operation(summary = "重建文档索引", description = "删除该文档旧向量并重新切分、向量化")
    @RequiresPermissions("cook:ai:knowledge:index")
    @PostMapping("/documents/{id}/reindex")
    public R<AiKnowledgeIndexJobVo> reindexDocument(@Parameter(description = "文档ID") @PathVariable Long id)
    {
        return R.ok(knowledgeService.reindexDocument(id));
    }

    @Operation(summary = "下线知识库文档", description = "下线文档并删除对应向量索引，原始文件仍保留在本地")
    @RequiresPermissions("cook:ai:knowledge:edit")
    @PutMapping("/documents/{id}/offline")
    public R<AiKnowledgeDocumentVo> offlineDocument(@Parameter(description = "文档ID") @PathVariable Long id)
    {
        return R.ok(knowledgeService.offlineDocument(id));
    }

    @Operation(summary = "知识库索引任务列表", description = "查看启动扫描、手动扫描和重建索引任务")
    @RequiresPermissions("cook:ai:knowledge:list")
    @GetMapping("/jobs")
    public R<PageVo<AiKnowledgeIndexJobVo>> listJobs(
            @Parameter(description = "文档ID") @RequestParam(required = false) Long documentId,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(knowledgeService.listJobs(documentId, page, pageSize));
    }
}
