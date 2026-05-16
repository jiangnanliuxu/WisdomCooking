package com.ruoyi.cook.operation.vo;

import java.util.List;
import lombok.Data;
import com.ruoyi.cook.recipe.domain.vo.CategoryGroupVo;
import com.ruoyi.cook.recipe.domain.vo.RecipeListItemVo;
import com.ruoyi.cook.user.domain.vo.UserPublicProfileVo;

/**
 * 首页聚合返回对象。
 */
@Data
public class HomeVo
{
    private List<BannerVo> banners;
    private List<CategoryGroupVo> categories;
    private List<RecipeListItemVo> recommendedRecipes;
    private List<UserPublicProfileVo> recommendedUsers;
    private List<String> hotKeywords;
}
