package com.ruoyi.cook.operation.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cook.operation.domain.Banner;
import com.ruoyi.cook.operation.domain.Feedback;
import com.ruoyi.cook.operation.domain.MediaAsset;
import com.ruoyi.cook.operation.domain.MediaUploadSession;
import com.ruoyi.cook.operation.domain.Report;
import com.ruoyi.cook.operation.vo.BannerVo;
import com.ruoyi.cook.operation.vo.FeedbackVo;
import com.ruoyi.cook.operation.vo.MediaAssetVo;
import com.ruoyi.cook.operation.vo.ReportVo;

/**
 * 运营域通用 Mapper。
 */
public interface OperationMapper
{
    List<BannerVo> selectActiveBanners(@Param("now") LocalDateTime now);

    List<BannerVo> selectAdminBannerList(@Param("status") String status, @Param("keyword") String keyword);

    Banner selectBannerById(Long id);

    int insertBanner(Banner banner);

    int updateBanner(Banner banner);

    int softDeleteBanner(Long id);

    int updateBannerStatus(@Param("id") Long id, @Param("status") String status);

    int updateBannerSort(@Param("id") Long id, @Param("sortNo") Integer sortNo);

    int increaseBannerClick(Long id);

    List<FeedbackVo> selectMyFeedbacks(Long userId);

    FeedbackVo selectMyFeedbackDetail(@Param("id") Long id, @Param("userId") Long userId);

    int insertFeedback(Feedback feedback);

    List<FeedbackVo> selectAdminFeedbacks(@Param("status") String status, @Param("keyword") String keyword);

    FeedbackVo selectAdminFeedbackDetail(Long id);

    int updateFeedback(Feedback feedback);

    List<ReportVo> selectMyReports(Long userId);

    ReportVo selectMyReportDetail(@Param("id") Long id, @Param("userId") Long userId);

    int insertReport(Report report);

    List<ReportVo> selectAdminReports(@Param("status") String status, @Param("targetType") String targetType);

    ReportVo selectAdminReportDetail(Long id);

    int updateReport(Report report);

    List<MediaAssetVo> selectAdminMediaAssets(@Param("fileType") String fileType, @Param("keyword") String keyword);

    MediaAsset selectMediaById(Long id);

    MediaAssetVo selectMediaVoById(Long id);

    int insertMedia(MediaAsset mediaAsset);

    int updateMediaUrl(@Param("id") Long id, @Param("url") String url);

    int updateMediaAfterOssUpload(@Param("id") Long id, @Param("url") String url, @Param("status") String status,
            @Param("sizeBytes") Long sizeBytes, @Param("metadataJson") String metadataJson);

    int updateMediaTranscodeStart(@Param("id") Long id, @Param("status") String status,
            @Param("metadataJson") String metadataJson);

    int updateMediaTranscodeSuccess(@Param("id") Long id, @Param("status") String status,
            @Param("hlsUrl") String hlsUrl, @Param("metadataJson") String metadataJson);

    int updateMediaTranscodeFailure(@Param("id") Long id, @Param("status") String status,
            @Param("metadataJson") String metadataJson);

    MediaUploadSession selectReusableUploadSession(@Param("ownerId") Long ownerId,
            @Param("fingerprint") String fingerprint, @Param("sizeBytes") Long sizeBytes);

    MediaUploadSession selectUploadSessionById(@Param("sessionId") String sessionId, @Param("ownerId") Long ownerId);

    int insertUploadSession(MediaUploadSession session);

    int updateUploadSessionStatus(@Param("sessionId") String sessionId, @Param("ownerId") Long ownerId,
            @Param("status") String status, @Param("errorMessage") String errorMessage);

    int completeUploadSession(@Param("sessionId") String sessionId, @Param("ownerId") Long ownerId,
            @Param("status") String status, @Param("mediaId") Long mediaId, @Param("errorMessage") String errorMessage);

    int cancelUploadSession(@Param("sessionId") String sessionId, @Param("ownerId") Long ownerId);
}
