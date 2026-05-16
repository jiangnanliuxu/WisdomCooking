package com.ruoyi.cook.ai.convert;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.cook.ai.domain.AiImageRecognitionLog;
import com.ruoyi.cook.ai.domain.AiMessage;
import com.ruoyi.cook.ai.domain.AiModel;
import com.ruoyi.cook.ai.dto.AiModelRequest;
import com.ruoyi.cook.ai.dto.AiRecognizeFoodRequest;
import com.ruoyi.cook.ai.vo.AiChatResponseVo;
import com.ruoyi.cook.ai.vo.AiConversationDetailVo;
import com.ruoyi.cook.ai.vo.AiMessageVo;
import com.ruoyi.cook.ai.vo.AiModelTestVo;
import com.ruoyi.cook.ai.vo.AiModelVo;
import com.ruoyi.cook.ai.vo.AiRecognitionVo;

/**
 * AI 模块对象转换器。
 * <p>
 * 通过 MapStruct 在编译期生成实体、DTO、VO 的映射代码，业务层只保留流程控制、
 * 事务边界和模型调用逻辑，避免大量手写 set/get 造成代码膨胀。
 * </p>
 */
@Mapper(componentModel = "spring")
public interface AiConvert
{
    String RECOGNITION_DISCLAIMER = "识别结果仅供参考，实际热量可能因食材、做法和份量而异。";

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lastTestStatus", ignore = true)
    @Mapping(target = "lastTestLatencyMs", ignore = true)
    @Mapping(target = "lastTestMessage", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    AiModel toModel(AiModelRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "modelType", source = "modelType")
    @Mapping(target = "configJson", source = "configJson")
    @Mapping(target = "isDefault", source = "isDefault")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "lastTestStatus", ignore = true)
    @Mapping(target = "lastTestLatencyMs", ignore = true)
    @Mapping(target = "lastTestMessage", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    AiModel toModel(AiModelRequest request, String modelType, String configJson, Boolean isDefault, String status);

    AiModel copyModel(AiModel model);

    @Mapping(target = "name", source = "name")
    @Mapping(target = "provider", source = "provider")
    @Mapping(target = "modelCode", source = "modelCode")
    @Mapping(target = "apiBaseUrl", source = "apiBaseUrl")
    AiModel toRuntimeModel(AiModel source, String name, String provider, String modelCode, String apiBaseUrl);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "modelId", source = "model.id")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "userNickname", ignore = true)
    @Mapping(target = "userAvatarUrl", ignore = true)
    @Mapping(target = "modelName", ignore = true)
    @Mapping(target = "modelType", ignore = true)
    @Mapping(target = "ragHit", ignore = true)
    @Mapping(target = "ragSourcesJson", ignore = true)
    @Mapping(target = "fallbackReason", ignore = true)
    AiMessage toMessage(Long conversationId, Long userId, AiModel model, String role, String content,
            Integer inputTokens, Integer outputTokens, Integer responseTimeMs, String flag, String flagReason);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "modelId", source = "model.id")
    @Mapping(target = "imageMediaId", source = "request.imageMediaId")
    @Mapping(target = "imageUrl", source = "request.imageUrl")
    @Mapping(target = "recognizedName", expression = "java(stringValue(result, \"recognizedName\"))")
    @Mapping(target = "confidence", expression = "java(bigDecimalValue(result, \"confidence\"))")
    @Mapping(target = "calories", expression = "java(integerValue(result, \"calories\"))")
    @Mapping(target = "suggestion", expression = "java(stringValue(result, \"suggestion\"))")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "userNickname", ignore = true)
    @Mapping(target = "userAvatarUrl", ignore = true)
    @Mapping(target = "modelName", ignore = true)
    AiImageRecognitionLog toRecognitionLog(Long userId, AiModel model, AiRecognizeFoodRequest request,
            Map<String, Object> result, String nutritionJson, String candidatesJson, String resultJson,
            Integer responseTimeMs, String status);

    @Mapping(target = "userMessageId", source = "userMessage.id")
    @Mapping(target = "assistantMessageId", source = "assistantMessage.id")
    @Mapping(target = "modelId", source = "model.id")
    @Mapping(target = "modelName", source = "model.name")
    @Mapping(target = "conversationId", source = "conversationId")
    @Mapping(target = "answer", source = "answer")
    @Mapping(target = "inputTokens", source = "inputTokens")
    @Mapping(target = "outputTokens", source = "outputTokens")
    @Mapping(target = "latencyMs", source = "latencyMs")
    @Mapping(target = "flag", source = "flag")
    @Mapping(target = "disclaimer", source = "disclaimer")
    @Mapping(target = "ragHit", ignore = true)
    @Mapping(target = "sources", ignore = true)
    @Mapping(target = "fallbackReason", ignore = true)
    AiChatResponseVo toChatResponse(Long conversationId, AiMessage userMessage, AiMessage assistantMessage,
            AiModel model, String answer, Integer inputTokens, Integer outputTokens, Integer latencyMs,
            String flag, String disclaimer);

    AiModelTestVo toModelTestVo(Long modelId, String status, Integer latencyMs, String message);

    AiModelVo toModelVo(AiModel model);

    List<AiModelVo> toModelVoList(List<AiModel> models);

    AiMessageVo toMessageVo(AiMessage message);

    List<AiMessageVo> toMessageVoList(List<AiMessage> messages);

    @Mapping(target = "latencyMs", source = "responseTimeMs")
    @Mapping(target = "nutrition", expression = "java(parseNutrition(log.getNutritionJson()))")
    @Mapping(target = "candidates", expression = "java(parseCandidates(log.getCandidatesJson()))")
    @Mapping(target = "disclaimer", expression = "java(RECOGNITION_DISCLAIMER)")
    AiRecognitionVo toRecognitionVo(AiImageRecognitionLog log);

    List<AiRecognitionVo> toRecognitionVoList(List<AiImageRecognitionLog> logs);

    AiConversationDetailVo toConversationDetail(com.ruoyi.cook.ai.vo.AiConversationVo summary, List<AiMessageVo> messages);

    @SuppressWarnings("unchecked")
    default Map<String, Object> parseNutrition(String nutritionJson)
    {
        if (StringUtils.isBlank(nutritionJson))
        {
            return Collections.emptyMap();
        }
        Map<String, Object> nutrition = JSON.parseObject(nutritionJson, Map.class);
        return nutrition == null ? Collections.emptyMap() : nutrition;
    }

    default Object parseCandidates(String candidatesJson)
    {
        if (StringUtils.isBlank(candidatesJson))
        {
            return new JSONArray();
        }
        return JSON.parse(candidatesJson);
    }

    default String stringValue(Map<String, Object> values, String key)
    {
        Object value = values == null ? null : values.get(key);
        return value == null ? null : String.valueOf(value);
    }

    default Integer integerValue(Map<String, Object> values, String key)
    {
        Object value = values == null ? null : values.get(key);
        if (value instanceof Number number)
        {
            return number.intValue();
        }
        return value == null ? null : Integer.valueOf(String.valueOf(value));
    }

    default BigDecimal bigDecimalValue(Map<String, Object> values, String key)
    {
        Object value = values == null ? null : values.get(key);
        if (value instanceof BigDecimal decimal)
        {
            return decimal;
        }
        if (value instanceof Number number)
        {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return value == null ? null : new BigDecimal(String.valueOf(value));
    }
}
