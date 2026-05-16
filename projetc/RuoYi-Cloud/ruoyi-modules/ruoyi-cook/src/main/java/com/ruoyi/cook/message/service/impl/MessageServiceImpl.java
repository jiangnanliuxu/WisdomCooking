package com.ruoyi.cook.message.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.cook.auth.domain.CookUser;
import com.ruoyi.cook.auth.mapper.CookUserMapper;
import com.ruoyi.cook.common.domain.vo.PageVo;
import com.ruoyi.cook.message.domain.Conversation;
import com.ruoyi.cook.message.domain.ConversationMember;
import com.ruoyi.cook.message.domain.GroupChat;
import com.ruoyi.cook.message.domain.Message;
import com.ruoyi.cook.message.domain.dto.ConversationSettingsRequest;
import com.ruoyi.cook.message.domain.dto.GroupCreateRequest;
import com.ruoyi.cook.message.domain.dto.GroupInviteRequest;
import com.ruoyi.cook.message.domain.dto.PrivateConversationRequest;
import com.ruoyi.cook.message.domain.dto.SendMessageRequest;
import com.ruoyi.cook.message.domain.vo.ConversationVo;
import com.ruoyi.cook.message.domain.vo.GroupMemberVo;
import com.ruoyi.cook.message.domain.vo.GroupVo;
import com.ruoyi.cook.message.domain.vo.MessageVo;
import com.ruoyi.cook.message.mapper.ConversationMapper;
import com.ruoyi.cook.message.mapper.ConversationMemberMapper;
import com.ruoyi.cook.message.mapper.GroupMapper;
import com.ruoyi.cook.message.mapper.MessageMapper;
import com.ruoyi.cook.message.service.IMessageService;

/**
 * 消息与群组服务实现。
 * <p>
 * 第四阶段实现入库、分页、未读数和基础 WebSocket 推送；
 * 复杂在线状态、撤回、群管理权限细分留到后续阶段。
 * </p>
 */
@Service
public class MessageServiceImpl implements IMessageService
{
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private static final String TYPE_PRIVATE = "private";
    private static final String TYPE_GROUP = "group";
    private static final String TYPE_NOTIFICATION = "notification";

