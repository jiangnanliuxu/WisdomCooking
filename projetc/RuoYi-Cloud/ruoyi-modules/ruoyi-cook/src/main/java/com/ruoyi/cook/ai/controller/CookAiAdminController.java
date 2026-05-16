package com.ruoyi.cook.ai.controller;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.cook.ai.dto.AiFlagRequest;
import com.ruoyi.cook.ai.dto.AiModelRequest;
import com.ruoyi.cook.ai.dto.AiPromptRequest;
import com.ruoyi.cook.ai.service.IAiService;
import com.ruoyi.cook.ai.vo.AiConversationDetailVo;
import com.ruoyi.cook.ai.vo.AiConversationVo;
import com.ruoyi.cook.ai.vo.AiModelTestVo;
import com.ruoyi.cook.ai.vo.AiModelVo;
import com.ruoyi.cook.ai.vo.AiRecognitionVo;
import com.ruoyi.cook.common.domain.vo.PageVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 AI 运维接口。
 * <p>
 * 管理员可维护模型配置、启用默认模型、测试连通性，并查看/标记 AI 对话和识图日志。
 * </p>
 */
@Tag(name = "管理端-AI运维", description = "AI模型配置、对话日志、识图日志和内容标记")
@RestController
@RequestMapping("/api/admin/v1/ai")
public class CookAiAdminController
{
    @Autowired
    private IAiService aiService;

    @Operation(summary = "AI模型列表", description = "支持按模型类型、状态和关键词筛选")
    @RequiresPermissions("cook:ai:model:list")
    @GetMapping("/models")
    public R<PageVo<AiModelVo>> listModels(
            @Parameter(description = "模型类型：chat/vision") @RequestParam(required = false) String modelType,
            @Parameter(description = "状态：enabled/disabled") @RequestParam(required = false) String status,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(aiService.listModels(modelType, status, keyword, page, pageSize));
    }

    @Operation(summary = "添加AI模型", description = "新增对话模型或视觉模型，可直接设为该类型默认模型")
    @RequiresPermissions("cook:ai:model:add")
    @PostMapping("/models")
    public R<AiModelVo> createModel(@Valid @RequestBody AiModelRequest request)
    {
        return R.ok(aiService.createModel(request));
    }

    @Operation(summary = "编辑AI模型", description = "更新模型供应商、模型标识、参数、Prompt和状态；API Key传空时保留原值")
    @RequiresPermissions("cook:ai:model:edit")
    @PutMapping("/models/{id}")
    public R<AiModelVo> updateModel(@Parameter(description = "模型ID") @PathVariable Long id,
            @Valid @RequestBody AiModelRequest request)
    {
        return R.ok(aiService.updateModel(id, request));
    }

    @Operation(summary = "启用默认模型", description = "启用某个chat模型为默认对话模型，后续AI问答会调用该模型；不会影响vision识图模型")
    @RequiresPermissions("cook:ai:model:edit")
    @PostMapping("/models/{id}/enable")
    public R<AiModelVo> enableModel(@Parameter(description = "模型ID") @PathVariable Long id)
    {
        return R.ok(aiService.enableModel(id));
    }

    @Operation(summary = "模型连通测试", description = "记录测试状态、耗时和摘要；chat模型按供应商OpenAI兼容接口真实测试")
    @RequiresPermissions("cook:ai:model:test")
    @PostMapping("/models/{id}/test")
    public R<AiModelTestVo> testModel(@Parameter(description = "模型ID") @PathVariable Long id)
    {
        return R.ok(aiService.testModel(id));
    }

    @Operation(summary = "保存模型Prompt", description = "保存系统提示词和Few-shot预设问答")
    @RequiresPermissions("cook:ai:model:edit")
    @PutMapping("/models/{id}/prompt")
    public R<AiModelVo> savePrompt(@Parameter(description = "模型ID") @PathVariable Long id,
            @Valid @RequestBody AiPromptRequest request)
    {
        return R.ok(aiService.savePrompt(id, request));
    }

    @Operation(summary = "AI对话日志", description = "按会话聚合查看AI问答日志，支持关键词、标记和模型类型筛选")
    @RequiresPermissions("cook:ai:log:list")
    @GetMapping("/conversation-logs")
    public R<PageVo<AiConversationVo>> listConversationLogs(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "标记：normal/warning/violation") @RequestParam(required = false) String flag,
            @Parameter(description = "模型类型：chat/vision") @RequestParam(required = false) String modelType,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(aiService.listConversationLogs(keyword, flag, modelType, page, pageSize));
    }

    @Operation(summary = "AI对话日志详情", description = "查看某个AI会话的完整用户提问和AI回复")
    @RequiresPermissions("cook:ai:log:query")
    @GetMapping("/conversation-logs/{id}")
    public R<AiConversationDetailVo> getConversationLog(@Parameter(description = "AI会话ID") @PathVariable Long id)
    {
        return R.ok(aiService.getConversationLog(id));
    }

    @Operation(summary = "标记AI对话", description = "将AI会话标记为正常、警告或违规")
    @RequiresPermissions("cook:ai:log:mark")
    @PostMapping("/conversation-logs/{id}/mark")
    public R<AiConversationDetailVo> markConversation(@Parameter(description = "AI会话ID") @PathVariable Long id,
            @Valid @RequestBody AiFlagRequest request)
    {
        return R.ok(aiService.markConversation(id, request));
    }

    @Operation(summary = "AI识图日志", description = "查看所有用户的识图日志，支持状态、用户和菜品关键词筛选")
    @RequiresPermissions("cook:ai:recognition:list")
    @GetMapping("/recognition-logs")
    public R<PageVo<AiRecognitionVo>> listRecognitionLogs(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "识图状态：success/failed/unknown") @RequestParam(required = false) String status,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(aiService.listRecognitionLogs(keyword, status, userId, page, pageSize));
    }

    @Operation(summary = "AI识图日志详情", description = "查看识图图片、识别结果、营养结构、候选结果和免责声明")
    @RequiresPermissions("cook:ai:recognition:query")
    @GetMapping("/recognition-logs/{id}")
    public R<AiRecognitionVo> getRecognitionLog(@Parameter(description = "识图日志ID") @PathVariable Long id)
    {
        return R.ok(aiService.getRecognitionLog(id));
    }

    @Operation(summary = "删除AI识图日志", description = "软删除识图日志，用户端历史同步不可见")
    @RequiresPermissions("cook:ai:recognition:remove")
    @DeleteMapping("/recognition-logs/{id}")
    public R<?> deleteRecognitionLog(@Parameter(description = "识图日志ID") @PathVariable Long id)
    {
        aiService.deleteRecognitionLog(id);
        return R.ok();
    }
}
