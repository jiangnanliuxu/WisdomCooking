package com.ruoyi.cook.operation.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cook.operation.domain.UserPenalty;
import com.ruoyi.cook.operation.vo.AdminDashboardSummaryVo;
import com.ruoyi.cook.operation.vo.AdminGroupVo;
import com.ruoyi.cook.operation.vo.AdminUserVo;
import com.ruoyi.cook.operation.vo.CategoryRatioVo;
import com.ruoyi.cook.operation.vo.DashboardTrendPointVo;
import com.ruoyi.cook.operation.vo.RecentOperationLogVo;

/**
 * 管理端统计与后台管理查询 Mapper。
 */
public interface OperationAdminMapper
{
    AdminDashboardSummaryVo selectDashboardSummary();

    List<DashboardTrendPointVo> selectUserGrowthTrend(@Param("days") Integer days);

    List<CategoryRatioVo> selectRecipeCategoryRatios();

    List<RecentOperationLogVo> selectRecentOperationLogs(@Param("limit") Integer limit);

    List<AdminUserVo> selectAdminUsers(@Param("status") String status, @Param("keyword") String keyword);

    AdminUserVo selectAdminUserDetail(Long id);

    int insertUserPenalty(UserPenalty penalty);

    List<AdminGroupVo> selectAdminGroups(@Param("status") String status, @Param("keyword") String keyword);

    AdminGroupVo selectAdminGroupDetail(Long id);

    int dissolveGroup(Long id);
}
