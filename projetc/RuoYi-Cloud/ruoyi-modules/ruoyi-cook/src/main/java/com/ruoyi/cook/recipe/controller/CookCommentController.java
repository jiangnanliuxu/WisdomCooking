package com.ruoyi.cook.recipe.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.security.annotation.RequiresLogin;
import com.ruoyi.cook.common.domain.vo.PageVo;
import com.ruoyi.cook.recipe.domain.dto.CommentCreateRequest;
import com.ruoyi.cook.recipe.domain.vo.CommentVo;
import com.ruoyi.cook.recipe.service.ICookRecipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 评论接口，第二阶段先支持菜谱评论和评论点赞。
 */
@Tag(name = "用户端-评论", description = "菜谱评论列表、发表评论、删除评论和评论点赞")
@RestController
@RequestMapping("/api/v1/comments")
public class CookCommentController
{
    @Autowired
    private ICookRecipeService recipeService;

    @Operation(summary = "评论列表", description = "按评论对象（菜谱/动态）分页查询评论，支持一级评论和二级回复")
    @GetMapping
    public R<PageVo<CommentVo>> listComments(
            @Parameter(description = "评论对象类型：recipe/post") @RequestParam("target_type") String targetType,
            @Parameter(description = "评论对象ID") @RequestParam("target_id") Long targetId,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(recipeService.listComments(targetType, targetId, page, pageSize));
    }

    @Operation(summary = "发表评论", description = "支持一级评论和二级回复（传 parentId 表示回复）")
    @RequiresLogin
    @PostMapping
    public R<CommentVo> createComment(@Valid @RequestBody CommentCreateRequest request)
    {
        return R.ok(recipeService.createComment(request));
    }

    @Operation(summary = "删除评论", description = "仅评论作者可删除自己的评论")
    @RequiresLogin
    @DeleteMapping("/{id}")
    public R<?> deleteComment(@Parameter(description = "评论ID") @PathVariable Long id)
    {
        recipeService.deleteComment(id);
        return R.ok();
    }

    @Operation(summary = "点赞评论")
    @RequiresLogin
    @PostMapping("/{id}/like")
    public R<?> likeComment(@Parameter(description = "评论ID") @PathVariable Long id)
    {
        recipeService.likeComment(id);
        return R.ok();
    }

    @Operation(summary = "取消点赞评论")
    @RequiresLogin
    @DeleteMapping("/{id}/like")
    public R<?> unlikeComment(@Parameter(description = "评论ID") @PathVariable Long id)
    {
        recipeService.unlikeComment(id);
        return R.ok();
    }
}
