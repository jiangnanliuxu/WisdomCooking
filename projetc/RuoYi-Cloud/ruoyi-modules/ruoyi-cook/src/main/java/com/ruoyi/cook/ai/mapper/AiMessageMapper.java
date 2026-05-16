package com.ruoyi.cook.ai.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cook.ai.domain.AiMessage;
import com.ruoyi.cook.ai.vo.AiConversationVo;

/**
 * AI 对话消息 Mapper。
 */
public interface AiMessageMapper
{
    Long selectNextConversationId();

    AiMessage selectById(Long id);

    int existsConversationForUser(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    List<AiMessage> selectByConversationId(@Param("conversationId") Long conversationId);

    List<AiMessage> selectByConversationIdForUser(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    List<AiConversationVo> selectUserConversations(@Param("userId") Long userId);

    List<String> selectTopUserQuestions(@Param("limit") Integer limit);

    List<AiConversationVo> selectAdminConversations(@Param("keyword") String keyword,
            @Param("flag") String flag, @Param("modelType") String modelType);

    int insertMessage(AiMessage message);

    int markConversation(@Param("conversationId") Long conversationId,
            @Param("flag") String flag, @Param("reason") String reason);

    int softDeleteConversationForUser(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}
