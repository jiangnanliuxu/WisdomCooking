package com.ruoyi.cook.recipe.service.impl;

import static com.ruoyi.cook.recipe.domain.RecipeConstants.CHINESE_CATEGORY_CODES;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.cook.recipe.domain.RecipeCategory;
import com.ruoyi.cook.recipe.domain.dto.CategorySaveRequest;
import com.ruoyi.cook.recipe.domain.vo.CategoryCountVo;
import com.ruoyi.cook.recipe.domain.vo.CategoryGroupVo;
import com.ruoyi.cook.recipe.domain.vo.CategoryVo;
import com.ruoyi.cook.recipe.mapper.CategoryMapper;
import com.ruoyi.cook.recipe.mapper.RecipeMapper;
import com.ruoyi.cook.recipe.service.ICookCategoryService;

/**
 * 菜谱分类服务实现。
 */
@Service
public class CookCategoryServiceImpl implements ICookCategoryService
{
    private static final String STATUS_ENABLED = "enabled";
    private static final String STATUS_DISABLED = "disabled";
    private static final String GROUP_MORE = "more";
    private static final Map<String, String> GROUP_NAMES = new LinkedHashMap<>();

    static
    {
        GROUP_NAMES.put("chinese", "中华菜系");
        GROUP_NAMES.put("scene", "场景分类");
        GROUP_NAMES.put("more", "更多分类");
    }

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private RecipeMapper recipeMapper;

    @Override
    public List<CategoryGroupVo> listEnabledCategoryGroups()
    {
        return toGroups(categoryMapper.selectList(STATUS_ENABLED));
    }

    @Override
    public List<CategoryGroupVo> listAdminCategoryGroups()
    {
        return toGroups(categoryMapper.selectList(null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryVo createCategory(CategorySaveRequest request)
    {
        RecipeCategory category = new RecipeCategory();
        category.setCategoryCode(generateCategoryCode());
        category.setGroupCode(GROUP_MORE);
        fillCategory(category, request);
        categoryMapper.insertCategory(category);
        return toVo(category, countMap().getOrDefault(category.getCategoryCode(), 0L));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryVo updateCategory(Long id, CategorySaveRequest request)
    {
        RecipeCategory category = loadCategory(id);
        assertEditable(category);
        fillCategory(category, request);
        categoryMapper.updateCategory(category);
        return toVo(loadCategory(id), countMap().getOrDefault(category.getCategoryCode(), 0L));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id)
    {
        RecipeCategory category = loadCategory(id);
        assertEditable(category);
        if (categoryMapper.softDeleteById(id) <= 0)
        {
            throw new ServiceException("分类不存在");
        }
    }

    @Override
    public void assertEnabledCategory(String categoryCode)
    {
        if (StringUtils.isBlank(categoryCode) || categoryMapper.countEnabledByCode(categoryCode) <= 0)
        {
            throw new ServiceException("不支持的菜谱分类");
        }
    }

    private List<CategoryGroupVo> toGroups(List<RecipeCategory> categories)
    {
        Map<String, Long> counts = countMap();
        Map<String, List<CategoryVo>> grouped = categories.stream()
                .map(category -> toVo(category, counts.getOrDefault(category.getCategoryCode(), 0L)))
                .collect(Collectors.groupingBy(CategoryVo::getGroupCode, LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream()
                .map(entry -> new CategoryGroupVo(entry.getKey(), groupName(entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
    }

    private Map<String, Long> countMap()
    {
        return recipeMapper.selectCategoryCounts().stream()
                .collect(Collectors.toMap(CategoryCountVo::getCategoryCode, CategoryCountVo::getRecipeCount,
                        (left, right) -> left));
    }

    private CategoryVo toVo(RecipeCategory category, Long recipeCount)
    {
        CategoryVo vo = new CategoryVo();
        vo.setId(category.getId());
        vo.setCode(category.getCategoryCode());
        vo.setName(category.getName());
        vo.setIcon(category.getIcon());
        vo.setColor(category.getColor());
        vo.setDescription(category.getDescription());
        vo.setGroupCode(category.getGroupCode());
        vo.setSortNo(category.getSortNo());
        vo.setStatus(category.getStatus());
        vo.setReadonly(isProtected(category.getCategoryCode()));
        vo.setRecipeCount(recipeCount);
        return vo;
    }

    private void fillCategory(RecipeCategory category, CategorySaveRequest request)
    {
        category.setName(request.getName().trim());
        category.setIcon(trimToNull(request.getIcon()));
        category.setColor(trimToNull(request.getColor()));
        category.setDescription(trimToNull(request.getDescription()));
        category.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        category.setStatus(defaultStatus(request.getStatus()));
    }

    private String generateCategoryCode()
    {
        String code;
        do
        {
            code = "cat_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        }
        while (categoryMapper.countByCode(code) > 0);
        return code;
    }

    private RecipeCategory loadCategory(Long id)
    {
        RecipeCategory category = categoryMapper.selectById(id);
        if (category == null)
        {
            throw new ServiceException("分类不存在");
        }
        return category;
    }

    private void assertEditable(RecipeCategory category)
    {
        if (isProtected(category.getCategoryCode()))
        {
            throw new ServiceException("社区广场预选菜系不允许修改或删除");
        }
    }

    private boolean isProtected(String categoryCode)
    {
        return CHINESE_CATEGORY_CODES.contains(categoryCode);
    }

    private String defaultStatus(String status)
    {
        if (StringUtils.isBlank(status))
        {
            return STATUS_ENABLED;
        }
        if (!STATUS_ENABLED.equals(status) && !STATUS_DISABLED.equals(status))
        {
            throw new ServiceException("不支持的分类状态");
        }
        return status;
    }

    private String groupName(String groupCode)
    {
        return GROUP_NAMES.getOrDefault(groupCode, groupCode);
    }

    private String trimToNull(String value)
    {
        return StringUtils.isBlank(value) ? null : value.trim();
    }
}
