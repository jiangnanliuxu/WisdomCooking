package com.ruoyi.cook.operation.vo;

import lombok.Data;

/**
 * 管理端仪表盘概览数据。
 */
@Data
public class AdminDashboardSummaryVo
{
    private Long totalUsers;
    private Long totalRecipes;
    private Long totalPosts;
    private Long pendingRecipeCount;
    private Long pendingPostCount;
    private Long processingFeedbackCount;
    private Long pendingReportCount;
    private Long onlineBannerCount;
}
