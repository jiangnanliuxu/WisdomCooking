package com.ruoyi.cook.operation.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.security.annotation.RequiresLogin;
import com.ruoyi.cook.common.domain.vo.PageVo;
import com.ruoyi.cook.operation.dto.FeedbackCreateRequest;
import com.ruoyi.cook.operation.dto.ReportCreateRequest;
import com.ruoyi.cook.operation.dto.VideoMultipartCompleteRequest;
import com.ruoyi.cook.operation.dto.VideoMultipartInitRequest;
import com.ruoyi.cook.operation.service.ICookOperationService;
import com.ruoyi.cook.operation.vo.BannerVo;
import com.ruoyi.cook.operation.vo.FeedbackVo;
import com.ruoyi.cook.operation.vo.HomeVo;
import com.ruoyi.cook.operation.vo.MediaAssetVo;
import com.ruoyi.cook.operation.vo.ReportVo;
import com.ruoyi.cook.operation.vo.SearchResultVo;
import com.ruoyi.cook.operation.vo.VideoMultipartInitVo;
import com.ruoyi.cook.operation.vo.VideoMultipartSessionVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 用户端运营相关接口。
 */
@Tag(name = "用户端-运营服务", description = "首页聚合、轮播、搜索、反馈、举报和资源查询")
@RestController
@RequestMapping("/api/v1")
public class CookOperationController
{
    @Autowired
    private ICookOperationService operationService;

    @Operation(summary = "首页聚合", description = "返回首页轮播、固定分类、推荐菜谱、推荐达人和热门搜索词")
    @GetMapping("/home")
    public R<HomeVo> home(
            @Parameter(description = "推荐菜谱数量，默认 6") @RequestParam(required = false) Integer recipeLimit,
            @Parameter(description = "推荐达人数量，默认 6") @RequestParam(required = false) Integer userLimit)
    {
        return R.ok(operationService.getHomeData(recipeLimit, userLimit));
    }

    @Operation(summary = "首页轮播图", description = "仅返回当前生效中的线上轮播图")
    @GetMapping("/banners")
    public R<List<BannerVo>> listBanners()
    {
        return R.ok(operationService.listActiveBanners());
    }

    @Operation(summary = "记录轮播点击", description = "用户点击轮播跳转前调用，用于统计点击量")
    @PostMapping("/banners/{id}/click")
    public R<?> clickBanner(@Parameter(description = "轮播图ID") @PathVariable Long id)
    {
        operationService.recordBannerClick(id);
        return R.ok();
    }

    @Operation(summary = "综合搜索", description = "同时搜索公开菜谱与达人，并记录热门词和个人历史")
    @GetMapping("/search")
    public R<SearchResultVo> search(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(operationService.search(keyword, page, pageSize));
    }

    @Operation(summary = "热门搜索", description = "返回 Redis 中聚合的高频搜索词")
    @GetMapping("/search/hot")
    public R<List<String>> hotKeywords(
            @Parameter(description = "返回数量，默认 8") @RequestParam(required = false) Integer limit)
    {
        return R.ok(operationService.listHotKeywords(limit));
    }

    @Operation(summary = "搜索历史", description = "返回当前登录用户最近搜索记录")
    @RequiresLogin
    @GetMapping("/search/history")
    public R<List<String>> searchHistory()
    {
        return R.ok(operationService.listSearchHistory());
    }

    @Operation(summary = "清空搜索历史", description = "清空当前登录用户最近搜索记录")
    @RequiresLogin
    @DeleteMapping("/search/history")
    public R<?> clearSearchHistory()
    {
        operationService.clearSearchHistory();
        return R.ok();
    }

    @Operation(summary = "我的反馈记录", description = "分页返回当前登录用户提交过的反馈")
    @RequiresLogin
    @GetMapping("/feedbacks")
    public R<PageVo<FeedbackVo>> listFeedbacks(
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(operationService.listMyFeedbacks(page, pageSize));
    }

    @Operation(summary = "提交反馈", description = "用户可提交功能问题、体验建议等反馈内容")
    @RequiresLogin
    @PostMapping("/feedbacks")
    public R<FeedbackVo> createFeedback(@Valid @RequestBody FeedbackCreateRequest request)
    {
        return R.ok(operationService.createFeedback(request));
    }

    @Operation(summary = "我的举报记录", description = "分页返回当前登录用户提交过的举报")
    @RequiresLogin
    @GetMapping("/reports")
    public R<PageVo<ReportVo>> listReports(
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(operationService.listMyReports(page, pageSize));
    }

    @Operation(summary = "举报详情", description = "查看当前登录用户某条举报记录详情")
    @RequiresLogin
    @GetMapping("/reports/{id}")
    public R<ReportVo> getReport(@Parameter(description = "举报ID") @PathVariable Long id)
    {
        return R.ok(operationService.getMyReportDetail(id));
    }

