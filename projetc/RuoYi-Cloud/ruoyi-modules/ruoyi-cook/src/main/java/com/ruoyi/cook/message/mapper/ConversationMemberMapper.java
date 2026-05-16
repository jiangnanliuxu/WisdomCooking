package com.ruoyi.cook.message.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cook.message.domain.ConversationMember;

/**
 * 会话成员 Mapper。
 */
public interface ConversationMemberMapper
{
    ConversationMember selectOne(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    List<ConversationMember> selectByConversationId(Long conversationId);

    List<ConversationMember> selectGroupMembers(Long groupId);

    int insertMember(ConversationMember member);

    int restoreMember(@Param("conversationId") Long conversationId, @Param("userId") Long userId,
            @Param("role") String role);

    int increaseUnreadForOthers(@Param("conversationId") Long conversationId, @Param("senderId") Long senderId);

    int markRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId,
            @Param("lastReadMessageId") Long lastReadMessageId);

    int updateSettings(@Param("conversationId") Long conversationId, @Param("userId") Long userId,
            @Param("muted") Boolean muted, @Param("pinned") Boolean pinned);

    int leaveConversation(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}
