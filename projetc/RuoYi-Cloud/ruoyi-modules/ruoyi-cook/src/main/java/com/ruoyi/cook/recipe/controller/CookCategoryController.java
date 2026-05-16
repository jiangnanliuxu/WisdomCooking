package com.ruoyi.cook.recipe.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.cook.recipe.domain.dto.CategorySaveRequest;
import com.ruoyi.cook.recipe.domain.vo.CategoryGroupVo;
import com.ruoyi.cook.recipe.domain.vo.CategoryVo;
import com.ruoyi.cook.recipe.service.ICookCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 菜谱分类接口。
 */
@Tag(name = "菜谱分类", description = "用户端分类入口与管理端分类维护")
@RestController
public class CookCategoryController
{
    @Autowired
    private ICookCategoryService categoryService;

    @Operation(summary = "分类分组列表", description = "返回启用且未删除的菜谱分类分组，无需登录")
    @GetMapping("/api/v1/categories")
    public R<List<CategoryGroupVo>> listCategories()
    {
        return R.ok(categoryService.listEnabledCategoryGroups());
    }

    @Operation(summary = "管理端分类列表", description = "返回全部未删除分类，含禁用项和只读标记")
    @RequiresPermissions("cook:recipe:query")
    @GetMapping("/api/admin/v1/categories")
    public R<List<CategoryGroupVo>> listAdminCategories()
    {
        return R.ok(categoryService.listAdminCategoryGroups());
    }

    @Operation(summary = "新增菜谱分类", description = "新增分类默认归入更多分类，编码由后端自动生成")
    @RequiresPermissions("cook:recipe:edit")
    @PostMapping("/api/admin/v1/categories")
    public R<CategoryVo> createCategory(@Valid @RequestBody CategorySaveRequest request)
    {
        return R.ok(categoryService.createCategory(request));
    }

    @Operation(summary = "编辑菜谱分类", description = "编辑普通分类，社区广场固定菜系不允许修改")
    @RequiresPermissions("cook:recipe:edit")
    @PutMapping("/api/admin/v1/categories/{id}")
    public R<CategoryVo> updateCategory(@PathVariable Long id, @Valid @RequestBody CategorySaveRequest request)
    {
        return R.ok(categoryService.updateCategory(id, request));
    }

    @Operation(summary = "删除菜谱分类", description = "软删除普通分类，社区广场固定菜系不允许删除")
    @RequiresPermissions("cook:recipe:remove")
    @DeleteMapping("/api/admin/v1/categories/{id}")
    public R<?> deleteCategory(@PathVariable Long id)
    {
        categoryService.deleteCategory(id);
        return R.ok();
    }
}
