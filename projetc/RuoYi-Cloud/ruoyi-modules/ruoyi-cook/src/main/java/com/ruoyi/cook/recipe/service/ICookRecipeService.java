package com.ruoyi.cook.recipe.service;

import java.util.List;
import com.ruoyi.cook.common.domain.vo.PageVo;
import com.ruoyi.cook.recipe.domain.dto.CommentCreateRequest;
import com.ruoyi.cook.recipe.domain.dto.RecipeSaveRequest;
import com.ruoyi.cook.recipe.domain.vo.CommentVo;
import com.ruoyi.cook.recipe.domain.vo.RecipeDetailVo;
import com.ruoyi.cook.recipe.domain.vo.RecipeListItemVo;

/**
 * 菜谱主链路服务接口。
 */
public interface ICookRecipeService
{
    PageVo<RecipeListItemVo> listPublicRecipes(String categoryCode, String difficulty, String keyword, Integer page,
            Integer pageSize);

    RecipeDetailVo getRecipeDetail(Long id);

    RecipeDetailVo createRecipe(RecipeSaveRequest request);

    RecipeDetailVo updateRecipe(Long id, RecipeSaveRequest request);

    RecipeDetailVo submitRecipe(Long id);

    RecipeDetailVo withdrawRecipe(Long id);

    void deleteRecipe(Long id);

    PageVo<RecipeListItemVo> listMyRecipes(String status, Integer page, Integer pageSize);

    void likeRecipe(Long id);

    void unlikeRecipe(Long id);

    void favoriteRecipe(Long id);

    void unfavoriteRecipe(Long id);

    void shareRecipe(Long id);

    PageVo<CommentVo> listComments(String targetType, Long targetId, Integer page, Integer pageSize);

    CommentVo createComment(CommentCreateRequest request);

    void deleteComment(Long id);

    void likeComment(Long id);

    void unlikeComment(Long id);

    PageVo<RecipeListItemVo> listAdminRecipes(String reviewStatus, String publishStatus, String keyword, Integer page,
            Integer pageSize);

    PageVo<RecipeListItemVo> listRecipeAudits(String status, String keyword, Integer page, Integer pageSize);

    RecipeDetailVo getAdminRecipeDetail(Long id);

    void adminDeleteRecipe(Long id);

    RecipeDetailVo approveRecipe(Long id);

    RecipeDetailVo rejectRecipe(Long id, String reason);

    RecipeDetailVo onlineRecipe(Long id);

    RecipeDetailVo offlineRecipe(Long id);

    RecipeDetailVo triggerVideoTranscode(Long id);
}
