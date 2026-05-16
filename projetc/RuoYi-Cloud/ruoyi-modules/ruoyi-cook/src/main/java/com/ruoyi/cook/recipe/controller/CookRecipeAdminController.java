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
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.cook.common.domain.vo.PageVo;
import com.ruoyi.cook.recipe.domain.dto.RecipeAuditRejectRequest;
import com.ruoyi.cook.recipe.domain.vo.RecipeDetailVo;
import com.ruoyi.cook.recipe.domain.vo.RecipeListItemVo;
import com.ruoyi.cook.recipe.service.ICookRecipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 管理端菜谱接口：列表、审核、上下架和转码触发。
 */
@Tag(name = "管理端-菜谱管理", description = "菜谱列表、审核通过/驳回、上下架和视频转码")
@RestController
@RequestMapping("/api/admin/v1")
public class CookRecipeAdminController
{
    @Autowired
    private ICookRecipeService recipeService;

    @Operation(summary = "菜谱管理列表", description = "管理端分页查询全部菜谱，支持审核状态和发布状态筛选")
    @RequiresPermissions("cook:recipe:list")
    @GetMapping("/recipes")
    public R<PageVo<RecipeListItemVo>> listRecipes(
            @Parameter(description = "审核状态：pending_review/approved/rejected") @RequestParam(required = false) String reviewStatus,
            @Parameter(description = "发布状态：published/draft/offline") @RequestParam(required = false) String publishStatus,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(recipeService.listAdminRecipes(reviewStatus, publishStatus, keyword, page, pageSize));
    }

    @Operation(summary = "菜谱管理详情", description = "查看任意菜谱的完整信息，含所有版本")
    @RequiresPermissions("cook:recipe:query")
    @GetMapping("/recipes/{id}")
    public R<RecipeDetailVo> getRecipe(@Parameter(description = "菜谱ID") @PathVariable Long id)
    {
        return R.ok(recipeService.getAdminRecipeDetail(id));
    }

    @Operation(summary = "删除菜谱", description = "管理端删除菜谱（软删除）")
    @RequiresPermissions("cook:recipe:remove")
    @DeleteMapping("/recipes/{id}")
    public R<?> deleteRecipe(@Parameter(description = "菜谱ID") @PathVariable Long id)
    {
        recipeService.adminDeleteRecipe(id);
        return R.ok();
    }

    @Operation(summary = "上架菜谱", description = "将已审核通过的菜谱上架到公开列表")
    @RequiresPermissions("cook:recipe:edit")
    @PostMapping("/recipes/{id}/online")
    public R<RecipeDetailVo> onlineRecipe(@Parameter(description = "菜谱ID") @PathVariable Long id)
    {
        return R.ok(recipeService.onlineRecipe(id));
    }

    @Operation(summary = "下架菜谱", description = "将已上架菜谱从公开列表下架")
    @RequiresPermissions("cook:recipe:edit")
    @PostMapping("/recipes/{id}/offline")
    public R<RecipeDetailVo> offlineRecipe(@Parameter(description = "菜谱ID") @PathVariable Long id)
    {
        return R.ok(recipeService.offlineRecipe(id));
    }

    @Operation(summary = "触发视频转码", description = "手动触发菜谱视频的 HLS 转码任务")
    @RequiresPermissions("cook:recipe:edit")
    @PostMapping("/recipes/{id}/video/transcode")
    public R<RecipeDetailVo> triggerVideoTranscode(@Parameter(description = "菜谱ID") @PathVariable Long id)
    {
        return R.ok(recipeService.triggerVideoTranscode(id));
    }

    @Operation(summary = "菜谱审核列表", description = "分页查询待审核菜谱，可按状态和关键词筛选")
    @RequiresPermissions("cook:recipe:audit")
    @GetMapping("/recipe-audits")
    public R<PageVo<RecipeListItemVo>> listRecipeAudits(
            @Parameter(description = "审核状态：pending_review/approved/rejected") @RequestParam(required = false) String status,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(recipeService.listRecipeAudits(status, keyword, page, pageSize));
    }

    @Operation(summary = "菜谱审核详情", description = "查看待审核菜谱的详细信息")
    @RequiresPermissions("cook:recipe:audit")
    @GetMapping("/recipe-audits/{recipeId}")
    public R<RecipeDetailVo> getRecipeAudit(@Parameter(description = "菜谱ID") @PathVariable Long recipeId)
    {
        return R.ok(recipeService.getAdminRecipeDetail(recipeId));
    }

    @Operation(summary = "通过菜谱审核", description = "审核通过后菜谱进入可上架状态")
    @RequiresPermissions("cook:recipe:audit")
    @PostMapping("/recipe-audits/{recipeId}/approve")
    public R<RecipeDetailVo> approveRecipe(@Parameter(description = "菜谱ID") @PathVariable Long recipeId)
    {
        return R.ok(recipeService.approveRecipe(recipeId));
    }

    @Operation(summary = "驳回菜谱审核", description = "驳回时需填写驳回原因，用户可见")
    @RequiresPermissions("cook:recipe:audit")
    @PostMapping("/recipe-audits/{recipeId}/reject")
    public R<RecipeDetailVo> rejectRecipe(@Parameter(description = "菜谱ID") @PathVariable Long recipeId,
            @Valid @RequestBody RecipeAuditRejectRequest request)
    {
        return R.ok(recipeService.rejectRecipe(recipeId, request.getReason()));
    }
}