    private static final String ROLE_OWNER = "owner";
    private static final String ROLE_MEMBER = "member";
    private static final String ROLE_SYSTEM = "system";
    private static final String STATUS_NORMAL = "normal";
    private static final String STATUS_DISSOLVED = "dissolved";
    private static final String MESSAGE_STATUS_NORMAL = "normal";
    private static final String MESSAGE_TEXT = "text";
    private static final String MESSAGE_IMAGE = "image";
    private static final String MESSAGE_VOICE = "voice";
    private static final String MESSAGE_SYSTEM = "system";

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private ConversationMemberMapper memberMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private CookUserMapper userMapper;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public PageVo<ConversationVo> listConversations(String type, Integer page, Integer pageSize)
    {
        Long userId = requireUserId();
        startPage(page, pageSize);
        List<ConversationVo> rows = conversationMapper.selectUserConversations(userId, type).stream()
                .map(this::toConversationVo)
                .toList();
        return toPage(rows, page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationVo createOrGetPrivateConversation(PrivateConversationRequest request)
    {
        Long userId = requireUserId();
        Long targetUserId = request.getTargetUserId();
        if (userId.equals(targetUserId))
        {
            throw new ServiceException("不能和自己创建私信");
        }
        assertUserExists(targetUserId);
        Conversation existed = conversationMapper.selectPrivateConversation(userId, targetUserId);
        if (existed != null)
        {
            return toConversationVo(fillPrivateTitle(existed, userId));
        }

        Conversation conversation = new Conversation();
        conversation.setType(TYPE_PRIVATE);
        conversationMapper.insertConversation(conversation);
        insertOrRestoreMember(conversation.getId(), userId, ROLE_MEMBER);
        insertOrRestoreMember(conversation.getId(), targetUserId, ROLE_MEMBER);
        return toConversationVo(fillPrivateTitle(conversationMapper.selectById(conversation.getId()), userId));
    }

    @Override
    public PageVo<MessageVo> listNotifications(Integer page, Integer pageSize)
    {
        Long userId = requireUserId();
        Conversation conversation = ensureNotificationConversation(userId);
        return listMessages(conversation.getId(), page, pageSize);
    }

    @Override
    public PageVo<MessageVo> listMessages(Long conversationId, Integer page, Integer pageSize)
    {
        Long userId = requireUserId();
        assertMember(conversationId, userId);
        startPage(page, pageSize);
        List<MessageVo> rows = messageMapper.selectByConversationId(conversationId).stream()
                .map(this::toMessageVo)
                .toList();
        return toPage(rows, page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageVo sendMessage(Long conversationId, SendMessageRequest request)
    {
        Long userId = requireUserId();
        assertUserCanSend(userId);
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null)
        {
            throw new ServiceException("会话不存在");
        }
        assertMember(conversationId, userId);
        if (TYPE_GROUP.equals(conversation.getType()))
        {
            assertGroupAvailable(conversation.getTargetId());
        }
        String messageType = normalizeMessageType(request.getMessageType());
        validateMessageBody(messageType, request);
        String contentJson = buildContentJson(request);

        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setMessageType(messageType);
        message.setContentJson(contentJson);
        message.setStatus(MESSAGE_STATUS_NORMAL);
        messageMapper.insertMessage(message);

        String preview = buildPreview(messageType, request);
        conversationMapper.updateLastMessage(conversationId, message.getId(), preview);
        memberMapper.increaseUnreadForOthers(conversationId, userId);
        MessageVo vo = toMessageVo(messageMapper.selectById(message.getId()));
        pushMessage(conversationId, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long conversationId)
    {
        Long userId = requireUserId();
        assertMember(conversationId, userId);
        Conversation conversation = conversationMapper.selectById(conversationId);
        memberMapper.markRead(conversationId, userId, conversation == null ? null : conversation.getLastMessageId());
        pushConversationUpdated(conversationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationVo updateSettings(Long conversationId, ConversationSettingsRequest request)
    {
        Long userId = requireUserId();
        assertMember(conversationId, userId);
        memberMapper.updateSettings(conversationId, userId, request.getMuted(), request.getPinned());
        return conversationMapper.selectUserConversations(userId, null).stream()
                .filter(conversation -> conversationId.equals(conversation.getId()))
                .findFirst()
                .map(this::toConversationVo)
                .orElseThrow(() -> new ServiceException("会话不存在"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupVo createGroup(GroupCreateRequest request)
    {
        Long userId = requireUserId();
        assertUserCanSend(userId);
        GroupChat group = new GroupChat();
        group.setOwnerId(userId);
        group.setName(request.getName());
        group.setAvatarMediaId(request.getAvatarMediaId());
        group.setIntro(request.getIntro());
        group.setNotice(request.getNotice());
        group.setStatus(STATUS_NORMAL);
        groupMapper.insertGroup(group);

        Conversation conversation = new Conversation();
        conversation.setType(TYPE_GROUP);
        conversation.setTargetId(group.getId());
        conversationMapper.insertConversation(conversation);
        groupMapper.updateConversationId(group.getId(), conversation.getId());

        insertOrRestoreMember(conversation.getId(), userId, ROLE_OWNER);
        for (Long memberId : normalizeMemberIds(request.getMemberIds(), userId))
        {
            assertUserExists(memberId);
            insertOrRestoreMember(conversation.getId(), memberId, ROLE_MEMBER);
        }
        sendSystemMessage(conversation.getId(), "群聊已创建");
        return toGroupVo(groupMapper.selectById(group.getId()));
    }

    @Override
    public GroupVo getGroup(Long id)
    {
        Long userId = requireUserId();
        GroupChat group = loadGroup(id);
        assertMember(group.getConversationId(), userId);
        return toGroupVo(group);
    }

    @Override
    public List<GroupMemberVo> listGroupMembers(Long id)
    {
        Long userId = requireUserId();
        GroupChat group = loadGroup(id);
        assertMember(group.getConversationId(), userId);
        return memberMapper.selectGroupMembers(id).stream().map(this::toGroupMemberVo).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupVo inviteMembers(Long id, GroupInviteRequest request)
    {
        Long userId = requireUserId();
        GroupChat group = loadGroup(id);
        assertMember(group.getConversationId(), userId);
        for (Long memberId : normalizeMemberIds(request.getUserIds(), userId))
        {
            assertUserExists(memberId);
            ConversationMember existed = memberMapper.selectOne(group.getConversationId(), memberId);
            insertOrRestoreMember(group.getConversationId(), memberId, ROLE_MEMBER);
            if (existed == null || !STATUS_NORMAL.equals(existed.getStatus()))
            {
                sendGroupInviteNotification(memberId, group);
            }
        }
        sendSystemMessage(group.getConversationId(), "有新成员加入群聊");
        return toGroupVo(groupMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveGroup(Long id)
    {
        Long userId = requireUserId();
        GroupChat group = loadGroup(id);
        if (userId.equals(group.getOwnerId()))
        {
            throw new ServiceException("群主暂不能退出群聊");
        }
        assertMember(group.getConversationId(), userId);
        memberMapper.leaveConversation(group.getConversationId(), userId);
        sendSystemMessage(group.getConversationId(), "有成员退出群聊");
    }

    private Conversation ensureNotificationConversation(Long userId)
    {
        Conversation existed = conversationMapper.selectNotificationConversation(userId);
        if (existed != null)
        {
            return existed;
        }
        Conversation conversation = new Conversation();
        conversation.setType(TYPE_NOTIFICATION);
        conversation.setTargetId(userId);
        conversationMapper.insertConversation(conversation);
        insertOrRestoreMember(conversation.getId(), userId, ROLE_SYSTEM);
        return conversationMapper.selectById(conversation.getId());
    }

    private void sendSystemMessage(Long conversationId, String content)
    {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(null);
        message.setMessageType(MESSAGE_SYSTEM);
        Map<String, Object> body = new HashMap<>();
        body.put("content", content);
        message.setContentJson(JSON.toJSONString(body));
        message.setStatus(MESSAGE_STATUS_NORMAL);
        messageMapper.insertMessage(message);
        conversationMapper.updateLastMessage(conversationId, message.getId(), content);
        memberMapper.increaseUnreadForOthers(conversationId, 0L);
        pushMessage(conversationId, toMessageVo(messageMapper.selectById(message.getId())));
    }

    private void sendGroupInviteNotification(Long userId, GroupChat group)
    {
        Conversation notification = ensureNotificationConversation(userId);
        sendSystemMessage(notification.getId(), "你已被邀请加入群聊「" + group.getName() + "」");
    }

    private void insertOrRestoreMember(Long conversationId, Long userId, String role)
    {
        ConversationMember existed = memberMapper.selectOne(conversationId, userId);
        if (existed == null)
        {
            ConversationMember member = new ConversationMember();
            member.setConversationId(conversationId);
            member.setUserId(userId);
            member.setRole(role);
            member.setStatus(STATUS_NORMAL);
            memberMapper.insertMember(member);
        }
        else if (!STATUS_NORMAL.equals(existed.getStatus()))
        {
            memberMapper.restoreMember(conversationId, userId, role);
        }
    }

    private void assertMember(Long conversationId, Long userId)
    {
        ConversationMember member = memberMapper.selectOne(conversationId, userId);
        if (member == null || !STATUS_NORMAL.equals(member.getStatus()))
        {
            throw new ServiceException("无权访问该会话");
        }
    }

    private void assertUserCanSend(Long userId)
    {
        CookUser user = userMapper.selectById(userId);
        if (user == null || !"normal".equals(user.getStatus()))
        {
            throw new ServiceException("当前账号状态不能发送消息");
        }
    }

    private void assertUserExists(Long userId)
    {
        CookUser user = userMapper.selectById(userId);
        if (user == null)
        {
            throw new ServiceException("用户不存在");
        }
    }

    private GroupChat loadGroup(Long id)
    {
        GroupChat group = groupMapper.selectById(id);
        if (group == null)
        {
            throw new ServiceException("群聊不存在");
        }
        return group;
    }

    private void assertGroupAvailable(Long groupId)
    {
        GroupChat group = loadGroup(groupId);
        if (!STATUS_NORMAL.equals(group.getStatus()) && !StringUtils.equals("warned", group.getStatus()))
        {
            throw new ServiceException("群聊状态不可发送消息");
        }
    }

    private Set<Long> normalizeMemberIds(List<Long> memberIds, Long ownerId)
    {
        Set<Long> result = new LinkedHashSet<>();
        if (memberIds != null)
        {
            result.addAll(memberIds);
        }
        result.remove(ownerId);
        return result;
    }

    private String normalizeMessageType(String messageType)
    {
        if (MESSAGE_TEXT.equals(messageType) || MESSAGE_IMAGE.equals(messageType)
                || MESSAGE_VOICE.equals(messageType) || MESSAGE_SYSTEM.equals(messageType))
        {
            return messageType;
        }
        throw new ServiceException("不支持的消息类型");
    }

    /**
     * 校验消息内容。
     * <p>
     * 文本消息必须有可见文本，避免测试报告 Bug 4.1 中的空消息入库；
     * 图片和语音消息允许 content 为空，但必须带媒体地址，方便后续前端按类型渲染。
     * </p>
     */
    private void validateMessageBody(String messageType, SendMessageRequest request)
    {
        if (MESSAGE_TEXT.equals(messageType) && StringUtils.isBlank(request.getContent()))
        {
            throw new ServiceException("消息内容不能为空");
        }
        if ((MESSAGE_IMAGE.equals(messageType) || MESSAGE_VOICE.equals(messageType))
                && StringUtils.isBlank(request.getMediaUrl()))
        {
            throw new ServiceException("媒体地址不能为空");
        }
    }

    private String buildContentJson(SendMessageRequest request)
    {
        Map<String, Object> body = new HashMap<>();
        body.put("content", request.getContent());
        body.put("mediaUrl", request.getMediaUrl());
        return JSON.toJSONString(body);
    }

    private String buildPreview(String messageType, SendMessageRequest request)
    {
        if (MESSAGE_TEXT.equals(messageType))
        {
            return StringUtils.substring(request.getContent(), 0, 80);
        }
        if (MESSAGE_IMAGE.equals(messageType))
        {
            return "[图片]";
        }
        if (MESSAGE_VOICE.equals(messageType))
        {
            return "[语音]";
        }
        return "[系统消息]";
    }

    private Conversation fillPrivateTitle(Conversation conversation, Long userId)
    {
        List<Conversation> rows = conversationMapper.selectUserConversations(userId, TYPE_PRIVATE);
        return rows.stream().filter(row -> conversation.getId().equals(row.getId())).findFirst().orElse(conversation);
    }

    private ConversationVo toConversationVo(Conversation conversation)
    {
        ConversationVo vo = new ConversationVo();
        vo.setId(conversation.getId());
        vo.setType(conversation.getType());
        vo.setTargetId(conversation.getTargetId());
        vo.setTitle(conversation.getTitle());
        vo.setAvatarUrl(conversation.getAvatarUrl());
        vo.setLastMessageId(conversation.getLastMessageId());
        vo.setLastMessagePreview(conversation.getLastMessagePreview());
        vo.setLastMessageAt(conversation.getLastMessageAt());
        vo.setUnreadCount(conversation.getUnreadCount() == null ? 0 : conversation.getUnreadCount());
        vo.setMuted(Boolean.TRUE.equals(conversation.getMuted()));
        vo.setPinned(Boolean.TRUE.equals(conversation.getPinned()));
        return vo;
    }

    private MessageVo toMessageVo(Message message)
    {
        MessageVo vo = new MessageVo();
        vo.setId(message.getId());
        vo.setConversationId(message.getConversationId());
        vo.setSenderId(message.getSenderId());
        vo.setSenderNickname(message.getSenderNickname());
        vo.setSenderAvatarUrl(message.getSenderAvatarUrl());
        vo.setMessageType(message.getMessageType());
        vo.setStatus(message.getStatus());
        vo.setCreatedAt(message.getCreatedAt());
        JSONObject body = JSON.parseObject(message.getContentJson());
        vo.setContent(body == null ? null : body.getString("content"));
        vo.setMediaUrl(body == null ? null : body.getString("mediaUrl"));
        return vo;
    }

    private GroupVo toGroupVo(GroupChat group)
    {
        GroupVo vo = new GroupVo();
        vo.setId(group.getId());
        vo.setOwnerId(group.getOwnerId());
        vo.setOwnerNickname(group.getOwnerNickname());
        vo.setConversationId(group.getConversationId());
        vo.setName(group.getName());
        vo.setAvatarMediaId(group.getAvatarMediaId());
        vo.setIntro(group.getIntro());
        vo.setNotice(group.getNotice());
        vo.setStatus(group.getStatus());
        vo.setMemberCount(group.getMemberCount());
        vo.setMessageCount(group.getMessageCount());
        vo.setCreatedAt(group.getCreatedAt());
        return vo;
    }

    private GroupMemberVo toGroupMemberVo(ConversationMember member)
    {
        GroupMemberVo vo = new GroupMemberVo();
        vo.setUserId(member.getUserId());
        vo.setNickname(member.getNickname());
        vo.setAvatarUrl(member.getAvatarUrl());
        vo.setRole(member.getRole());
        vo.setStatus(member.getStatus());
        vo.setJoinedAt(member.getJoinedAt());
        return vo;
    }

    private void pushMessage(Long conversationId, MessageVo message)
    {
        if (messagingTemplate != null)
        {
            // 订阅 /topic/conversations/{id} 的客户端会收到新消息。
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, message);
            pushConversationUpdated(conversationId);
        }
    }

    private void pushConversationUpdated(Long conversationId)
    {
        if (messagingTemplate != null)
        {
            Map<String, Object> payload = new HashMap<>();
            payload.put("conversationId", conversationId);
            payload.put("event", "conversation.updated");
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/events", (Object) payload);
        }
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

    private <T> PageVo<T> toPage(List<T> rows, Integer page, Integer pageSize)
    {
        PageInfo<T> pageInfo = new PageInfo<>(rows);
        return new PageVo<>(normalizePage(page), normalizePageSize(pageSize), pageInfo.getTotal(), new ArrayList<>(rows));
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
}
