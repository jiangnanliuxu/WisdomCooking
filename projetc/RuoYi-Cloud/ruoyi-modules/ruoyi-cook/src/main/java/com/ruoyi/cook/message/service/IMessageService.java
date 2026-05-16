package com.ruoyi.cook.message.service;

import java.util.List;
import com.ruoyi.cook.common.domain.vo.PageVo;
import com.ruoyi.cook.message.domain.dto.ConversationSettingsRequest;
import com.ruoyi.cook.message.domain.dto.GroupCreateRequest;
import com.ruoyi.cook.message.domain.dto.GroupInviteRequest;
import com.ruoyi.cook.message.domain.dto.PrivateConversationRequest;
import com.ruoyi.cook.message.domain.dto.SendMessageRequest;
import com.ruoyi.cook.message.domain.vo.ConversationVo;
import com.ruoyi.cook.message.domain.vo.GroupMemberVo;
import com.ruoyi.cook.message.domain.vo.GroupVo;
import com.ruoyi.cook.message.domain.vo.MessageVo;

/**
 * 消息与群组服务接口。
 */
public interface IMessageService
{
    PageVo<ConversationVo> listConversations(String type, Integer page, Integer pageSize);

    ConversationVo createOrGetPrivateConversation(PrivateConversationRequest request);

    PageVo<MessageVo> listNotifications(Integer page, Integer pageSize);

    PageVo<MessageVo> listMessages(Long conversationId, Integer page, Integer pageSize);

    MessageVo sendMessage(Long conversationId, SendMessageRequest request);

    void markRead(Long conversationId);

    ConversationVo updateSettings(Long conversationId, ConversationSettingsRequest request);

    GroupVo createGroup(GroupCreateRequest request);

    GroupVo getGroup(Long id);

    List<GroupMemberVo> listGroupMembers(Long id);

    GroupVo inviteMembers(Long id, GroupInviteRequest request);

    void leaveGroup(Long id);
}
