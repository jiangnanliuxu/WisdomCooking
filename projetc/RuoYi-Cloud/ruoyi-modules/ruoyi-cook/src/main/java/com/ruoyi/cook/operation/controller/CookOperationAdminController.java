package com.ruoyi.cook.operation.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.cook.common.domain.vo.PageVo;
import com.ruoyi.cook.operation.dto.BannerMoveRequest;
import com.ruoyi.cook.operation.dto.BannerSaveRequest;
import com.ruoyi.cook.operation.dto.FeedbackHandleRequest;
import com.ruoyi.cook.operation.dto.ReportHandleRequest;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 管理端运营与治理接口。
 */
@Tag(name = "管理端-运营治理", description = "仪表盘、轮播图、反馈、举报、用户管理和群组管理")
@RestController
@RequestMapping("/api/admin/v1")
public class CookOperationAdminController
{
    @Autowired
    private ICookOperationAdminService operationAdminService;

    @Operation(summary = "顶部统计指标", description = "返回管理端仪表盘顶部卡片所需的聚合统计值")
    @RequiresPermissions("cook:dashboard:summary")
    @GetMapping("/dashboard/summary")
    public R<AdminDashboardSummaryVo> dashboardSummary()
    {
        return R.ok(operationAdminService.getDashboardSummary());
    }

    @Operation(summary = "近 7 天用户增长", description = "按天聚合用户注册数，用于仪表盘趋势图")
    @RequiresPermissions("cook:dashboard:summary")
    @GetMapping("/dashboard/user-growth")
    public R<List<DashboardTrendPointVo>> userGrowth()
    {
        return R.ok(operationAdminService.listUserGrowthTrend());
    }

    @Operation(summary = "菜谱分类占比", description = "按分类统计菜谱数量，用于饼图和排名展示")
    @RequiresPermissions("cook:dashboard:summary")
    @GetMapping("/dashboard/recipe-category-ratio")
    public R<List<CategoryRatioVo>> recipeCategoryRatio()
    {
        return R.ok(operationAdminService.listRecipeCategoryRatios());
    }

    @Operation(summary = "最近操作", description = "返回后台最近关键操作日志")
    @RequiresPermissions("cook:dashboard:summary")
    @GetMapping("/operation-logs/recent")
    public R<List<RecentOperationLogVo>> recentLogs(
            @Parameter(description = "返回数量，默认 10") @RequestParam(required = false) Integer limit)
    {
        return R.ok(operationAdminService.listRecentOperationLogs(limit));
    }

    @Operation(summary = "轮播图列表", description = "管理端分页查询轮播图，支持状态和关键词筛选")
    @RequiresPermissions("cook:banner:list")
    @GetMapping("/banners")
    public R<PageVo<BannerVo>> listBanners(
            @Parameter(description = "状态：online/offline") @RequestParam(required = false) String status,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(operationAdminService.listBanners(status, keyword, page, pageSize));
    }

    @Operation(summary = "新增轮播图", description = "创建新的首页轮播图配置")
    @RequiresPermissions("cook:banner:add")
    @PostMapping("/banners")
    public R<BannerVo> createBanner(@Valid @RequestBody BannerSaveRequest request)
    {
        return R.ok(operationAdminService.createBanner(request));
    }

    @Operation(summary = "编辑轮播图", description = "修改轮播图文案、图片、跳转和有效期")
    @RequiresPermissions("cook:banner:edit")
    @PutMapping("/banners/{id}")
    public R<BannerVo> updateBanner(@PathVariable Long id, @Valid @RequestBody BannerSaveRequest request)
    {
        return R.ok(operationAdminService.updateBanner(id, request));
    }

    @Operation(summary = "删除轮播图", description = "软删除轮播图记录")
    @RequiresPermissions("cook:banner:remove")
    @DeleteMapping("/banners/{id}")
    public R<?> deleteBanner(@PathVariable Long id)
    {
        operationAdminService.deleteBanner(id);
        return R.ok();
    }

