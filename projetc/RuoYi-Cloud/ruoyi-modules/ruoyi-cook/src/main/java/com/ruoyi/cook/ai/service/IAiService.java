package com.ruoyi.cook.ai.service;

import java.util.List;
import com.ruoyi.cook.ai.dto.AiChatRequest;
import com.ruoyi.cook.ai.dto.AiFlagRequest;
import com.ruoyi.cook.ai.dto.AiModelRequest;
import com.ruoyi.cook.ai.dto.AiPromptRequest;
import com.ruoyi.cook.ai.dto.AiRecognizeFoodRequest;
import com.ruoyi.cook.ai.vo.AiChatResponseVo;
import com.ruoyi.cook.ai.vo.AiConversationDetailVo;
import com.ruoyi.cook.ai.vo.AiConversationVo;
import com.ruoyi.cook.ai.vo.AiModelTestVo;
import com.ruoyi.cook.ai.vo.AiModelVo;
import com.ruoyi.cook.ai.vo.AiRecognitionVo;
import com.ruoyi.cook.common.domain.vo.PageVo;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 能力服务接口。
 */
public interface IAiService
{
    AiChatResponseVo chat(AiChatRequest request);

    SseEmitter chatStream(AiChatRequest request);

    List<String> listRecommendedQuestions();

    PageVo<AiConversationVo> listMyConversations(Integer page, Integer pageSize);

    AiConversationDetailVo getMyConversation(Long id);

    void deleteMyConversation(Long id);

    AiRecognitionVo recognizeFood(AiRecognizeFoodRequest request);

    PageVo<AiRecognitionVo> listMyRecognitions(String status, Integer page, Integer pageSize);

    void deleteMyRecognition(Long id);

    PageVo<AiModelVo> listModels(String modelType, String status, String keyword, Integer page, Integer pageSize);

    AiModelVo createModel(AiModelRequest request);

    AiModelVo updateModel(Long id, AiModelRequest request);

    AiModelVo enableModel(Long id);

    AiModelTestVo testModel(Long id);

    AiModelVo savePrompt(Long id, AiPromptRequest request);

    PageVo<AiConversationVo> listConversationLogs(String keyword, String flag, String modelType, Integer page, Integer pageSize);

    AiConversationDetailVo getConversationLog(Long id);

    AiConversationDetailVo markConversation(Long id, AiFlagRequest request);

    PageVo<AiRecognitionVo> listRecognitionLogs(String keyword, String status, Long userId, Integer page, Integer pageSize);

    AiRecognitionVo getRecognitionLog(Long id);

    void deleteRecognitionLog(Long id);
}
