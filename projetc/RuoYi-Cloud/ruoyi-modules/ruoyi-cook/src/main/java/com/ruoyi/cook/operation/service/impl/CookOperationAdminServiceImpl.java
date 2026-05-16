package com.ruoyi.cook.operation.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.alibaba.fastjson2.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.cook.auth.mapper.CookUserMapper;
import com.ruoyi.cook.common.domain.vo.PageVo;
import com.ruoyi.cook.operation.domain.Banner;
import com.ruoyi.cook.operation.domain.Feedback;
import com.ruoyi.cook.operation.domain.Report;
import com.ruoyi.cook.operation.domain.UserPenalty;
import com.ruoyi.cook.operation.dto.BannerSaveRequest;
import com.ruoyi.cook.operation.dto.FeedbackHandleRequest;
import com.ruoyi.cook.operation.dto.ReportHandleRequest;
import com.ruoyi.cook.operation.mapper.OperationAdminMapper;
import com.ruoyi.cook.operation.mapper.OperationMapper;
import com.ruoyi.cook.operation.service.ICookOperationService;
import com.ruoyi.cook.operation.service.ICookOperationAdminService;
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
import com.ruoyi.cook.recipe.domain.AdminOperationLog;
import com.ruoyi.cook.recipe.mapper.AdminOperationLogMapper;

/**
 * 管理端运营服务实现。
 * <p>
 * 这里统一处理仪表盘、轮播图、反馈、举报、用户处罚和群组治理。
 * 关键动作会写入 cook_admin_operation_logs，便于后台追溯。
 * </p>
 */
@Service
public class CookOperationAdminServiceImpl implements ICookOperationAdminService
{
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private OperationMapper operationMapper;

    @Autowired
    private ICookOperationService operationService;

    @Autowired
    private OperationAdminMapper operationAdminMapper;

    @Autowired
    private CookUserMapper userMapper;

    @Autowired
    private AdminOperationLogMapper adminOperationLogMapper;

    @Override
    public AdminDashboardSummaryVo getDashboardSummary()
    {
        return operationAdminMapper.selectDashboardSummary();
    }

    @Override
    public List<DashboardTrendPointVo> listUserGrowthTrend()
    {
        return operationAdminMapper.selectUserGrowthTrend(7);
    }

    @Override
    public List<CategoryRatioVo> listRecipeCategoryRatios()
    {
        return operationAdminMapper.selectRecipeCategoryRatios();
    }

    @Override
    public List<RecentOperationLogVo> listRecentOperationLogs(Integer limit)
    {
        int safeLimit = limit == null || limit < 1 ? 10 : Math.min(limit, 50);
        return operationAdminMapper.selectRecentOperationLogs(safeLimit);
    }

