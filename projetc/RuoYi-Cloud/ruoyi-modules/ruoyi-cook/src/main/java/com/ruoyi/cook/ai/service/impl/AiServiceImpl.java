package com.ruoyi.cook.ai.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.cook.ai.config.RagProperties;
import com.ruoyi.cook.ai.config.QwenAiProperties;
import com.ruoyi.cook.ai.convert.AiConvert;
import com.ruoyi.cook.ai.domain.AiImageRecognitionLog;
import com.ruoyi.cook.ai.domain.AiMessage;
import com.ruoyi.cook.ai.domain.AiModel;
import com.ruoyi.cook.ai.dto.AiChatRequest;
import com.ruoyi.cook.ai.dto.AiFlagRequest;
import com.ruoyi.cook.ai.dto.AiModelRequest;
import com.ruoyi.cook.ai.dto.AiPromptRequest;
import com.ruoyi.cook.ai.dto.AiRecognizeFoodRequest;
import com.ruoyi.cook.ai.mapper.AiMessageMapper;
import com.ruoyi.cook.ai.mapper.AiModelMapper;
import com.ruoyi.cook.ai.mapper.AiRecognitionMapper;
import com.ruoyi.cook.ai.service.IAiKnowledgeService;
import com.ruoyi.cook.ai.service.IAiService;
import com.ruoyi.cook.ai.vo.AiChatResponseVo;
import com.ruoyi.cook.ai.vo.AiConversationDetailVo;
import com.ruoyi.cook.ai.vo.AiConversationVo;
import com.ruoyi.cook.ai.vo.AiMessageVo;
import com.ruoyi.cook.ai.vo.AiModelTestVo;
import com.ruoyi.cook.ai.vo.AiModelVo;
import com.ruoyi.cook.ai.vo.AiRagRetrieveResult;
import com.ruoyi.cook.ai.vo.AiRecognitionVo;
import com.ruoyi.cook.common.domain.vo.PageVo;
import jakarta.annotation.PreDestroy;

/**
 * AI 能力服务实现。
 * <p>
 * 当前服务已接入阿里云通义千问真实模型，负责模型配置、默认模型隔离、调用日志、
 * 识图结构化结果和管理端标记等后端业务。
 * </p>
 */
@Service
public class AiServiceImpl implements IAiService
{
    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String MODEL_CHAT = "chat";
    private static final String MODEL_VISION = "vision";
    private static final String STATUS_ENABLED = "enabled";
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String FLAG_NORMAL = "normal";
    private static final String FLAG_WARNING = "warning";
    private static final String FLAG_VIOLATION = "violation";
    private static final String RECOGNITION_SUCCESS = "success";
    private static final String PROVIDER_QWEN = "aliyun-qwen";
    private static final String PROVIDER_DEEPSEEK = "deepseek";
    private static final String PROVIDER_ZHIPU = "zhipu";
    private static final String PROVIDER_BAIDU_QIANFAN = "baidu-qianfan";
    private static final String PROVIDER_OPENAI_COMPATIBLE = "openai-compatible";
    private static final String OPENAI_CHAT_COMPLETIONS_PATH = "/chat/completions";
    private static final String BASE_URL_QWEN = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private static final String BASE_URL_DEEPSEEK = "https://api.deepseek.com";
    private static final String BASE_URL_ZHIPU = "https://open.bigmodel.cn/api/paas/v4";
    private static final String BASE_URL_BAIDU_QIANFAN = "https://qianfan.baidubce.com/v2";
    private static final String DISCLAIMER_CHAT = "AI饮食建议仅供参考，不能替代医生或注册营养师的专业意见。";
    private static final String DISCLAIMER_RECOGNITION = "识别结果仅供参考，实际热量可能因食材、做法和份量而异。";
    private static final int STREAM_THREAD_COUNT = 4;
    private static final long STREAM_CHAR_DELAY_MS = 25L;
    private static final long STREAM_PUNCTUATION_DELAY_MS = 90L;
    private static final long STREAM_LINE_BREAK_DELAY_MS = 120L;
    private static final int RECOMMENDED_QUESTION_LIMIT = 3;
    private static final List<String> DEFAULT_RECOMMENDED_QUESTIONS = List.of("减脂食谱推荐", "增肌怎么吃", "糖尿病饮食");

    private final ExecutorService chatStreamExecutor = Executors.newFixedThreadPool(STREAM_THREAD_COUNT);

    @Autowired
    private AiModelMapper modelMapper;

    @Autowired
    private AiMessageMapper messageMapper;

    @Autowired
    private AiRecognitionMapper recognitionMapper;

    @Autowired
    private QwenAiProperties qwenProperties;

    @Autowired
    private RagProperties ragProperties;

    @Autowired
    private AiConvert aiConvert;

    @Autowired
    private IAiKnowledgeService knowledgeService;

    @Autowired(required = false)
    private ChatModel springAiChatModel;

