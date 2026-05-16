package com.ruoyi.cook.recipe.controller;

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
import com.ruoyi.cook.recipe.domain.dto.RecipeSaveRequest;
import com.ruoyi.cook.recipe.domain.vo.RecipeDetailVo;
import com.ruoyi.cook.recipe.domain.vo.RecipeListItemVo;
import com.ruoyi.cook.recipe.service.ICookRecipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 用户端菜谱接口：公开浏览、草稿保存、提交审核和互动。
 */
@Tag(name = "用户端-菜谱", description = "菜谱浏览、创建、编辑、提交审核、点赞、收藏和分享")
@RestController
@RequestMapping("/api/v1")
public class CookRecipeController
{
    @Autowired
    private ICookRecipeService recipeService;

    @Operation(summary = "菜谱列表", description = "分页查询已发布的公开菜谱，支持按分类、难度、关键词筛选")
    @GetMapping("/recipes")
    public R<PageVo<RecipeListItemVo>> listRecipes(
            @Parameter(description = "分类编码") @RequestParam(required = false) String categoryCode,
            @Parameter(description = "难度：easy/medium/hard") @RequestParam(required = false) String difficulty,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(recipeService.listPublicRecipes(categoryCode, difficulty, keyword, page, pageSize));
    }

    @Operation(summary = "菜谱详情", description = "公开菜谱所有人可见，未公开菜谱仅作者本人可见")
    @GetMapping("/recipes/{id}")
    public R<RecipeDetailVo> getRecipe(@Parameter(description = "菜谱ID") @PathVariable Long id)
    {
        return R.ok(recipeService.getRecipeDetail(id));
    }

    @Operation(summary = "创建菜谱", description = "创建后保存为草稿，需调用提交接口进入审核")
    @RequiresLogin
    @PostMapping("/recipes")
    public R<RecipeDetailVo> createRecipe(@Valid @RequestBody RecipeSaveRequest request)
    {
        return R.ok(recipeService.createRecipe(request));
    }

    @Operation(summary = "编辑菜谱", description = "已发布菜谱编辑后生成新版本，需重新审核")
    @RequiresLogin
    @PutMapping("/recipes/{id}")
    public R<RecipeDetailVo> updateRecipe(@Parameter(description = "菜谱ID") @PathVariable Long id,
            @Valid @RequestBody RecipeSaveRequest request)
    {
        return R.ok(recipeService.updateRecipe(id, request));
    }

    @Operation(summary = "提交菜谱审核", description = "将草稿或已驳回菜谱提交审核")
    @RequiresLogin
    @PostMapping("/recipes/{id}/submit")
    public R<RecipeDetailVo> submitRecipe(@Parameter(description = "菜谱ID") @PathVariable Long id)
    {
        return R.ok(recipeService.submitRecipe(id));
    }

    @Operation(summary = "撤回菜谱审核", description = "仅审核中的菜谱可以撤回")
    @RequiresLogin
    @PostMapping("/recipes/{id}/withdraw")
    public R<RecipeDetailVo> withdrawRecipe(@Parameter(description = "菜谱ID") @PathVariable Long id)
    {
        return R.ok(recipeService.withdrawRecipe(id));
    }

    @Operation(summary = "删除菜谱", description = "软删除自己的菜谱")
    @RequiresLogin
    @DeleteMapping("/recipes/{id}")
    public R<?> deleteRecipe(@Parameter(description = "菜谱ID") @PathVariable Long id)
    {
        recipeService.deleteRecipe(id);
        return R.ok();
    }

    @Operation(summary = "我的菜谱列表", description = "分页查询当前用户的菜谱，可按状态筛选")
    @RequiresLogin
    @GetMapping("/users/me/recipes")
    public R<PageVo<RecipeListItemVo>> listMyRecipes(
            @Parameter(description = "菜谱状态：draft/pending_review/published/rejected") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(recipeService.listMyRecipes(status, page, pageSize));
    }

    @Operation(summary = "点赞菜谱")
    @RequiresLogin
    @PostMapping("/recipes/{id}/like")
    public R<?> likeRecipe(@Parameter(description = "菜谱ID") @PathVariable Long id)
    {
        recipeService.likeRecipe(id);
        return R.ok();
    }

    @Operation(summary = "取消点赞菜谱")
    @RequiresLogin
    @DeleteMapping("/recipes/{id}/like")
    public R<?> unlikeRecipe(@Parameter(description = "菜谱ID") @PathVariable Long id)
    {
        recipeService.unlikeRecipe(id);
        return R.ok();
    }

    @Operation(summary = "收藏菜谱")
    @RequiresLogin
    @PostMapping("/recipes/{id}/favorite")
    public R<?> favoriteRecipe(@Parameter(description = "菜谱ID") @PathVariable Long id)
    {
        recipeService.favoriteRecipe(id);
        return R.ok();
    }

    @Operation(summary = "取消收藏菜谱")
    @RequiresLogin
    @DeleteMapping("/recipes/{id}/favorite")
    public R<?> unfavoriteRecipe(@Parameter(description = "菜谱ID") @PathVariable Long id)
    {
        recipeService.unfavoriteRecipe(id);
        return R.ok();
    }

    @Operation(summary = "分享菜谱", description = "记录分享行为，增加分享计数")
    @RequiresLogin
    @PostMapping("/recipes/{id}/share")
    public R<?> shareRecipe(@Parameter(description = "菜谱ID") @PathVariable Long id)
    {
        recipeService.shareRecipe(id);
        return R.ok();
    }
}
