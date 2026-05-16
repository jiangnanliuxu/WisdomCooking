package com.ruoyi.cook.recipe.mapper;

import com.ruoyi.cook.recipe.domain.AdminOperationLog;

/**
 * 后台操作日志 Mapper，用于记录菜谱审核、上下架等关键动作。
 */
public interface AdminOperationLogMapper
{
    int insertLog(AdminOperationLog log);
}