    @PreDestroy
    public void shutdownChatStreamExecutor()
    {
        chatStreamExecutor.shutdown();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatResponseVo chat(AiChatRequest request)
    {
        // 1. 解析当前登录用户、默认对话模型和会话编号，保证一轮问答能完整归档到同一个 conversationId。
        Long userId = requireUserId();
        AiModel model = resolveRuntimeModel(requireDefaultModel(MODEL_CHAT), MODEL_CHAT);
        Long conversationId = resolveConversationId(request.getConversationId(), userId);
        String flag = detectContentFlag(request.getQuestion());
        String flagReason = FLAG_NORMAL.equals(flag) ? null : "命中高风险饮食或健康建议关键词";
        int inputTokens = estimateTokens(request.getQuestion());
        LocalDateTime startedAt = LocalDateTime.now();

        // 2. 先写入用户消息，后续即使模型返回异常，也能保留用户发起过的业务上下文。
        AiMessage userMessage = aiConvert.toMessage(conversationId, userId, model, ROLE_USER, request.getQuestion(),
                inputTokens, 0, 0, flag, flagReason);
        messageMapper.insertMessage(userMessage);

        // 3. 调用真实模型并优先使用供应商返回的 Token 统计，便于后续做调用成本核算。
        AiProviderResult aiResult = generateChatAnswer(model, request.getQuestion(), flag);
        String answer = aiResult.content();
        int outputTokens = aiResult.outputTokens() == null ? estimateTokens(answer) : aiResult.outputTokens();
        inputTokens = aiResult.inputTokens() == null ? inputTokens : aiResult.inputTokens();
        int latencyMs = latencySince(startedAt);

        // 4. 写入助手回复并返回前端响应对象；实体到 VO 的组装交给 MapStruct 生成代码完成。
        AiMessage assistantMessage = aiConvert.toMessage(conversationId, userId, model, ROLE_ASSISTANT, answer,
                inputTokens, outputTokens, latencyMs, flag, flagReason);
        applyRagMetadata(assistantMessage, aiResult.ragResult());
        messageMapper.insertMessage(assistantMessage);
        AiChatResponseVo response = aiConvert.toChatResponse(conversationId, userMessage, assistantMessage, model, answer,
                inputTokens, outputTokens, latencyMs, flag, DISCLAIMER_CHAT);
        applyRagMetadata(response, aiResult.ragResult());
        return response;
    }

    @Override
    public SseEmitter chatStream(AiChatRequest request)
    {
        ChatStreamContext context = createChatStreamContext(request);
        long timeoutMs = Math.max(qwenProperties.getTimeoutSeconds() + 30, 60) * 1000L;
        SseEmitter emitter = new SseEmitter(timeoutMs);
        AtomicBoolean closed = new AtomicBoolean(false);
        emitter.onCompletion(() -> closed.set(true));
        emitter.onTimeout(() -> closed.set(true));
        emitter.onError(error -> closed.set(true));

        chatStreamExecutor.execute(() -> runChatStream(context, emitter, closed));
        return emitter;
    }

    @Override
    public List<String> listRecommendedQuestions()
    {
        List<String> questions = new ArrayList<>();
        List<String> topQuestions = messageMapper.selectTopUserQuestions(RECOMMENDED_QUESTION_LIMIT);
        appendRecommendedQuestions(questions, topQuestions);
        appendRecommendedQuestions(questions, DEFAULT_RECOMMENDED_QUESTIONS);
        return questions.size() > RECOMMENDED_QUESTION_LIMIT
                ? questions.subList(0, RECOMMENDED_QUESTION_LIMIT)
                : questions;
    }

    private ChatStreamContext createChatStreamContext(AiChatRequest request)
    {
        Long userId = requireUserId();
        AiModel model = resolveRuntimeModel(requireDefaultModel(MODEL_CHAT), MODEL_CHAT);
        Long conversationId = resolveConversationId(request.getConversationId(), userId);
        String flag = detectContentFlag(request.getQuestion());
        String flagReason = FLAG_NORMAL.equals(flag) ? null : "命中高风险饮食或健康建议关键词";
        int inputTokens = estimateTokens(request.getQuestion());
        LocalDateTime startedAt = LocalDateTime.now();

        AiMessage userMessage = aiConvert.toMessage(conversationId, userId, model, ROLE_USER, request.getQuestion(),
                inputTokens, 0, 0, flag, flagReason);
        messageMapper.insertMessage(userMessage);
        return new ChatStreamContext(request, conversationId, userId, model, userMessage, inputTokens, flag,
                flagReason, startedAt);
    }

    private void runChatStream(ChatStreamContext context, SseEmitter emitter, AtomicBoolean closed)
    {
        StringBuilder answerBuilder = new StringBuilder();
        try
        {
            sendSseComment(emitter, "stream-open");
            sendSseEvent(emitter, "meta", buildStreamMeta(context));
            AiProviderResult aiResult = generateChatAnswerStream(context.model(), context.request().getQuestion(),
                    context.flag(), delta -> {
                        answerBuilder.append(delta);
                        sendDeltaCharacters(emitter, closed, delta);
                    });
            String answer = StringUtils.isBlank(aiResult.content())
                    ? normalizeModelText(answerBuilder.toString())
                    : aiResult.content();
            int outputTokens = aiResult.outputTokens() == null ? estimateTokens(answer) : aiResult.outputTokens();
            int inputTokens = aiResult.inputTokens() == null ? context.inputTokens() : aiResult.inputTokens();
            int latencyMs = latencySince(context.startedAt());

            AiMessage assistantMessage = aiConvert.toMessage(context.conversationId(), context.userId(), context.model(),
                    ROLE_ASSISTANT, answer, inputTokens, outputTokens, latencyMs, context.flag(), context.flagReason());
            applyRagMetadata(assistantMessage, aiResult.ragResult());
            messageMapper.insertMessage(assistantMessage);
            AiChatResponseVo done = aiConvert.toChatResponse(context.conversationId(), context.userMessage(),
                    assistantMessage, context.model(), answer, inputTokens, outputTokens, latencyMs, context.flag(),
                    DISCLAIMER_CHAT);
            applyRagMetadata(done, aiResult.ragResult());
            if (!closed.get())
            {
                sendSseEvent(emitter, "done", done);
                emitter.complete();
            }
        }
        catch (Exception e)
        {
            handleChatStreamError(context, emitter, closed, answerBuilder, e);
        }
    }

    private JSONObject buildStreamMeta(ChatStreamContext context)
    {
        JSONObject data = new JSONObject();
        data.put("conversationId", context.conversationId());
        data.put("userMessageId", context.userMessage().getId());
        data.put("modelId", context.model().getId());
        data.put("modelName", context.model().getName());
        return data;
    }

    private JSONObject buildDelta(String content)
    {
        JSONObject data = new JSONObject();
        data.put("content", content);
        return data;
    }

    private void handleChatStreamError(ChatStreamContext context, SseEmitter emitter, AtomicBoolean closed,
            StringBuilder answerBuilder, Exception error)
    {
        AiMessage assistantMessage = null;
        String partialAnswer = normalizeModelText(answerBuilder.toString());
        if (StringUtils.isNotBlank(partialAnswer))
        {
            int latencyMs = latencySince(context.startedAt());
            assistantMessage = aiConvert.toMessage(context.conversationId(), context.userId(), context.model(),
                    ROLE_ASSISTANT, partialAnswer, context.inputTokens(), estimateTokens(partialAnswer), latencyMs,
                    context.flag(), context.flagReason());
            messageMapper.insertMessage(assistantMessage);
        }
        if (!closed.get())
        {
            try
            {
                JSONObject data = new JSONObject();
                data.put("message", StringUtils.isBlank(error.getMessage()) ? "AI流式回复失败" : error.getMessage());
                data.put("assistantMessageId", assistantMessage == null ? null : assistantMessage.getId());
                data.put("partial", assistantMessage != null);
                sendSseEvent(emitter, "error", data);
            }
            catch (IOException ignored)
            {
                // 客户端断开时不再尝试额外写入。
            }
            finally
            {
                emitter.complete();
            }
        }
    }

    private void sendSseEvent(SseEmitter emitter, String eventName, Object data) throws IOException
    {
        emitter.send(SseEmitter.event().name(eventName).data(JSON.toJSONString(data), MediaType.TEXT_PLAIN));
    }

    private void sendSseComment(SseEmitter emitter, String comment) throws IOException
    {
        emitter.send(SseEmitter.event().comment(comment));
    }

    private void sendDeltaCharacters(SseEmitter emitter, AtomicBoolean closed, String content) throws IOException
    {
        if (StringUtils.isEmpty(content))
        {
            return;
        }
        for (int offset = 0; offset < content.length();)
        {
            if (closed.get())
            {
                throw new IOException("SSE连接已关闭");
            }
            int codePoint = content.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            sendSseEvent(emitter, "delta", buildDelta(character));
            sleepBeforeNextCharacter(character);
            offset += Character.charCount(codePoint);
        }
    }

    private void sleepBeforeNextCharacter(String character) throws IOException
    {
        long delayMs = resolveCharacterDelayMs(character);
        if (delayMs <= 0)
        {
            return;
        }
        try
        {
            Thread.sleep(delayMs);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IOException("AI流式回复中断", e);
        }
    }

    private long resolveCharacterDelayMs(String character)
    {
        if ("\n".equals(character) || "\r".equals(character))
        {
            return STREAM_LINE_BREAK_DELAY_MS;
        }
        if ("。！？!?；;，,、：:".contains(character))
        {
            return STREAM_PUNCTUATION_DELAY_MS;
        }
        return STREAM_CHAR_DELAY_MS;
    }

    @Override
    public PageVo<AiConversationVo> listMyConversations(Integer page, Integer pageSize)
    {
        Long userId = requireUserId();
        startPage(page, pageSize);
        return toPage(messageMapper.selectUserConversations(userId), page, pageSize);
    }

    @Override
    public AiConversationDetailVo getMyConversation(Long id)
    {
        Long userId = requireUserId();
        List<AiMessage> messages = messageMapper.selectByConversationIdForUser(id, userId);
        if (messages.isEmpty())
        {
            throw new ServiceException("AI会话不存在");
        }
        return buildConversationDetail(id, messages, findSummary(messageMapper.selectUserConversations(userId), id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMyConversation(Long id)
    {
        Long userId = requireUserId();
        if (messageMapper.softDeleteConversationForUser(id, userId) == 0)
        {
            throw new ServiceException("AI会话不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiRecognitionVo recognizeFood(AiRecognizeFoodRequest request)
    {
        // 1. 识图和对话使用不同默认模型，避免管理端切换对话模型时误影响视觉识别。
        Long userId = requireUserId();
        AiModel model = resolveRuntimeModel(requireDefaultModel(MODEL_VISION), MODEL_VISION);
        LocalDateTime startedAt = LocalDateTime.now();

        // 2. 千问视觉模型返回结构化 JSON，业务层只负责校验、序列化和落库。
        JSONObject result = generateRecognitionResult(model, request.getImageUrl());
        JSONObject nutrition = result.getJSONObject("nutrition");
        JSONArray candidates = result.getJSONArray("candidates");
        String nutritionJson = nutrition.toJSONString();
        String candidatesJson = candidates.toJSONString();
        String resultJson = result.toJSONString();
        int latencyMs = latencySince(startedAt);

        // 3. 识图日志实体由 MapStruct 统一构建，减少业务层重复 set 字段。
        AiImageRecognitionLog log = aiConvert.toRecognitionLog(userId, model, request, result,
                nutritionJson, candidatesJson, resultJson, latencyMs, RECOGNITION_SUCCESS);
        recognitionMapper.insertLog(log);
        return aiConvert.toRecognitionVo(recognitionMapper.selectById(log.getId()));
    }

    @Override
    public PageVo<AiRecognitionVo> listMyRecognitions(String status, Integer page, Integer pageSize)
    {
        Long userId = requireUserId();
        startPage(page, pageSize);
        List<AiImageRecognitionLog> rows = recognitionMapper.selectUserList(userId, status);
        PageInfo<AiImageRecognitionLog> info = new PageInfo<>(rows);
        return new PageVo<>(normalizePage(page), normalizePageSize(pageSize), info.getTotal(),
                aiConvert.toRecognitionVoList(rows));
    }

    @Override
    public void deleteMyRecognition(Long id)
    {
        Long userId = requireUserId();
        if (recognitionMapper.softDeleteForUser(id, userId) == 0)
        {
            throw new ServiceException("识图记录不存在");
        }
    }

    @Override
    public PageVo<AiModelVo> listModels(String modelType, String status, String keyword, Integer page, Integer pageSize)
    {
        startPage(page, pageSize);
        List<AiModel> rows = modelMapper.selectList(modelType, status, keyword);
        PageInfo<AiModel> info = new PageInfo<>(rows);
        return new PageVo<>(normalizePage(page), normalizePageSize(pageSize), info.getTotal(),
                aiConvert.toModelVoList(rows));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiModelVo createModel(AiModelRequest request)
    {
        AiModel model = buildModel(request);
        if (Boolean.TRUE.equals(model.getIsDefault()))
        {
            modelMapper.clearDefaultByType(model.getModelType());
        }
        modelMapper.insertModel(model);
        return aiConvert.toModelVo(modelMapper.selectById(model.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiModelVo updateModel(Long id, AiModelRequest request)
    {
        AiModel existed = requireModel(id);
        AiModel model = buildModel(request);
        model.setId(id);
        model.setModelType(existed.getModelType());
        if (Boolean.TRUE.equals(model.getIsDefault()))
        {
            modelMapper.clearDefaultByType(existed.getModelType());
        }
        modelMapper.updateModel(model);
        return aiConvert.toModelVo(modelMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiModelVo enableModel(Long id)
    {
        AiModel model = requireModel(id);
        validateEnableChatModel(model);
        modelMapper.clearDefaultByType(model.getModelType());
        modelMapper.setDefault(id);
        return aiConvert.toModelVo(modelMapper.selectById(id));
    }

    @Override
    public AiModelTestVo testModel(Long id)
    {
        AiModel model = requireModel(id);
        LocalDateTime startedAt = LocalDateTime.now();
        String message = testRuntimeModel(model);
        int latency = latencySince(startedAt);
        modelMapper.updateTestResult(id, "success", latency, message);
        return aiConvert.toModelTestVo(id, "success", latency, message);
    }

    @Override
    public AiModelVo savePrompt(Long id, AiPromptRequest request)
    {
        AiModel model = requireModel(id);
        JSONObject config = parseObject(model.getConfigJson());
        if (StringUtils.isNotBlank(request.getFewShotExamplesJson()))
        {
            config.put("fewShotExamples", JSON.parse(request.getFewShotExamplesJson()));
        }
        model.setSystemPrompt(request.getSystemPrompt());
        model.setConfigJson(config.toJSONString());
        modelMapper.updateModel(model);
        return aiConvert.toModelVo(modelMapper.selectById(id));
    }

    @Override
    public PageVo<AiConversationVo> listConversationLogs(String keyword, String flag, String modelType, Integer page, Integer pageSize)
    {
        startPage(page, pageSize);
        return toPage(messageMapper.selectAdminConversations(keyword, flag, modelType), page, pageSize);
    }

    @Override
    public AiConversationDetailVo getConversationLog(Long id)
    {
        List<AiMessage> messages = messageMapper.selectByConversationId(id);
        if (messages.isEmpty())
        {
            throw new ServiceException("AI会话日志不存在");
        }
        return buildConversationDetail(id, messages, findSummary(messageMapper.selectAdminConversations(null, null, null), id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiConversationDetailVo markConversation(Long id, AiFlagRequest request)
    {
        String flag = normalizeFlag(request.getFlag());
        if (messageMapper.markConversation(id, flag, request.getReason()) == 0)
        {
            throw new ServiceException("AI会话日志不存在");
        }
        return getConversationLog(id);
    }

    @Override
    public PageVo<AiRecognitionVo> listRecognitionLogs(String keyword, String status, Long userId, Integer page, Integer pageSize)
    {
        startPage(page, pageSize);
        List<AiImageRecognitionLog> rows = recognitionMapper.selectAdminList(keyword, status, userId);
        PageInfo<AiImageRecognitionLog> info = new PageInfo<>(rows);
        return new PageVo<>(normalizePage(page), normalizePageSize(pageSize), info.getTotal(),
                aiConvert.toRecognitionVoList(rows));
    }

    @Override
    public AiRecognitionVo getRecognitionLog(Long id)
    {
        AiImageRecognitionLog log = recognitionMapper.selectById(id);
        if (log == null)
        {
            throw new ServiceException("识图日志不存在");
        }
        return aiConvert.toRecognitionVo(log);
    }

    @Override
    public void deleteRecognitionLog(Long id)
    {
        if (recognitionMapper.softDeleteById(id) == 0)
        {
            throw new ServiceException("识图日志不存在");
        }
    }

    private AiModel buildModel(AiModelRequest request)
    {
        String modelType = normalizeModelType(request.getModelType());
        AiModel model = aiConvert.toModel(request, modelType, normalizeJson(request.getConfigJson()),
                Boolean.TRUE.equals(request.getIsDefault()),
                StringUtils.isBlank(request.getStatus()) ? STATUS_ENABLED : request.getStatus());
        model.setProvider(normalizeProvider(model.getProvider()));
        model.setEncryptedApiKey(StringUtils.isBlank(model.getEncryptedApiKey()) ? null : model.getEncryptedApiKey());
        model.setApiBaseUrl(StringUtils.isBlank(model.getApiBaseUrl()) ? null : model.getApiBaseUrl());
        return model;
    }

    private Long resolveConversationId(Long conversationId, Long userId)
    {
        if (conversationId == null)
        {
            return messageMapper.selectNextConversationId();
        }
        if (messageMapper.existsConversationForUser(conversationId, userId) == 0)
        {
            throw new ServiceException("AI会话不存在");
        }
        return conversationId;
    }

    private AiModel requireDefaultModel(String modelType)
    {
        AiModel model = modelMapper.selectDefaultByType(modelType);
        if (model == null)
        {
            throw new ServiceException("未配置默认" + ("chat".equals(modelType) ? "对话" : "视觉") + "模型");
        }
        return model;
    }

    private AiModel resolveRuntimeModel(AiModel dbModel, String modelType)
    {
        if (MODEL_CHAT.equals(modelType))
        {
            return prepareChatRuntimeModel(dbModel);
        }
        if (!qwenProperties.isEnabled())
        {
            return dbModel;
        }
        String modelName = getQwenModelName(modelType);
        assertQwenConfigured(modelType, modelName);

        return aiConvert.toRuntimeModel(dbModel, "阿里云通义千问-" + modelName, PROVIDER_QWEN,
                modelName, qwenProperties.getBaseUrl());
    }

    private AiModel requireModel(Long id)
    {
        AiModel model = modelMapper.selectById(id);
        if (model == null)
        {
            throw new ServiceException("AI模型不存在");
        }
        return model;
    }

    private AiModel prepareChatRuntimeModel(AiModel dbModel)
    {
        String provider = normalizeProvider(dbModel.getProvider());
        validateSupportedProvider(provider);
        String baseUrl = resolveChatBaseUrl(dbModel, provider);
        String apiKey = resolveChatApiKey(dbModel, provider);
        if (StringUtils.isBlank(dbModel.getModelCode()))
        {
            throw new ServiceException("未配置对话模型标识，请检查模型配置");
        }
        if (StringUtils.isBlank(apiKey))
        {
            throw new ServiceException("未配置对话模型API Key，请检查模型配置");
        }
        return aiConvert.toRuntimeModel(dbModel, dbModel.getName(), provider, dbModel.getModelCode(), baseUrl);
    }

    private AiProviderResult generateChatAnswer(AiModel model, String question, String flag)
    {
        AiRagRetrieveResult ragResult = retrieveKnowledge(question);
        if (FLAG_VIOLATION.equals(flag))
        {
            return new AiProviderResult("这个问题涉及不安全的饮食或健康行为，我不能提供执行方案。建议保持规律饮食，并咨询医生或注册营养师。"
                    + " " + DISCLAIMER_CHAT, null, null, AiRagRetrieveResult.miss("内容安全拒答"));
        }
        if (ragResult.isHit())
        {
            AiProviderResult result = callConfiguredChatModel(model, buildRagSystemPrompt(model),
                    buildRagUserPrompt(question, ragResult));
            return result.withRagResult(ragResult);
        }
        if (!knowledgeServiceAllowsFallback())
        {
            return new AiProviderResult("当前知识库暂未覆盖这个问题，暂时无法基于知识库回答。", null, null, ragResult);
        }
        AiProviderResult result = callConfiguredChatModel(model, defaultChatPrompt(model), question);
        return result.withRagResult(ragResult);
    }

    private AiProviderResult generateChatAnswerStream(AiModel model, String question, String flag,
            AiStreamConsumer consumer) throws IOException
    {
        AiRagRetrieveResult ragResult = retrieveKnowledge(question);
        if (FLAG_VIOLATION.equals(flag))
        {
            String answer = "这个问题涉及不安全的饮食或健康行为，我不能提供执行方案。建议保持规律饮食，并咨询医生或注册营养师。"
                    + " " + DISCLAIMER_CHAT;
            consumer.accept(answer);
            return new AiProviderResult(answer, null, null, AiRagRetrieveResult.miss("内容安全拒答"));
        }
        if (ragResult.isHit())
        {
            AiProviderResult result = callConfiguredChatModelStream(model, buildRagSystemPrompt(model),
                    buildRagUserPrompt(question, ragResult), consumer);
            return result.withRagResult(ragResult);
        }
        if (!knowledgeServiceAllowsFallback())
        {
            String answer = "当前知识库暂未覆盖这个问题，暂时无法基于知识库回答。";
            consumer.accept(answer);
            return new AiProviderResult(answer, null, null, ragResult);
        }
        AiProviderResult result = callConfiguredChatModelStream(model, defaultChatPrompt(model), question, consumer);
        return result.withRagResult(ragResult);
    }

    private AiRagRetrieveResult retrieveKnowledge(String question)
    {
        try
        {
            return knowledgeService.retrieve(question);
        }
        catch (Exception e)
        {
            log.warn("RAG检索异常，降级通用AI回答：{}", e.getMessage());
            return AiRagRetrieveResult.miss("知识库检索异常：" + e.getMessage());
        }
    }

    private boolean knowledgeServiceAllowsFallback()
    {
        return ragProperties.isAllowGeneralFallback();
    }

    private String buildRagSystemPrompt(AiModel model)
    {
        return defaultChatPrompt(model)
                + "\n你正在进行知识库增强问答。回答时优先依据用户问题中提供的【知识库资料】。"
                + "如果资料能回答问题，请给出具体、可执行的饮食建议，并在末尾用“参考来源”列出知识库标题。"
                + "不要编造知识库中不存在的来源；涉及疾病、孕期、儿童、老人等场景时必须保留风险提示。"
                + "全程使用纯文本，不使用Markdown标题、加粗、列表符号、表格或代码块。";
    }

    private String buildRagUserPrompt(String question, AiRagRetrieveResult ragResult)
    {
        return "【知识库资料】\n" + ragResult.getContext()
                + "\n\n【用户问题】\n" + question
                + "\n\n请基于知识库资料用纯文本回答。";
    }

    private AiProviderResult callConfiguredChatModel(AiModel model, String systemPrompt, String userPrompt)
    {
        if (canUseSpringAiDashScope(model))
        {
            try
            {
                String content = ChatClient.builder(springAiChatModel)
                        .build()
                        .prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .options(buildDashScopeOptions(model, false))
                        .call()
                        .content();
                return new AiProviderResult(normalizeModelText(content), null, null);
            }
            catch (Exception e)
            {
                log.warn("Spring AI DashScope兼容调用失败，回退OpenAI兼容调用：{}", e.getMessage());
            }
        }
        JSONArray messages = new JSONArray();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        return callOpenAiCompatibleChat(model, messages);
    }

    private AiProviderResult callConfiguredChatModelStream(AiModel model, String systemPrompt, String userPrompt,
            AiStreamConsumer consumer) throws IOException
    {
        if (canUseSpringAiDashScope(model))
        {
            try
            {
                StringBuilder answer = new StringBuilder();
                Iterable<String> chunks = ChatClient.builder(springAiChatModel)
                        .build()
                        .prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .options(buildDashScopeOptions(model, true))
                        .stream()
                        .content()
                        .toIterable();
                for (String chunk : chunks)
                {
                    if (!StringUtils.isEmpty(chunk))
                    {
                        answer.append(chunk);
                        consumer.accept(chunk);
                    }
                }
                return new AiProviderResult(normalizeModelText(answer.toString()), null, null);
            }
            catch (IOException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                log.warn("Spring AI DashScope兼容流式调用失败，回退OpenAI兼容调用：{}", e.getMessage());
            }
        }
        JSONArray messages = new JSONArray();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        return callOpenAiCompatibleChatStream(model, messages, consumer);
    }

    private boolean canUseSpringAiDashScope(AiModel model)
    {
        return springAiChatModel != null && PROVIDER_QWEN.equals(model.getProvider());
    }

    private DashScopeChatOptions buildDashScopeOptions(AiModel model, boolean stream)
    {
        JSONObject config = parseObject(model.getConfigJson());
        DashScopeChatOptions.DashScopeChatOptionsBuilder builder = DashScopeChatOptions.builder()
                .model(model.getModelCode());
        if (config.getBigDecimal("temperature") != null)
        {
            builder.temperature(config.getBigDecimal("temperature").doubleValue());
        }
        if (config.getInteger("max_tokens") != null)
        {
            builder.maxToken(config.getInteger("max_tokens"));
        }
        if (config.getBigDecimal("top_p") != null)
        {
            builder.topP(config.getBigDecimal("top_p").doubleValue());
        }
        if (config.getBigDecimal("repetition_penalty") != null)
        {
            builder.repetitionPenalty(config.getBigDecimal("repetition_penalty").doubleValue());
        }
        String apiKey = resolveChatApiKey(model, model.getProvider());
        if (StringUtils.isNotBlank(apiKey))
        {
            builder.httpHeaders(Map.of("Authorization", "Bearer " + apiKey));
        }
        return builder.stream(stream).build();
    }

    private void applyRagMetadata(AiMessage message, AiRagRetrieveResult ragResult)
    {
        if (message == null || ragResult == null)
        {
            return;
        }
        message.setRagHit(ragResult.isHit());
        message.setRagSourcesJson(JSON.toJSONString(ragResult.getSources()));
        message.setFallbackReason(ragResult.getFallbackReason());
    }

    private void applyRagMetadata(AiChatResponseVo response, AiRagRetrieveResult ragResult)
    {
        if (response == null || ragResult == null)
        {
            return;
        }
        response.setRagHit(ragResult.isHit());
        response.setSources(ragResult.getSources());
        response.setFallbackReason(ragResult.getFallbackReason());
    }

    private JSONObject generateRecognitionResult(AiModel model, String imageUrl)
    {
        if (PROVIDER_QWEN.equals(model.getProvider()))
        {
            return normalizeRecognitionResult(callQwenVision(model, imageUrl));
        }
        String lower = imageUrl == null ? "" : imageUrl.toLowerCase();
        String name = lower.contains("salad") ? "蔬菜鸡胸沙拉" : lower.contains("rice") ? "鸡肉米饭套餐" : "综合餐盘";
        int calories = "蔬菜鸡胸沙拉".equals(name) ? 285 : "鸡肉米饭套餐".equals(name) ? 520 : 430;

        JSONObject nutrition = new JSONObject();
        nutrition.put("protein", Map.of("grams", 28, "percent", 32));
        nutrition.put("fat", Map.of("grams", 12, "percent", 28));
        nutrition.put("carbohydrate", Map.of("grams", 46, "percent", 40));
        nutrition.put("fiber", Map.of("grams", 6, "percent", 0));

        JSONArray candidates = new JSONArray();
        candidates.add(Map.of("name", name, "confidence", 86));
        candidates.add(Map.of("name", "家常便当", "confidence", 62));
        candidates.add(Map.of("name", "轻食套餐", "confidence", 55));

        JSONObject result = new JSONObject();
        result.put("recognizedName", name);
        result.put("confidence", new BigDecimal("86.00"));
        result.put("calories", calories);
        result.put("nutrition", nutrition);
        result.put("candidates", candidates);
        result.put("suggestion", "这份餐食整体结构较均衡，建议优先保证蛋白质摄入，若正在控脂可减少酱料和精制主食。");
        result.put("disclaimer", DISCLAIMER_RECOGNITION);
        return result;
    }

    private AiProviderResult callOpenAiCompatibleChat(AiModel model, JSONArray messages)
    {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model.getModelCode());
        requestBody.put("messages", messages);
        applyModelParameters(model, requestBody);

        JSONObject response = postOpenAiCompatible(model, OPENAI_CHAT_COMPLETIONS_PATH, requestBody);
        JSONObject message = response.getJSONArray("choices").getJSONObject(0).getJSONObject("message");
        JSONObject usage = response.getJSONObject("usage");
        String content = normalizeModelText(message.getString("content"));
        Integer promptTokens = usage == null ? null : usage.getInteger("prompt_tokens");
        Integer completionTokens = usage == null ? null : usage.getInteger("completion_tokens");
        return new AiProviderResult(content, promptTokens, completionTokens);
    }

    private AiProviderResult callOpenAiCompatibleChatStream(AiModel model, JSONArray messages,
            AiStreamConsumer consumer) throws IOException
    {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model.getModelCode());
        requestBody.put("messages", messages);
        requestBody.put("stream", true);
        applyModelParameters(model, requestBody);

        HttpResponse<Stream<String>> response = postOpenAiCompatibleStream(model, OPENAI_CHAT_COMPLETIONS_PATH,
                requestBody);
        StringBuilder answer = new StringBuilder();
        Integer promptTokens = null;
        Integer completionTokens = null;
        try (Stream<String> lines = response.body())
        {
            Iterator<String> iterator = lines.iterator();
            while (iterator.hasNext())
            {
                String line = iterator.next();
                if (line == null)
                {
                    continue;
                }
                String payload = extractSsePayload(line);
                if (payload == null)
                {
                    continue;
                }
                if ("[DONE]".equals(payload))
                {
                    break;
                }
                JSONObject chunk = JSON.parseObject(payload);
                JSONObject usage = chunk.getJSONObject("usage");
                if (usage != null)
                {
                    promptTokens = usage.getInteger("prompt_tokens");
                    completionTokens = usage.getInteger("completion_tokens");
                }
                JSONArray choices = chunk.getJSONArray("choices");
                if (choices == null || choices.isEmpty())
                {
                    continue;
                }
                JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                String content = delta == null ? null : delta.getString("content");
                if (content != null && !content.isEmpty())
                {
                    answer.append(content);
                    consumer.accept(content);
                }
            }
        }
        return new AiProviderResult(normalizeModelText(answer.toString()), promptTokens, completionTokens);
    }

    private JSONObject callQwenVision(AiModel model, String imageUrl)
    {
        JSONArray content = new JSONArray();
        content.add(Map.of("type", "image_url", "image_url", Map.of("url", imageUrl)));
        content.add(Map.of("type", "text", "text", "请识别图片中的主要食物，估算每份热量和营养结构。只返回JSON对象，字段必须包含：recognizedName、confidence、calories、nutrition、candidates、suggestion、disclaimer。nutrition包含protein、fat、carbohydrate、fiber，每项包含grams和percent。"));

        JSONArray messages = new JSONArray();
        messages.add(Map.of("role", "system", "content", "你是码上智厨的食物识图营养分析模型。必须返回严格JSON，不要使用Markdown代码块。"));
        messages.add(Map.of("role", "user", "content", content));

        AiProviderResult result = callOpenAiCompatibleChat(model, messages);
        return JSON.parseObject(extractJsonObject(result.content()));
    }

    private JSONObject postOpenAiCompatible(AiModel model, String path, JSONObject requestBody)
    {
        try
        {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(qwenProperties.getTimeoutSeconds()))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildProviderUrl(model.getApiBaseUrl(), path)))
                    .timeout(Duration.ofSeconds(qwenProperties.getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + resolveChatApiKey(model, model.getProvider()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toJSONString(), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300)
            {
                throw new ServiceException("AI模型调用失败：" + response.statusCode() + " " + response.body());
            }
            return JSON.parseObject(response.body());
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("AI模型调用异常：" + e.getMessage());
        }
    }

    private HttpResponse<Stream<String>> postOpenAiCompatibleStream(AiModel model, String path, JSONObject requestBody)
    {
        try
        {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(qwenProperties.getTimeoutSeconds()))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildProviderUrl(model.getApiBaseUrl(), path)))
                    .timeout(Duration.ofSeconds(qwenProperties.getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + resolveChatApiKey(model, model.getProvider()))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toJSONString(), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<Stream<String>> response = client.send(request, HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
            {
                String responseBody;
                try (Stream<String> lines = response.body())
                {
                    responseBody = lines.limit(20).reduce("", (left, right) -> left + right);
                }
                throw new ServiceException("AI模型流式调用失败：" + response.statusCode() + " " + responseBody);
            }
            return response;
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("AI模型流式调用异常：" + e.getMessage());
        }
    }

    private String extractSsePayload(String line)
    {
        String value = line.trim();
        if (value.isEmpty() || value.startsWith(":") || !value.startsWith("data:"))
        {
            return null;
        }
        return value.substring("data:".length()).trim();
    }

    private JSONObject normalizeRecognitionResult(JSONObject raw)
    {
        JSONObject nutrition = raw.getJSONObject("nutrition");
        if (nutrition == null)
        {
            nutrition = new JSONObject();
        }
        JSONObject result = new JSONObject();
        result.put("recognizedName", raw.getString("recognizedName"));
        result.put("confidence", raw.getBigDecimal("confidence") == null ? new BigDecimal("0") : raw.getBigDecimal("confidence"));
        result.put("calories", raw.getInteger("calories") == null ? 0 : raw.getInteger("calories"));
        result.put("nutrition", nutrition);
        result.put("candidates", raw.getJSONArray("candidates") == null ? new JSONArray() : raw.getJSONArray("candidates"));
        result.put("suggestion", StringUtils.isBlank(raw.getString("suggestion")) ? "识图成功，请结合实际份量调整摄入。" : raw.getString("suggestion"));
        result.put("disclaimer", StringUtils.isBlank(raw.getString("disclaimer")) ? DISCLAIMER_RECOGNITION : raw.getString("disclaimer"));
        return result;
    }

    private String defaultChatPrompt(AiModel model)
    {
        String plainTextRule = "请默认使用纯文本回答，不使用Markdown标题、加粗、列表符号、表格或代码块。";
        if (StringUtils.isNotBlank(model.getSystemPrompt()))
        {
            return model.getSystemPrompt() + " " + plainTextRule + " " + DISCLAIMER_CHAT;
        }
        return "你是码上智厨的AI营养师。回答要具体、温和、可执行，不推荐极端饮食，不替代专业医疗建议。"
                + plainTextRule + DISCLAIMER_CHAT;
    }

    private String testRuntimeModel(AiModel model)
    {
        AiModel runtimeModel = resolveRuntimeModel(model, model.getModelType());
        if (MODEL_CHAT.equals(runtimeModel.getModelType()))
        {
            JSONArray messages = new JSONArray();
            messages.add(Map.of("role", "user", "content", "请回复：连通成功"));
            AiProviderResult result = callOpenAiCompatibleChat(runtimeModel, messages);
            return "模型连通成功：" + result.content();
        }
        if (PROVIDER_QWEN.equals(runtimeModel.getProvider()))
        {
            return "千问视觉模型配置完整：" + runtimeModel.getModelCode();
        }
        throw new ServiceException("当前模型类型暂不支持连通测试");
    }

    private void validateEnableChatModel(AiModel model)
    {
        if (!MODEL_CHAT.equals(model.getModelType()))
        {
            throw new ServiceException("当前仅支持切换对话模型，视觉模型请保持独立配置");
        }
        String provider = normalizeProvider(model.getProvider());
        validateSupportedProvider(provider);
        resolveChatBaseUrl(model, provider);
        if (StringUtils.isBlank(resolveChatApiKey(model, provider)))
        {
            throw new ServiceException("启用失败：该模型未配置API Key");
        }
        if (StringUtils.isBlank(model.getModelCode()))
        {
            throw new ServiceException("启用失败：该模型未配置模型标识");
        }
    }

    private void validateSupportedProvider(String provider)
    {
        if (!PROVIDER_QWEN.equals(provider)
                && !PROVIDER_DEEPSEEK.equals(provider)
                && !PROVIDER_ZHIPU.equals(provider)
                && !PROVIDER_BAIDU_QIANFAN.equals(provider)
                && !PROVIDER_OPENAI_COMPATIBLE.equals(provider))
        {
            throw new ServiceException("不支持的模型供应商：" + provider);
        }
    }

    private String normalizeProvider(String provider)
    {
        if (StringUtils.isBlank(provider))
        {
            throw new ServiceException("未配置模型供应商");
        }
        String value = provider.trim().toLowerCase();
        if ("aliyun".equals(value) || "qwen".equals(value) || "dashscope".equals(value))
        {
            return PROVIDER_QWEN;
        }
        if ("baidu".equals(value) || "qianfan".equals(value))
        {
            return PROVIDER_BAIDU_QIANFAN;
        }
        if ("bigmodel".equals(value) || "glm".equals(value))
        {
            return PROVIDER_ZHIPU;
        }
        return value;
    }

    private String resolveChatApiKey(AiModel model, String provider)
    {
        if (StringUtils.isNotBlank(model.getEncryptedApiKey()))
        {
            return model.getEncryptedApiKey();
        }
        return PROVIDER_QWEN.equals(provider) ? qwenProperties.getApiKey() : null;
    }

    private String resolveChatBaseUrl(AiModel model, String provider)
    {
        if (StringUtils.isNotBlank(model.getApiBaseUrl()))
        {
            return model.getApiBaseUrl();
        }
        if (PROVIDER_QWEN.equals(provider))
        {
            return StringUtils.isBlank(qwenProperties.getBaseUrl()) ? BASE_URL_QWEN : qwenProperties.getBaseUrl();
        }
        if (PROVIDER_DEEPSEEK.equals(provider))
        {
            return BASE_URL_DEEPSEEK;
        }
        if (PROVIDER_ZHIPU.equals(provider))
        {
            return BASE_URL_ZHIPU;
        }
        if (PROVIDER_BAIDU_QIANFAN.equals(provider))
        {
            return BASE_URL_BAIDU_QIANFAN;
        }
        throw new ServiceException("启用失败：自定义OpenAI兼容模型必须配置API地址");
    }

    private void applyModelParameters(AiModel model, JSONObject requestBody)
    {
        JSONObject config = parseObject(model.getConfigJson());
        requestBody.put("temperature", config.getBigDecimal("temperature") == null ? new BigDecimal("0.7") : config.getBigDecimal("temperature"));
        if (config.getInteger("max_tokens") != null)
        {
            requestBody.put("max_tokens", config.getInteger("max_tokens"));
        }
        if (config.getBigDecimal("top_p") != null)
        {
            requestBody.put("top_p", config.getBigDecimal("top_p"));
        }
    }

    private void assertQwenConfigured(String modelType, String modelName)
    {
        if (StringUtils.isBlank(qwenProperties.getBaseUrl()))
        {
            throw new ServiceException("未配置千问base-url，请检查cook-ai.yml");
        }
        if (StringUtils.isBlank(qwenProperties.getApiKey()))
        {
            throw new ServiceException("未配置千问API Key，请设置DASHSCOPE_API_KEY或填写cook-ai.yml");
        }
        if (StringUtils.isBlank(modelName))
        {
            throw new ServiceException("未配置千问" + (MODEL_CHAT.equals(modelType) ? "对话" : "视觉") + "模型名，请检查cook-ai.yml");
        }
    }

    private String getQwenModelName(String modelType)
    {
        return MODEL_CHAT.equals(modelType)
                ? qwenProperties.getChat().getModelName()
                : qwenProperties.getVision().getModelName();
    }

    private String buildProviderUrl(String baseUrl, String path)
    {
        return (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl) + path;
    }

    private String normalizeModelText(String content)
    {
        if (StringUtils.isBlank(content))
        {
            return "";
        }
        String normalized = content.replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("(?m)^```[a-zA-Z0-9_-]*\\s*$", "")
                .replaceAll("(?m)^```\\s*$", "")
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replaceAll("(?m)^\\s{0,3}#{1,6}\\s*", "")
                .replaceAll("(?m)^\\s{0,3}>\\s?", "")
                .replaceAll("(?m)^\\s*[-*+]\\s+", "")
                .replaceAll("(?m)^\\s*\\d+[.)、]\\s+", "")
                .replaceAll("(?m)^\\s*\\[[ xX]\\]\\s+", "")
                .replaceAll("(?m)^\\s*\\|?\\s*:?-{3,}:?\\s*(\\|\\s*:?-{3,}:?\\s*)+\\|?\\s*$", "");
        String[] lines = normalized.split("\\n", -1);
        StringBuilder plain = new StringBuilder();
        for (String line : lines)
        {
            String current = line.trim();
            if (current.startsWith("|") && current.endsWith("|") && current.length() > 1)
            {
                current = current.substring(1, current.length() - 1).replace("|", "，").trim();
            }
            else
            {
                current = current.replace("|", "，").trim();
            }
            if (plain.length() > 0)
            {
                plain.append('\n');
            }
            plain.append(current);
        }
        return plain.toString().replaceAll("\\n{3,}", "\n\n").trim();
    }

    private String extractJsonObject(String content)
    {
        String text = content == null ? "" : content.trim();
        if (text.startsWith("```"))
        {
            text = text.replaceFirst("^```[a-zA-Z]*", "").replaceFirst("```$", "").trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start)
        {
            throw new ServiceException("千问视觉模型未返回JSON结果：" + normalizeModelText(content));
        }
        return text.substring(start, end + 1);
    }

    private String detectContentFlag(String content)
    {
        String text = content == null ? "" : content;
        if (text.contains("绝食") || text.contains("催吐") || text.contains("极端减肥"))
        {
            return FLAG_VIOLATION;
        }
        if (text.contains("糖尿病") || text.contains("孕期") || text.contains("疾病"))
        {
            return FLAG_WARNING;
        }
        return FLAG_NORMAL;
    }

    private AiConversationDetailVo buildConversationDetail(Long id, List<AiMessage> messages, AiConversationVo summary)
    {
        AiConversationVo safeSummary = summary == null ? new AiConversationVo() : summary;
        safeSummary.setConversationId(id);
        return aiConvert.toConversationDetail(safeSummary, aiConvert.toMessageVoList(messages));
    }

    private AiConversationVo findSummary(List<AiConversationVo> rows, Long conversationId)
    {
        return rows.stream().filter(row -> conversationId.equals(row.getConversationId())).findFirst().orElse(null);
    }

    private String normalizeModelType(String modelType)
    {
        if (MODEL_CHAT.equals(modelType) || MODEL_VISION.equals(modelType))
        {
            return modelType;
        }
        throw new ServiceException("模型类型仅支持chat或vision");
    }

    private String normalizeFlag(String flag)
    {
        if (FLAG_NORMAL.equals(flag) || FLAG_WARNING.equals(flag) || FLAG_VIOLATION.equals(flag))
        {
            return flag;
        }
        throw new ServiceException("标记仅支持normal、warning或violation");
    }

    private String normalizeJson(String json)
    {
        if (StringUtils.isBlank(json))
        {
            return "{}";
        }
        return JSON.parse(json).toString();
    }

    private JSONObject parseObject(String json)
    {
        return StringUtils.isBlank(json) ? new JSONObject() : JSON.parseObject(json);
    }

    private int estimateTokens(String text)
    {
        return Math.max(1, (text == null ? 0 : text.length()) / 2);
    }

    private void appendRecommendedQuestions(List<String> target, List<String> source)
    {
        if (source == null)
        {
            return;
        }
        for (String question : source)
        {
            if (target.size() >= RECOMMENDED_QUESTION_LIMIT)
            {
                return;
            }
            String normalized = question == null ? "" : question.trim();
            if (StringUtils.isNotBlank(normalized) && !target.contains(normalized))
            {
                target.add(normalized);
            }
        }
    }

    private int latencySince(LocalDateTime startedAt)
    {
        return (int) Math.max(1, Duration.between(startedAt, LocalDateTime.now()).toMillis());
    }

    private Long requireUserId()
    {
        Long userId = SecurityUtils.getUserId();
        if (userId == null)
        {
            throw new ServiceException("用户未登录");
        }
        return userId;
    }

    private void startPage(Integer page, Integer pageSize)
    {
        PageHelper.startPage(normalizePage(page), normalizePageSize(pageSize));
    }

    private int normalizePage(Integer page)
    {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    private int normalizePageSize(Integer pageSize)
    {
        if (pageSize == null || pageSize < 1)
        {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private <T> PageVo<T> toPage(List<T> rows, Integer page, Integer pageSize)
    {
        PageInfo<T> info = new PageInfo<>(rows);
        return new PageVo<>(normalizePage(page), normalizePageSize(pageSize), info.getTotal(), rows);
    }

    private record ChatStreamContext(AiChatRequest request, Long conversationId, Long userId, AiModel model,
            AiMessage userMessage, Integer inputTokens, String flag, String flagReason, LocalDateTime startedAt)
    {
    }

    @FunctionalInterface
    private interface AiStreamConsumer
    {
        void accept(String content) throws IOException;
    }

    private record AiProviderResult(String content, Integer inputTokens, Integer outputTokens,
            AiRagRetrieveResult ragResult)
    {
        private AiProviderResult(String content, Integer inputTokens, Integer outputTokens)
        {
            this(content, inputTokens, outputTokens, null);
        }

        private AiProviderResult withRagResult(AiRagRetrieveResult ragResult)
        {
            return new AiProviderResult(content, inputTokens, outputTokens, ragResult);
        }
    }
}
