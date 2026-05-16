package com.ruoyi.cook.message.mapper;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.cook.message.domain.GroupChat;

/**
 * 群组 Mapper。
 */
public interface GroupMapper
{
    GroupChat selectById(Long id);

    int insertGroup(GroupChat group);

    int updateConversationId(@Param("id") Long id, @Param("conversationId") Long conversationId);
}