    @Override
    public PageVo<BannerVo> listBanners(String status, String keyword, Integer page, Integer pageSize)
    {
        startPage(page, pageSize);
        List<BannerVo> rows = operationMapper.selectAdminBannerList(status, keyword);
        return toPage(rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BannerVo createBanner(BannerSaveRequest request)
    {
        assertMediaExists(request.getImageMediaId());
        Banner banner = fillBanner(new Banner(), request);
        banner.setStatus(defaultStatus(request.getStatus()));
        operationMapper.insertBanner(banner);
        writeAdminLog("banner", banner.getId(), "create", null, banner, "新增轮播图");
        return findBannerVo(banner.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BannerVo updateBanner(Long id, BannerSaveRequest request)
    {
        Banner before = loadBanner(id);
        assertMediaExists(request.getImageMediaId());
        Banner after = fillBanner(before, request);
        operationMapper.updateBanner(after);
        writeAdminLog("banner", id, "update", before, after, "编辑轮播图");
        return findBannerVo(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBanner(Long id)
    {
        Banner before = loadBanner(id);
        if (operationMapper.softDeleteBanner(id) <= 0)
        {
            throw new ServiceException("轮播图不存在");
        }
        writeAdminLog("banner", id, "delete", before, null, "删除轮播图");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onlineBanner(Long id)
    {
        changeBannerStatus(id, "online", "轮播图上架");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offlineBanner(Long id)
    {
        changeBannerStatus(id, "offline", "轮播图下架");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveBanner(Long id, Integer sortNo)
    {
        Banner before = loadBanner(id);
        operationMapper.updateBannerSort(id, sortNo);
        Banner after = loadBanner(id);
        writeAdminLog("banner", id, "move", before, after, "调整轮播排序");
    }

    @Override
    public PageVo<MediaAssetVo> listMediaAssets(String fileType, String keyword, Integer page, Integer pageSize)
    {
        startPage(page, pageSize);
        List<MediaAssetVo> rows = operationMapper.selectAdminMediaAssets(fileType, keyword);
        return toPage(rows);
    }

    @Override
    public MediaAssetVo getMediaAsset(Long id)
    {
        return operationService.getMediaAsset(id);
    }

    @Override
    public MediaAssetVo uploadImage(MultipartFile file)
    {
        return operationService.uploadImage(file);
    }

    @Override
    public PageVo<FeedbackVo> listFeedbacks(String status, String keyword, Integer page, Integer pageSize)
    {
        startPage(page, pageSize);
        List<FeedbackVo> rows = operationMapper.selectAdminFeedbacks(status, keyword);
        return toPage(rows);
    }

    @Override
    public FeedbackVo getFeedbackDetail(Long id)
    {
        FeedbackVo detail = operationMapper.selectAdminFeedbackDetail(id);
        if (detail == null)
        {
            throw new ServiceException("反馈不存在");
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FeedbackVo handleFeedback(Long id, FeedbackHandleRequest request)
    {
        FeedbackVo before = getFeedbackDetail(id);
        Feedback feedback = new Feedback();
        feedback.setId(id);
        feedback.setStatus(request.getStatus());
        feedback.setReplyContent(request.getReplyContent());
        feedback.setRepliedAt(LocalDateTime.now());
        operationMapper.updateFeedback(feedback);
        FeedbackVo after = getFeedbackDetail(id);
        writeAdminLog("feedback", id, "handle", before, after, "处理用户反馈");
        return after;
    }

    @Override
    public PageVo<ReportVo> listReports(String status, String targetType, Integer page, Integer pageSize)
    {
        startPage(page, pageSize);
        List<ReportVo> rows = operationMapper.selectAdminReports(status, targetType);
        return toPage(rows);
    }

    @Override
    public ReportVo getReportDetail(Long id)
    {
        ReportVo detail = operationMapper.selectAdminReportDetail(id);
        if (detail == null)
        {
            throw new ServiceException("举报不存在");
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportVo handleReport(Long id, ReportHandleRequest request)
    {
        ReportVo before = getReportDetail(id);
        Report report = new Report();
        report.setId(id);
        report.setStatus(request.getStatus());
        report.setHandleResult(request.getHandleResult());
        report.setHandlerId(SecurityUtils.getUserId());
        report.setHandledAt(LocalDateTime.now());
        operationMapper.updateReport(report);
        ReportVo after = getReportDetail(id);
        writeAdminLog("report", id, "handle", before, after, "处理内容举报");
        return after;
    }

    @Override
    public PageVo<AdminUserVo> listUsers(String status, String keyword, Integer page, Integer pageSize)
    {
        startPage(page, pageSize);
        List<AdminUserVo> rows = operationAdminMapper.selectAdminUsers(status, keyword);
        return toPage(rows);
    }

    @Override
    public AdminUserVo getUserDetail(Long id)
    {
        AdminUserVo detail = operationAdminMapper.selectAdminUserDetail(id);
        if (detail == null)
        {
            throw new ServiceException("用户不存在");
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void muteUser(Long id, String reason)
    {
        changeUserStatus(id, "muted", "mute", reason == null ? "禁言用户" : reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void banUser(Long id, String reason)
    {
        changeUserStatus(id, "banned", "ban", reason == null ? "封禁用户" : reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unblockUser(Long id, String reason)
    {
        changeUserStatus(id, "normal", "unblock", reason == null ? "解除处罚" : reason);
    }

    @Override
    public PageVo<AdminGroupVo> listGroups(String status, String keyword, Integer page, Integer pageSize)
    {
        startPage(page, pageSize);
        List<AdminGroupVo> rows = operationAdminMapper.selectAdminGroups(status, keyword);
        return toPage(rows);
    }

    @Override
    public AdminGroupVo getGroupDetail(Long id)
    {
        AdminGroupVo detail = operationAdminMapper.selectAdminGroupDetail(id);
        if (detail == null)
        {
            throw new ServiceException("群组不存在");
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dissolveGroup(Long id, String reason)
    {
        AdminGroupVo before = getGroupDetail(id);
        operationAdminMapper.dissolveGroup(id);
        AdminGroupVo after = getGroupDetail(id);
        writeAdminLog("group", id, "dissolve", before, after, reason == null ? "解散群组" : reason);
    }

    /**
     * 统一处理轮播图上下架，保证日志写入逻辑一致。
     */
    private void changeBannerStatus(Long id, String status, String remark)
    {
        Banner before = loadBanner(id);
        operationMapper.updateBannerStatus(id, status);
        Banner after = loadBanner(id);
        writeAdminLog("banner", id, status, before, after, remark);
    }

    /**
     * 统一处理用户状态变更：改状态、写处罚记录、补审计日志。
     */
    private void changeUserStatus(Long id, String status, String action, String reason)
    {
        AdminUserVo before = getUserDetail(id);
        if (userMapper.updateStatus(id, status) <= 0)
        {
            throw new ServiceException("用户不存在");
        }
        UserPenalty penalty = new UserPenalty();
        penalty.setUserId(id);
        penalty.setPenaltyType(action);
        penalty.setReason(reason);
        penalty.setStartAt(LocalDateTime.now());
        penalty.setOperatorId(SecurityUtils.getUserId());
        operationAdminMapper.insertUserPenalty(penalty);
        AdminUserVo after = getUserDetail(id);
        writeAdminLog("user", id, action, before, after, reason);
    }

    private Banner fillBanner(Banner banner, BannerSaveRequest request)
    {
        banner.setTitle(request.getTitle());
        banner.setSubtitle(request.getSubtitle());
        banner.setImageMediaId(request.getImageMediaId());
        banner.setJumpType(request.getJumpType());
        banner.setJumpTarget(request.getJumpTarget());
        banner.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        banner.setStatus(defaultStatus(request.getStatus()));
        banner.setStartAt(request.getStartAt());
        banner.setEndAt(request.getEndAt());
        return banner;
    }

    private String defaultStatus(String status)
    {
        return status == null || status.isBlank() ? "offline" : status;
    }

    private Banner loadBanner(Long id)
    {
        Banner banner = operationMapper.selectBannerById(id);
        if (banner == null)
        {
            throw new ServiceException("轮播图不存在");
        }
        return banner;
    }

    private BannerVo findBannerVo(Long id)
    {
        return operationMapper.selectAdminBannerList(null, null).stream()
                .filter(item -> id.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new ServiceException("轮播图不存在"));
    }

    private void assertMediaExists(Long mediaId)
    {
        MediaAssetVo media = operationMapper.selectMediaVoById(mediaId);
        if (media == null)
        {
            throw new ServiceException("轮播图资源不存在");
        }
    }

    private void startPage(Integer page, Integer pageSize)
    {
        int safePage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int safePageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        PageHelper.startPage(safePage, safePageSize);
    }

    private <T> PageVo<T> toPage(List<T> rows)
    {
        PageInfo<T> pageInfo = PageInfo.of(rows);
        return new PageVo<>(pageInfo.getPageNum(), pageInfo.getPageSize(), pageInfo.getTotal(), rows);
    }

    /**
     * 审计日志统一入口。
     * before / after 都序列化保存，便于后续追查状态变化来源。
     */
    private void writeAdminLog(String bizType, Long bizId, String action, Object before, Object after, String remark)
    {
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminId(SecurityUtils.getUserId());
        log.setBizType(bizType);
        log.setBizId(bizId);
        log.setAction(action);
        log.setBeforeJson(before == null ? null : JSON.toJSONString(before));
        log.setAfterJson(after == null ? null : JSON.toJSONString(after));
        log.setRemark(remark);
        adminOperationLogMapper.insertLog(log);
    }
}
