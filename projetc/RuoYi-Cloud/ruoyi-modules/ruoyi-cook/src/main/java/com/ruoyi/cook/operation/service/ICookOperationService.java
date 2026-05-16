package com.ruoyi.cook.operation.service;

import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.cook.common.domain.vo.PageVo;
import com.ruoyi.cook.operation.dto.FeedbackCreateRequest;
import com.ruoyi.cook.operation.dto.ReportCreateRequest;
import com.ruoyi.cook.operation.dto.VideoMultipartCompleteRequest;
import com.ruoyi.cook.operation.dto.VideoMultipartInitRequest;
import com.ruoyi.cook.operation.vo.BannerVo;
import com.ruoyi.cook.operation.vo.FeedbackVo;
import com.ruoyi.cook.operation.vo.HomeVo;
import com.ruoyi.cook.operation.vo.MediaAssetVo;
import com.ruoyi.cook.operation.vo.ReportVo;
import com.ruoyi.cook.operation.vo.SearchResultVo;
import com.ruoyi.cook.operation.vo.VideoMultipartInitVo;
import com.ruoyi.cook.operation.vo.VideoMultipartSessionVo;

/**
 * 用户端运营服务。
 */
public interface ICookOperationService
{
    HomeVo getHomeData(Integer recipeLimit, Integer userLimit);

    List<BannerVo> listActiveBanners();

    void recordBannerClick(Long id);

    SearchResultVo search(String keyword, Integer page, Integer pageSize);

    List<String> listHotKeywords(Integer limit);

    List<String> listSearchHistory();

    void clearSearchHistory();

    PageVo<FeedbackVo> listMyFeedbacks(Integer page, Integer pageSize);

    FeedbackVo createFeedback(FeedbackCreateRequest request);

    PageVo<ReportVo> listMyReports(Integer page, Integer pageSize);

    ReportVo getMyReportDetail(Long id);

    ReportVo createReport(ReportCreateRequest request);

    MediaAssetVo getMediaAsset(Long mediaId);

    MediaAssetVo uploadImage(MultipartFile file);

    MediaAssetVo uploadVideo(MultipartFile file);

    MediaAssetVo uploadAudio(MultipartFile file);

    VideoMultipartInitVo initVideoMultipartUpload(VideoMultipartInitRequest request);

    MediaAssetVo completeVideoMultipartUpload(VideoMultipartCompleteRequest request);

    VideoMultipartSessionVo getVideoMultipartSession(String sessionId);

    void cancelVideoMultipartUpload(String sessionId);

    String loadHlsPlaylist(Long mediaId);

    byte[] loadHlsSegment(Long mediaId, String segmentName);

    Resource loadMediaContent(Long mediaId);
}
