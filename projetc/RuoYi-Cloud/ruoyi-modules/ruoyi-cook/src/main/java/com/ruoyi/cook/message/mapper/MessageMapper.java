package com.ruoyi.cook.message.mapper;

import java.util.List;
import com.ruoyi.cook.message.domain.Message;

/**
 * 消息 Mapper。
 */
public interface MessageMapper
{
    Message selectById(Long id);

    List<Message> selectByConversationId(Long conversationId);

    int insertMessage(Message message);
}
