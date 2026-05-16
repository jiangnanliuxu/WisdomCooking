package com.ruoyi.cook.message.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.security.annotation.RequiresLogin;
import com.ruoyi.cook.common.domain.vo.PageVo;
import com.ruoyi.cook.message.domain.dto.ConversationSettingsRequest;
import com.ruoyi.cook.message.domain.dto.PrivateConversationRequest;
import com.ruoyi.cook.message.domain.dto.SendMessageRequest;
import com.ruoyi.cook.message.domain.vo.ConversationVo;
import com.ruoyi.cook.message.domain.vo.MessageVo;
import com.ruoyi.cook.message.service.IMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 消息中心接口。
 * <p>
 * 私信、群聊、通知三类 Tab 都通过 conversations 统一承载。
 * </p>
 */
@Tag(name = "用户端-消息中心", description = "会话列表、私信会话、历史消息、发送消息和已读设置")
@RequiresLogin
@RestController
@RequestMapping("/api/v1")
public class CookMessageController
{
    @Autowired
    private IMessageService messageService;

    @Operation(summary = "消息中心会话列表", description = "type 可选 private/group/notification")
    @GetMapping("/conversations")
    public R<PageVo<ConversationVo>> listConversations(@Parameter(description = "会话类型") @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(messageService.listConversations(type, page, pageSize));
    }

    @Operation(summary = "创建或获取私信会话", description = "若双方已有私信会话则直接返回已有会话")
    @PostMapping("/conversations/private")
    public R<ConversationVo> createPrivateConversation(@Valid @RequestBody PrivateConversationRequest request)
    {
        return R.ok(messageService.createOrGetPrivateConversation(request));
    }

    @Operation(summary = "通知列表", description = "通知作为 notification 类型会话中的系统消息展示")
    @GetMapping("/notifications")
    public R<PageVo<MessageVo>> listNotifications(@RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(messageService.listNotifications(page, pageSize));
    }

    @Operation(summary = "历史消息", description = "进入会话后分页拉取历史消息")
    @GetMapping("/conversations/{id}/messages")
    public R<PageVo<MessageVo>> listMessages(@PathVariable Long id, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(messageService.listMessages(id, page, pageSize));
    }

    @Operation(summary = "发送消息", description = "发送前校验当前用户状态和会话成员权限")
    @PostMapping("/conversations/{id}/messages")
    public R<MessageVo> sendMessage(@PathVariable Long id, @Valid @RequestBody SendMessageRequest request)
    {
        return R.ok(messageService.sendMessage(id, request));
    }

    @Operation(summary = "标记会话已读", description = "清空当前用户在该会话的未读数")
    @PostMapping("/conversations/{id}/read")
    public R<?> markRead(@PathVariable Long id)
    {
        messageService.markRead(id);
        return R.ok();
    }

    @Operation(summary = "更新会话设置", description = "支持免打扰和置顶")
    @PutMapping("/conversations/{id}/settings")
    public R<ConversationVo> updateSettings(@PathVariable Long id,
            @Valid @RequestBody ConversationSettingsRequest request)
    {
        return R.ok(messageService.updateSettings(id, request));
    }
}
