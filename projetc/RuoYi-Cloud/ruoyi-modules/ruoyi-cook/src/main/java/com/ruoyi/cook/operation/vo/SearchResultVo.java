package com.ruoyi.cook.operation.vo;

import java.util.List;
import lombok.Data;
import com.ruoyi.cook.common.domain.vo.PageVo;
import com.ruoyi.cook.recipe.domain.vo.RecipeListItemVo;
import com.ruoyi.cook.user.domain.vo.UserPublicProfileVo;

/**
 * 搜索聚合结果。
 */
@Data
public class SearchResultVo
{
    private String keyword;
    private PageVo<RecipeListItemVo> recipes;
    private List<UserPublicProfileVo> users;
    private List<String> hotKeywords;
}
