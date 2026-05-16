package com.ruoyi.cook.message.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cook.message.domain.Conversation;

/**
 * 会话 Mapper。
 */
public interface ConversationMapper
{
    Conversation selectById(Long id);

    Conversation selectPrivateConversation(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);

    Conversation selectNotificationConversation(Long userId);

    List<Conversation> selectUserConversations(@Param("userId") Long userId, @Param("type") String type);

    int insertConversation(Conversation conversation);

    int updateLastMessage(@Param("id") Long id, @Param("lastMessageId") Long lastMessageId,
            @Param("preview") String preview);
}
