package com.ruoyi.cook.ai.controller;

import java.util.List;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.security.annotation.RequiresLogin;
import com.ruoyi.cook.ai.dto.AiChatRequest;
import com.ruoyi.cook.ai.dto.AiRecognizeFoodRequest;
import com.ruoyi.cook.ai.service.IAiService;
import com.ruoyi.cook.ai.vo.AiChatResponseVo;
import com.ruoyi.cook.ai.vo.AiConversationDetailVo;
import com.ruoyi.cook.ai.vo.AiConversationVo;
import com.ruoyi.cook.ai.vo.AiRecognitionVo;
import com.ruoyi.cook.common.domain.vo.PageVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 用户端 AI 营养师接口。
 * <p>
 * 第五阶段包含 AI 问答、会话历史、食物识图和识图历史；
 * 对话能力调用真实大模型，并完整写入问答日志。
 * </p>
 */
@Tag(name = "用户端-AI营养师", description = "AI饮食问答、AI识图、对话历史和识图历史")
@RequiresLogin
@RestController
@RequestMapping("/api/v1/ai")
public class CookAiController
{
    @Autowired
    private IAiService aiService;

    @Operation(summary = "AI饮食问答", description = "读取默认对话模型，返回AI营养建议，并写入AI对话日志")
    @PostMapping("/chat")
    public R<AiChatResponseVo> chat(@Valid @RequestBody AiChatRequest request)
    {
        return R.ok(aiService.chat(request));
    }

    @Operation(summary = "AI饮食问答流式输出", description = "读取默认对话模型，按SSE实时返回AI营养建议，并写入AI对话日志")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> chatStream(@Valid @RequestBody AiChatRequest request)
    {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform")
                .header("X-Accel-Buffering", "no")
                .body(aiService.chatStream(request));
    }

    @Operation(summary = "AI推荐提问", description = "从全站AI对话日志中统计用户提问最多的前三个问题")
    @GetMapping("/recommended-questions")
    public R<List<String>> listRecommendedQuestions()
    {
        return R.ok(aiService.listRecommendedQuestions());
    }

    @Operation(summary = "我的AI对话记录", description = "按conversationId聚合展示当前用户的AI对话")
    @GetMapping("/conversations")
    public R<PageVo<AiConversationVo>> listConversations(
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(aiService.listMyConversations(page, pageSize));
    }

    @Operation(summary = "AI对话详情", description = "查看当前用户某个AI会话的完整消息")
    @GetMapping("/conversations/{id}")
    public R<AiConversationDetailVo> getConversation(@Parameter(description = "AI会话ID") @PathVariable Long id)
    {
        return R.ok(aiService.getMyConversation(id));
    }

    @Operation(summary = "删除AI对话", description = "用户删除自己的AI对话，采用软删除")
    @DeleteMapping("/conversations/{id}")
    public R<?> deleteConversation(@Parameter(description = "AI会话ID") @PathVariable Long id)
    {
        aiService.deleteMyConversation(id);
        return R.ok();
    }

    @Operation(summary = "AI食物识图", description = "读取默认视觉模型，返回热量、营养结构、建议和免责声明，并写入识图日志")
    @PostMapping("/recognize-food")
    public R<AiRecognitionVo> recognizeFood(@Valid @RequestBody AiRecognizeFoodRequest request)
    {
        return R.ok(aiService.recognizeFood(request));
    }

    @Operation(summary = "我的识图历史", description = "分页查询当前用户的AI食物识图记录")
    @GetMapping("/recognitions")
    public R<PageVo<AiRecognitionVo>> listRecognitions(
            @Parameter(description = "识图状态：success/failed/unknown") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(aiService.listMyRecognitions(status, page, pageSize));
    }

    @Operation(summary = "删除识图历史", description = "用户删除自己的识图记录，采用软删除")
    @DeleteMapping("/recognitions/{id}")
    public R<?> deleteRecognition(@Parameter(description = "识图记录ID") @PathVariable Long id)
    {
        aiService.deleteMyRecognition(id);
        return R.ok();
    }
}
