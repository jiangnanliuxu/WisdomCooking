package com.ruoyi.cook.community.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.ruoyi.cook.community.domain.dto.PostSaveRequest;
import com.ruoyi.cook.community.domain.vo.PostVo;
import com.ruoyi.cook.community.domain.vo.TopicVo;
import com.ruoyi.cook.community.service.ICommunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 用户端社区动态接口。
 * <p>
 * 所有动态先审后发：创建和编辑后都不会立即进入社区公开流。
 * </p>
 */
@Tag(name = "用户端-社区动态", description = "动态发布、社区广场、我的动态和动态互动")
@RestController
@RequestMapping("/api/v1")
public class CookPostController
{
    @Autowired
    private ICommunityService communityService;

    @Operation(summary = "社区动态流", description = "只返回已审核通过且公开可见的动态，支持话题、关键词和关联菜谱菜系筛选")
    @GetMapping("/posts")
    public R<PageVo<PostVo>> listPosts(@Parameter(description = "话题编码") @RequestParam(required = false) String topicCode,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "关联菜谱菜系编码，other 表示非八大菜系或未关联菜谱") @RequestParam(required = false) String recipeCategoryCode,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(communityService.listPublicPosts(topicCode, keyword, recipeCategoryCode, page, pageSize));
    }

    @Operation(summary = "动态详情", description = "公开动态所有人可见，未公开动态仅作者本人可见")
    @GetMapping("/posts/{id}")
    public R<PostVo> getPost(@PathVariable Long id)
    {
        return R.ok(communityService.getPostDetail(id));
    }

    @Operation(summary = "创建动态", description = "创建后直接进入 pending_review，等待后台审核")
    @RequiresLogin
    @PostMapping("/posts")
    public R<PostVo> createPost(@Valid @RequestBody PostSaveRequest request)
    {
        return R.ok(communityService.createPost(request));
    }

    @Operation(summary = "编辑动态", description = "编辑后重新进入审核，避免已发布内容直接替换上线")
    @RequiresLogin
    @PutMapping("/posts/{id}")
    public R<PostVo> updatePost(@PathVariable Long id, @Valid @RequestBody PostSaveRequest request)
    {
        return R.ok(communityService.updatePost(id, request));
    }

    @Operation(summary = "提交动态审核", description = "用于撤回或驳回后的动态重新提交审核")
    @RequiresLogin
    @PostMapping("/posts/{id}/submit")
    public R<PostVo> submitPost(@PathVariable Long id)
    {
        return R.ok(communityService.submitPost(id));
    }

    @Operation(summary = "撤回动态审核", description = "仅审核中的动态可以撤回")
    @RequiresLogin
    @PostMapping("/posts/{id}/withdraw")
    public R<PostVo> withdrawPost(@PathVariable Long id)
    {
        return R.ok(communityService.withdrawPost(id));
    }

    @Operation(summary = "删除自己的动态", description = "软删除，不物理删除数据")
    @RequiresLogin
    @DeleteMapping("/posts/{id}")
    public R<?> deletePost(@PathVariable Long id)
    {
        communityService.deletePost(id);
        return R.ok();
    }

    @Operation(summary = "点赞动态")
    @RequiresLogin
    @PostMapping("/posts/{id}/like")
    public R<?> likePost(@PathVariable Long id)
    {
        communityService.likePost(id);
        return R.ok();
    }

    @Operation(summary = "取消点赞动态")
    @RequiresLogin
    @DeleteMapping("/posts/{id}/like")
    public R<?> unlikePost(@PathVariable Long id)
    {
        communityService.unlikePost(id);
        return R.ok();
    }

    @Operation(summary = "收藏动态")
    @RequiresLogin
    @PostMapping("/posts/{id}/favorite")
    public R<?> favoritePost(@PathVariable Long id)
    {
        communityService.favoritePost(id);
        return R.ok();
    }

    @Operation(summary = "取消收藏动态")
    @RequiresLogin
    @DeleteMapping("/posts/{id}/favorite")
    public R<?> unfavoritePost(@PathVariable Long id)
    {
        communityService.unfavoritePost(id);
        return R.ok();
    }

    @Operation(summary = "我的动态列表", description = "展示当前用户自己的全部动态，可按状态筛选")
    @RequiresLogin
    @GetMapping("/users/me/posts")
    public R<PageVo<PostVo>> listMyPosts(@RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(communityService.listMyPosts(status, page, pageSize));
    }

    @Operation(summary = "社区话题列表", description = "第三阶段使用后端固定话题配置")
    @GetMapping("/topics")
    public R<List<TopicVo>> listTopics()
    {
        return R.ok(communityService.listTopics());
    }
}
