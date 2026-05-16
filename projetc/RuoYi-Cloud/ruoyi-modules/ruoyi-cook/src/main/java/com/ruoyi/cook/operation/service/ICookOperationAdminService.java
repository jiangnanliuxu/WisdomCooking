package com.ruoyi.cook.operation.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.cook.common.domain.vo.PageVo;
import com.ruoyi.cook.operation.dto.BannerSaveRequest;
import com.ruoyi.cook.operation.dto.FeedbackHandleRequest;
import com.ruoyi.cook.operation.dto.ReportHandleRequest;
import com.ruoyi.cook.operation.vo.AdminDashboardSummaryVo;
import com.ruoyi.cook.operation.vo.AdminGroupVo;
import com.ruoyi.cook.operation.vo.AdminUserVo;
import com.ruoyi.cook.operation.vo.BannerVo;
import com.ruoyi.cook.operation.vo.CategoryRatioVo;
import com.ruoyi.cook.operation.vo.DashboardTrendPointVo;
import com.ruoyi.cook.operation.vo.FeedbackVo;
import com.ruoyi.cook.operation.vo.MediaAssetVo;
import com.ruoyi.cook.operation.vo.RecentOperationLogVo;
import com.ruoyi.cook.operation.vo.ReportVo;

/**
 * 管理端运营服务。
 */
public interface ICookOperationAdminService
{
    AdminDashboardSummaryVo getDashboardSummary();

    List<DashboardTrendPointVo> listUserGrowthTrend();

    List<CategoryRatioVo> listRecipeCategoryRatios();

    List<RecentOperationLogVo> listRecentOperationLogs(Integer limit);

    PageVo<BannerVo> listBanners(String status, String keyword, Integer page, Integer pageSize);

    BannerVo createBanner(BannerSaveRequest request);

    BannerVo updateBanner(Long id, BannerSaveRequest request);

    void deleteBanner(Long id);

    void onlineBanner(Long id);

    void offlineBanner(Long id);

    void moveBanner(Long id, Integer sortNo);

    PageVo<MediaAssetVo> listMediaAssets(String fileType, String keyword, Integer page, Integer pageSize);

    MediaAssetVo getMediaAsset(Long id);

    MediaAssetVo uploadImage(MultipartFile file);

    PageVo<FeedbackVo> listFeedbacks(String status, String keyword, Integer page, Integer pageSize);

    FeedbackVo getFeedbackDetail(Long id);

    FeedbackVo handleFeedback(Long id, FeedbackHandleRequest request);

    PageVo<ReportVo> listReports(String status, String targetType, Integer page, Integer pageSize);

    ReportVo getReportDetail(Long id);

    ReportVo handleReport(Long id, ReportHandleRequest request);

    PageVo<AdminUserVo> listUsers(String status, String keyword, Integer page, Integer pageSize);

    AdminUserVo getUserDetail(Long id);

    void muteUser(Long id, String reason);

    void banUser(Long id, String reason);

    void unblockUser(Long id, String reason);

    PageVo<AdminGroupVo> listGroups(String status, String keyword, Integer page, Integer pageSize);

    AdminGroupVo getGroupDetail(Long id);

    void dissolveGroup(Long id, String reason);
}