    @Operation(summary = "轮播图上架", description = "将轮播图设置为线上可见")
    @RequiresPermissions("cook:banner:edit")
    @PostMapping("/banners/{id}/online")
    public R<?> onlineBanner(@PathVariable Long id)
    {
        operationAdminService.onlineBanner(id);
        return R.ok();
    }

    @Operation(summary = "轮播图下架", description = "将轮播图设置为后台保留但前台不可见")
    @RequiresPermissions("cook:banner:edit")
    @PostMapping("/banners/{id}/offline")
    public R<?> offlineBanner(@PathVariable Long id)
    {
        operationAdminService.offlineBanner(id);
        return R.ok();
    }

    @Operation(summary = "调整轮播图排序", description = "按指定 sortNo 更新轮播图排序值")
    @RequiresPermissions("cook:banner:edit")
    @PostMapping("/banners/{id}/move")
    public R<?> moveBanner(@PathVariable Long id, @Valid @RequestBody BannerMoveRequest request)
    {
        operationAdminService.moveBanner(id, request.getSortNo());
        return R.ok();
    }

    @Operation(summary = "媒体资源列表", description = "分页返回后台可选资源，当前用于轮播图选图")
    @RequiresPermissions("cook:banner:list")
    @GetMapping("/media-assets")
    public R<PageVo<MediaAssetVo>> listMediaAssets(
            @Parameter(description = "文件类型，默认 image") @RequestParam(required = false) String fileType,
            @Parameter(description = "原始文件名关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(operationAdminService.listMediaAssets(fileType, keyword, page, pageSize));
    }

    @Operation(summary = "媒体资源详情", description = "根据资源 ID 查询后台可选资源详情")
    @RequiresPermissions("cook:banner:list")
    @GetMapping("/media-assets/{id}")
    public R<MediaAssetVo> getMediaAsset(@PathVariable Long id)
    {
        return R.ok(operationAdminService.getMediaAsset(id));
    }

    @Operation(summary = "后台上传图片资源", description = "为轮播图等后台内容上传图片并返回可选资源")
    @RequiresPermissions("cook:banner:edit")
    @PostMapping(value = "/media-assets/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<MediaAssetVo> uploadImage(@RequestPart("file") MultipartFile file)
    {
        return R.ok(operationAdminService.uploadImage(file));
    }

    @Operation(summary = "反馈列表", description = "管理端分页查询用户反馈")
    @RequiresPermissions("cook:feedback:list")
    @GetMapping("/feedbacks")
    public R<PageVo<FeedbackVo>> listFeedbacks(
            @Parameter(description = "反馈状态") @RequestParam(required = false) String status,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(operationAdminService.listFeedbacks(status, keyword, page, pageSize));
    }

    @Operation(summary = "反馈详情", description = "查看反馈内容、联系方式和回复情况")
    @RequiresPermissions("cook:feedback:query")
    @GetMapping("/feedbacks/{id}")
    public R<FeedbackVo> getFeedback(@PathVariable Long id)
    {
        return R.ok(operationAdminService.getFeedbackDetail(id));
    }

    @Operation(summary = "处理反馈", description = "回复反馈并更新状态，例如 processing/resolved/closed")
    @RequiresPermissions("cook:feedback:edit")
    @PutMapping("/feedbacks/{id}")
    public R<FeedbackVo> handleFeedback(@PathVariable Long id, @Valid @RequestBody FeedbackHandleRequest request)
    {
        return R.ok(operationAdminService.handleFeedback(id, request));
    }

    @Operation(summary = "举报列表", description = "管理端分页查询用户提交的举报")
    @RequiresPermissions("cook:report:list")
    @GetMapping("/reports")
    public R<PageVo<ReportVo>> listReports(
            @Parameter(description = "处理状态") @RequestParam(required = false) String status,
            @Parameter(description = "举报对象类型") @RequestParam(required = false) String targetType,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(operationAdminService.listReports(status, targetType, page, pageSize));
    }

    @Operation(summary = "举报详情", description = "查看举报原因、处理结果和举报对象信息")
    @RequiresPermissions("cook:report:query")
    @GetMapping("/reports/{id}")
    public R<ReportVo> getReport(@PathVariable Long id)
    {
        return R.ok(operationAdminService.getReportDetail(id));
    }

    @Operation(summary = "处理举报", description = "更新举报状态并记录处理结论")
    @RequiresPermissions("cook:report:edit")
    @PutMapping("/reports/{id}")
    public R<ReportVo> handleReport(@PathVariable Long id, @Valid @RequestBody ReportHandleRequest request)
    {
        return R.ok(operationAdminService.handleReport(id, request));
    }

    @Operation(summary = "用户列表", description = "管理端分页查询普通用户，支持状态和关键词筛选")
    @RequiresPermissions("cook:user:list")
    @GetMapping("/users")
    public R<PageVo<AdminUserVo>> listUsers(
            @Parameter(description = "用户状态") @RequestParam(required = false) String status,
            @Parameter(description = "手机号或昵称关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(operationAdminService.listUsers(status, keyword, page, pageSize));
    }

    @Operation(summary = "用户详情", description = "查看用户资料与内容统计")
    @RequiresPermissions("cook:user:query")
    @GetMapping("/users/{id}")
    public R<AdminUserVo> getUser(@PathVariable Long id)
    {
        return R.ok(operationAdminService.getUserDetail(id));
    }

    @Operation(summary = "禁言用户", description = "将用户状态改为 muted，并写入处罚记录")
    @RequiresPermissions("cook:user:edit")
    @PostMapping("/users/{id}/mute")
    public R<?> muteUser(@PathVariable Long id,
            @Parameter(description = "禁言原因") @RequestParam(required = false) String reason)
    {
        operationAdminService.muteUser(id, reason);
        return R.ok();
    }

    @Operation(summary = "封禁用户", description = "将用户状态改为 banned，并写入处罚记录")
    @RequiresPermissions("cook:user:edit")
    @PostMapping("/users/{id}/ban")
    public R<?> banUser(@PathVariable Long id,
            @Parameter(description = "封禁原因") @RequestParam(required = false) String reason)
    {
        operationAdminService.banUser(id, reason);
        return R.ok();
    }

    @Operation(summary = "解除处罚", description = "将用户状态恢复为 normal，并写入处罚记录")
    @RequiresPermissions("cook:user:edit")
    @PostMapping("/users/{id}/unblock")
    public R<?> unblockUser(@PathVariable Long id,
            @Parameter(description = "解除原因") @RequestParam(required = false) String reason)
    {
        operationAdminService.unblockUser(id, reason);
        return R.ok();
    }

    @Operation(summary = "群组列表", description = "管理端分页查询群组，支持状态和关键词筛选")
    @RequiresPermissions("cook:group:list")
    @GetMapping("/groups")
    public R<PageVo<AdminGroupVo>> listGroups(
            @Parameter(description = "群组状态") @RequestParam(required = false) String status,
            @Parameter(description = "群名或群主昵称关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(operationAdminService.listGroups(status, keyword, page, pageSize));
    }

    @Operation(summary = "群组详情", description = "查看群组信息、成员数量和消息数量")
    @RequiresPermissions("cook:group:query")
    @GetMapping("/groups/{id}")
    public R<AdminGroupVo> getGroup(@PathVariable Long id)
    {
        return R.ok(operationAdminService.getGroupDetail(id));
    }

    @Operation(summary = "解散群组", description = "将群组状态置为 dissolved，并记录操作日志")
    @RequiresPermissions("cook:group:edit")
    @PostMapping("/groups/{id}/dissolve")
    public R<?> dissolveGroup(@PathVariable Long id,
            @Parameter(description = "解散原因") @RequestParam(required = false) String reason)
    {
        operationAdminService.dissolveGroup(id, reason);
        return R.ok();
    }
}
