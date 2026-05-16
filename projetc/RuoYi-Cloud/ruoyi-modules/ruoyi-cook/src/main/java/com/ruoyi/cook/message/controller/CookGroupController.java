package com.ruoyi.cook.message.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.security.annotation.RequiresLogin;
import com.ruoyi.cook.message.domain.dto.GroupCreateRequest;
import com.ruoyi.cook.message.domain.dto.GroupInviteRequest;
import com.ruoyi.cook.message.domain.vo.GroupMemberVo;
import com.ruoyi.cook.message.domain.vo.GroupVo;
import com.ruoyi.cook.message.service.IMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 群聊接口。
 */
@Tag(name = "用户端-群聊", description = "用户自主创建群聊、查看群详情、成员列表、邀请和退出")
@RequiresLogin
@RestController
@RequestMapping("/api/v1/groups")
public class CookGroupController
{
    @Autowired
    private IMessageService messageService;

    @Operation(summary = "创建群聊", description = "创建者自动成为群主，并创建对应 group 类型会话")
    @PostMapping
    public R<GroupVo> createGroup(@Valid @RequestBody GroupCreateRequest request)
    {
        return R.ok(messageService.createGroup(request));
    }

    @Operation(summary = "群详情")
    @GetMapping("/{id}")
    public R<GroupVo> getGroup(@PathVariable Long id)
    {
        return R.ok(messageService.getGroup(id));
    }

    @Operation(summary = "群成员列表")
    @GetMapping("/{id}/members")
    public R<List<GroupMemberVo>> listMembers(@PathVariable Long id)
    {
        return R.ok(messageService.listGroupMembers(id));
    }

    @Operation(summary = "邀请成员")
    @PostMapping("/{id}/invite")
    public R<GroupVo> inviteMembers(@PathVariable Long id, @Valid @RequestBody GroupInviteRequest request)
    {
        return R.ok(messageService.inviteMembers(id, request));
    }

    @Operation(summary = "退出群聊", description = "第四阶段暂不支持群主直接退出")
    @PostMapping("/{id}/leave")
    public R<?> leaveGroup(@PathVariable Long id)
    {
        messageService.leaveGroup(id);
        return R.ok();
    }
}
