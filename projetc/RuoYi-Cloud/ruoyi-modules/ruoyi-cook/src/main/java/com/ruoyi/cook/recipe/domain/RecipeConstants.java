package com.ruoyi.cook.recipe.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 菜谱模块常量，集中维护状态码、互动类型和社区固定菜系。
 */
public final class RecipeConstants
{
    public static final String TARGET_RECIPE = "recipe";
    public static final String TARGET_POST = "post";
    public static final String TARGET_COMMENT = "comment";
    public static final String TARGET_USER = "user";

    public static final String ACTION_LIKE = "like";
    public static final String ACTION_FAVORITE = "favorite";
    public static final String ACTION_SHARE = "share";
    public static final String ACTION_FOLLOW = "follow";

    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_PENDING_REVIEW = "pending_review";
    public static final String STATUS_PUBLISHED = "published";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_WITHDRAWN = "withdrawn";
    public static final String STATUS_NORMAL = "normal";
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_INACTIVE = "inactive";

    public static final String PUBLISH_ONLINE = "online";
    public static final String PUBLISH_OFFLINE = "offline";

    public static final String BIZ_RECIPE = "recipe";
    public static final String BIZ_POST = "post";

    public static final List<String> CHINESE_CATEGORY_CODES = List.of(
            "sichuan", "cantonese", "shandong", "jiangsu", "zhejiang", "hunan", "fujian", "anhui");

    public static final List<String> CHINESE_CATEGORY_NAMES = List.of(
            "川菜", "粤菜", "鲁菜", "苏菜", "浙菜", "湘菜", "闽菜", "徽菜");

    public static List<String> categoryCodesByNames(List<String> names)
    {
        if (names == null || names.isEmpty())
        {
            return List.of();
        }
        Set<String> normalizedNames = new LinkedHashSet<>();
        for (String name : names)
        {
            if (name == null || name.isBlank())
            {
                continue;
            }
            normalizedNames.add(name.trim());
        }
        if (normalizedNames.isEmpty())
        {
            return List.of();
        }

        List<String> codes = new ArrayList<>();
        for (int i = 0; i < CHINESE_CATEGORY_CODES.size(); i++)
        {
            String code = CHINESE_CATEGORY_CODES.get(i);
            String name = CHINESE_CATEGORY_NAMES.get(i);
            if (normalizedNames.contains(name) || normalizedNames.contains(code))
            {
                codes.add(code);
            }
        }
        return codes;
    }

    private RecipeConstants()
    {
    }
}
