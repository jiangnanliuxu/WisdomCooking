package com.ruoyi.cook.community.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.cook.common.domain.vo.PageVo;
import com.ruoyi.cook.community.domain.dto.PostBlockRequest;
import com.ruoyi.cook.community.domain.vo.PostVo;
import com.ruoyi.cook.community.service.ICommunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 管理端动态审核接口。
 */
@Tag(name = "管理端-动态审核", description = "动态审核列表、通过、屏蔽和恢复")
@RestController
@RequestMapping("/api/admin/v1")
public class CookPostAdminController
{
    @Autowired
    private ICommunityService communityService;

    @Operation(summary = "动态审核列表", description = "默认查询 pending_review，可按状态和关键词筛选")
    @RequiresPermissions("cook:post:audit")
    @GetMapping("/post-audits")
    public R<PageVo<PostVo>> listPostAudits(@RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(communityService.listAdminPosts(status, keyword, page, pageSize));
    }

    @Operation(summary = "动态审核详情")
    @RequiresPermissions("cook:post:audit")
    @GetMapping("/post-audits/{postId}")
    public R<PostVo> getPostAudit(@PathVariable Long postId)
    {
        return R.ok(communityService.getAdminPostDetail(postId));
    }

    @Operation(summary = "通过动态审核", description = "通过后动态进入社区公开流")
    @RequiresPermissions("cook:post:audit")
    @PostMapping("/post-audits/{postId}/approve")
    public R<PostVo> approvePost(@PathVariable Long postId)
    {
        return R.ok(communityService.approvePost(postId));
    }

    @Operation(summary = "屏蔽动态", description = "支持仅屏蔽、屏蔽并警告、屏蔽并禁言")
    @RequiresPermissions("cook:post:audit")
    @PostMapping("/post-audits/{postId}/block")
    public R<PostVo> blockPost(@PathVariable Long postId, @Valid @RequestBody PostBlockRequest request)
    {
        return R.ok(communityService.blockPost(postId, request));
    }

    @Operation(summary = "恢复动态", description = "已屏蔽动态恢复为 published")
    @RequiresPermissions("cook:post:audit")
    @PostMapping("/post-audits/{postId}/restore")
    public R<PostVo> restorePost(@PathVariable Long postId)
    {
        return R.ok(communityService.restorePost(postId));
    }
}