    @Operation(summary = "提交举报", description = "支持举报菜谱、动态、评论和用户")
    @RequiresLogin
    @PostMapping("/reports")
    public R<ReportVo> createReport(@Valid @RequestBody ReportCreateRequest request)
    {
        return R.ok(operationService.createReport(request));
    }

    @Operation(summary = "上传资源信息", description = "根据资源 ID 查询图片、视频或音频资源元数据")
    @GetMapping("/uploads/{mediaId}")
    public R<MediaAssetVo> getMedia(@Parameter(description = "资源ID") @PathVariable Long mediaId)
    {
        return R.ok(operationService.getMediaAsset(mediaId));
    }

    @Operation(summary = "上传图片", description = "上传用户端图片资源，返回资源元数据和可访问地址")
    @RequiresLogin
    @PostMapping(value = "/uploads/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<MediaAssetVo> uploadImage(@RequestPart("file") MultipartFile file)
    {
        return R.ok(operationService.uploadImage(file));
    }

    @Operation(summary = "上传视频", description = "上传用户端视频资源，返回资源元数据和可访问地址")
    @RequiresLogin
    @PostMapping(value = "/uploads/videos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<MediaAssetVo> uploadVideo(@RequestPart("file") MultipartFile file)
    {
        return R.ok(operationService.uploadVideo(file));
    }

    @Operation(summary = "初始化视频分片直传", description = "返回 OSS 临时凭证、对象 Key 和断点续传 checkpointKey")
    @RequiresLogin
    @PostMapping("/uploads/videos/multipart/init")
    public R<VideoMultipartInitVo> initVideoMultipartUpload(@Valid @RequestBody VideoMultipartInitRequest request)
    {
        return R.ok(operationService.initVideoMultipartUpload(request));
    }

    @Operation(summary = "完成视频分片直传", description = "前端完成 OSS multipartUpload 后调用，后端校验对象并创建资源记录")
    @RequiresLogin
    @PostMapping("/uploads/videos/multipart/complete")
    public R<MediaAssetVo> completeVideoMultipartUpload(@Valid @RequestBody VideoMultipartCompleteRequest request)
    {
        return R.ok(operationService.completeVideoMultipartUpload(request));
    }

    @Operation(summary = "查询视频分片上传会话", description = "用于断点续传和上传状态恢复")
    @RequiresLogin
    @GetMapping("/uploads/videos/multipart/{sessionId}")
    public R<VideoMultipartSessionVo> getVideoMultipartSession(@PathVariable String sessionId)
    {
        return R.ok(operationService.getVideoMultipartSession(sessionId));
    }

    @Operation(summary = "取消视频分片上传会话", description = "取消当前用户尚未完成的 OSS 直传业务会话")
    @RequiresLogin
    @PostMapping("/uploads/videos/multipart/{sessionId}/cancel")
    public R<?> cancelVideoMultipartUpload(@PathVariable String sessionId)
    {
        operationService.cancelVideoMultipartUpload(sessionId);
        return R.ok();
    }

    @Operation(summary = "上传语音", description = "上传用户端语音资源，返回资源元数据和可访问地址")
    @RequiresLogin
    @PostMapping(value = "/uploads/audios", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<MediaAssetVo> uploadAudio(@RequestPart("file") MultipartFile file)
    {
        return R.ok(operationService.uploadAudio(file));
    }

    @Operation(summary = "读取 HLS 播放列表", description = "私有 OSS Bucket 兼容：后端读取并重写 HLS playlist")
    @GetMapping(value = "/uploads/{mediaId}/hls/index.m3u8", produces = "application/vnd.apple.mpegurl")
    public ResponseEntity<byte[]> hlsPlaylist(@PathVariable Long mediaId)
    {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                .body(operationService.loadHlsPlaylist(mediaId).getBytes(StandardCharsets.UTF_8));
    }

    @Operation(summary = "读取 HLS 分片", description = "私有 OSS Bucket 兼容：后端转发 HLS ts 分片")
    @GetMapping(value = "/uploads/{mediaId}/hls/{segmentName:.+}", produces = "video/mp2t")
    public ResponseEntity<byte[]> hlsSegment(@PathVariable Long mediaId, @PathVariable String segmentName)
    {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp2t"))
                .body(operationService.loadHlsSegment(mediaId, segmentName));
    }

    @Operation(summary = "下载原始资源", description = "根据资源 ID 返回图片、视频或音频原始文件流")
    @GetMapping("/uploads/{mediaId}/raw")
    public ResponseEntity<Resource> rawMedia(@PathVariable Long mediaId)
    {
        MediaAssetVo media = operationService.getMediaAsset(mediaId);
        Resource resource = operationService.loadMediaContent(mediaId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + media.getOriginalName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
