package com.ruoyi.cook.operation.service.impl;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.cook.auth.domain.CookUser;
import com.ruoyi.cook.auth.mapper.CookUserMapper;
import com.ruoyi.cook.common.domain.vo.PageVo;
import com.ruoyi.cook.operation.domain.Feedback;
import com.ruoyi.cook.operation.domain.MediaUploadSession;
import com.ruoyi.cook.operation.domain.Report;
import com.ruoyi.cook.operation.dto.FeedbackCreateRequest;
import com.ruoyi.cook.operation.dto.ReportCreateRequest;
import com.ruoyi.cook.operation.dto.VideoMultipartCompleteRequest;
import com.ruoyi.cook.operation.dto.VideoMultipartInitRequest;
import com.ruoyi.cook.operation.mapper.OperationMapper;
import com.ruoyi.cook.operation.config.CookMediaProperties;
import com.ruoyi.cook.operation.service.ICookOperationService;
import com.ruoyi.cook.operation.service.OssMediaStorageService;
import com.ruoyi.cook.operation.service.VideoTranscodeService;
import com.ruoyi.cook.operation.vo.BannerVo;
import com.ruoyi.cook.operation.vo.FeedbackVo;
import com.ruoyi.cook.operation.vo.HomeVo;
import com.ruoyi.cook.operation.vo.MediaAssetVo;
import com.ruoyi.cook.operation.vo.ReportVo;
import com.ruoyi.cook.operation.vo.SearchResultVo;
import com.ruoyi.cook.operation.vo.VideoMultipartInitVo;
import com.ruoyi.cook.operation.vo.VideoMultipartSessionVo;
import com.ruoyi.cook.recipe.domain.RecipeConstants;
import com.ruoyi.cook.recipe.domain.vo.RecipeListItemVo;
import com.ruoyi.cook.recipe.mapper.RecipeMapper;
import com.ruoyi.cook.recipe.service.ICookCategoryService;
import com.ruoyi.cook.user.domain.vo.UserPublicProfileVo;
import com.ruoyi.cook.user.service.ICookUserExtraService;

/**
 * 用户端运营服务实现。
 * <p>
 * 这里负责首页聚合、搜索、轮播点击、反馈、举报等能力。
 * 首版以“先把主要业务跑通”为目标，搜索历史和热门词使用 Redis 轻量实现。
 * </p>
 */
@Service
public class CookOperationServiceImpl implements ICookOperationService
{
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_RECIPE_LIMIT = 6;
    private static final int DEFAULT_USER_LIMIT = 6;
    private static final int DEFAULT_HOT_LIMIT = 8;
    private static final String SEARCH_HISTORY_KEY_PREFIX = "cook:search:history:";
    private static final String SEARCH_HOT_KEY = "cook:search:hot";
    private static final Path UPLOAD_BASE_DIR = Paths.get(System.getProperty("user.home"), ".cook-uploads");
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_UPLOADED = "uploaded";
    private static final String STATUS_CANCELED = "canceled";
    private static final long MB = 1024L * 1024L;
    private static final long MAX_IMAGE_SIZE = 20L * MB;
    private static final long MAX_VIDEO_SIZE = 500L * MB;

    @Autowired
    private OperationMapper operationMapper;

    @Autowired
    private RecipeMapper recipeMapper;

    @Autowired
    private ICookCategoryService categoryService;

    @Autowired
    private CookUserMapper userMapper;

    @Autowired
    private ICookUserExtraService userExtraService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CookMediaProperties mediaProperties;

    @Autowired
    private OssMediaStorageService ossMediaStorageService;

    @Autowired
    private VideoTranscodeService videoTranscodeService;

    @Override
    public HomeVo getHomeData(Integer recipeLimit, Integer userLimit)
    {
        HomeVo home = new HomeVo();
        home.setBanners(listActiveBanners());
        home.setCategories(categoryService.listEnabledCategoryGroups());
        home.setRecommendedRecipes(listRecommendedRecipes(recipeLimit));
        home.setRecommendedUsers(userExtraService.listRecommendedUsers(userLimit == null ? DEFAULT_USER_LIMIT : userLimit));
        home.setHotKeywords(listHotKeywords(DEFAULT_HOT_LIMIT));
        return home;
    }

    @Override
    public List<BannerVo> listActiveBanners()
    {
        return operationMapper.selectActiveBanners(LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordBannerClick(Long id)
    {
        if (operationMapper.increaseBannerClick(id) <= 0)
        {
            throw new ServiceException("轮播图不存在");
        }
    }

    @Override
    public SearchResultVo search(String keyword, Integer page, Integer pageSize)
    {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isEmpty())
        {
            throw new ServiceException("搜索关键词不能为空");
        }

        // 搜索一旦发生，就同步更新热门词和当前用户历史。
        rememberSearchKeyword(normalizedKeyword);
        PageVo<RecipeListItemVo> recipePage = listRecipesByKeyword(normalizedKeyword, page, pageSize);
        List<UserPublicProfileVo> users = userExtraService.listRecommendedUsers(20).stream()
                .filter(user -> user.getNickname() != null && user.getNickname().contains(normalizedKeyword))
                .collect(Collectors.toList());

        SearchResultVo result = new SearchResultVo();
        result.setKeyword(normalizedKeyword);
        result.setRecipes(recipePage);
        result.setUsers(users);
        result.setHotKeywords(listHotKeywords(DEFAULT_HOT_LIMIT));
        return result;
    }

    @Override
    public List<String> listHotKeywords(Integer limit)
    {
        int safeLimit = limit == null || limit < 1 ? DEFAULT_HOT_LIMIT : Math.min(limit, 20);
        List<String> values = stringRedisTemplate.opsForZSet().reverseRange(SEARCH_HOT_KEY, 0, safeLimit - 1)
                .stream()
                .toList();
        if (!values.isEmpty())
        {
            return values;
        }
        // Redis 暂无热词时，用固定分类和部分推荐词兜底，保证前端首页有内容。
        return List.of("家常菜", "减脂餐", "早餐", "川菜", "烘焙", "夜宵", "便当", "甜品").subList(0, safeLimit);
    }

    @Override
    public List<String> listSearchHistory()
    {
        Long userId = SecurityUtils.getUserId();
        if (userId == null)
        {
            return Collections.emptyList();
        }
        List<String> history = stringRedisTemplate.opsForList().range(historyKey(userId), 0, 9);
        return history == null ? Collections.emptyList() : history;
    }

    @Override
    public void clearSearchHistory()
    {
        Long userId = SecurityUtils.getUserId();
        if (userId != null)
        {
            stringRedisTemplate.delete(historyKey(userId));
        }
    }

    @Override
    public PageVo<FeedbackVo> listMyFeedbacks(Integer page, Integer pageSize)
    {
        Long userId = requireUserId();
        startPage(page, pageSize);
        List<FeedbackVo> rows = operationMapper.selectMyFeedbacks(userId);
        return toPage(rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FeedbackVo createFeedback(FeedbackCreateRequest request)
    {
        Long userId = requireUserId();
        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setType(request.getType());
        feedback.setContent(request.getContent());
        feedback.setMediaIdsJson(JSON.toJSONString(request.getMediaIds()));
        feedback.setContact(request.getContact());
        feedback.setStatus("processing");
        operationMapper.insertFeedback(feedback);
        FeedbackVo detail = operationMapper.selectMyFeedbackDetail(feedback.getId(), userId);
        if (detail == null)
        {
            throw new ServiceException("反馈创建失败");
        }
        return detail;
    }

    @Override
    public PageVo<ReportVo> listMyReports(Integer page, Integer pageSize)
    {
        Long userId = requireUserId();
        startPage(page, pageSize);
        List<ReportVo> rows = operationMapper.selectMyReports(userId);
        return toPage(rows);
    }

    @Override
    public ReportVo getMyReportDetail(Long id)
    {
        Long userId = requireUserId();
        ReportVo report = operationMapper.selectMyReportDetail(id, userId);
        if (report == null)
        {
            throw new ServiceException("举报记录不存在");
        }
        return report;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportVo createReport(ReportCreateRequest request)
    {
        Long userId = requireUserId();
        Report report = new Report();
        report.setReporterId(userId);
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReasonType(request.getReasonType());
        report.setReason(request.getReason());
        report.setMediaIdsJson(JSON.toJSONString(request.getMediaIds()));
        report.setStatus("pending");
        operationMapper.insertReport(report);
        ReportVo detail = operationMapper.selectMyReportDetail(report.getId(), userId);
        if (detail == null)
        {
            throw new ServiceException("举报提交失败");
        }
        return detail;
    }

    @Override
    public MediaAssetVo getMediaAsset(Long mediaId)
    {
        MediaAssetVo media = operationMapper.selectMediaVoById(mediaId);
        if (media == null)
        {
            throw new ServiceException("资源不存在");
        }
        rewritePrivateHlsUrl(media);
        return media;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MediaAssetVo uploadImage(MultipartFile file)
    {
        return saveUploadedFile(file, "image");
    }

    private void rewritePrivateHlsUrl(MediaAssetVo media)
    {
        if (media != null && "video".equals(media.getFileType()) && "ready".equals(media.getStatus()))
        {
            media.setHlsUrl(hasHlsPlaylist(media.getId(), media.getMetadata()) ? proxyHlsUrl(media.getId()) : null);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MediaAssetVo uploadVideo(MultipartFile file)
    {
        return saveUploadedFile(file, "video");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MediaAssetVo uploadAudio(MultipartFile file)
    {
        return saveUploadedFile(file, "audio");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoMultipartInitVo initVideoMultipartUpload(VideoMultipartInitRequest request)
    {
        Long userId = requireUserId();
        assertVideoSize(request.getFileSize());

        MediaUploadSession session = operationMapper.selectReusableUploadSession(userId, request.getFingerprint(),
                request.getFileSize());
        if (session == null)
        {
            session = new MediaUploadSession();
            session.setSessionId(UUID.randomUUID().toString());
            session.setOwnerId(userId);
            session.setObjectKey(buildVideoObjectKey(userId, request.getFileName()));
            session.setOriginalName(request.getFileName());
            session.setContentType(StringUtils.hasText(request.getContentType()) ? request.getContentType()
                    : "video/mp4");
            session.setSizeBytes(request.getFileSize());
            session.setFingerprint(request.getFingerprint());
            session.setStatus(STATUS_PENDING);
            session.setExpiresAt(LocalDateTime.now().plusHours(24));
            operationMapper.insertUploadSession(session);
        }

        VideoMultipartInitVo vo = toInitVo(session);
        vo.setSts(ossMediaStorageService.assumeUploadRole(session.getObjectKey()));
        if (session.getMediaId() != null)
        {
            vo.setMedia(getMediaAsset(session.getMediaId()));
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MediaAssetVo completeVideoMultipartUpload(VideoMultipartCompleteRequest request)
    {
        Long userId = requireUserId();
        MediaUploadSession session = loadUploadSession(request.getSessionId(), userId);
        if (session.getMediaId() != null)
        {
            return getMediaAsset(session.getMediaId());
        }
        if (STATUS_CANCELED.equals(session.getStatus()))
        {
            throw new ServiceException("上传会话已取消");
        }
        if (StringUtils.hasText(request.getObjectKey()) && !request.getObjectKey().equals(session.getObjectKey()))
        {
            throw new ServiceException("上传对象与会话不一致");
        }

        long objectSize = ossMediaStorageService.objectSize(session.getObjectKey());
        if (session.getSizeBytes() != null && objectSize != session.getSizeBytes())
        {
            throw new ServiceException("OSS 文件大小与上传会话不一致");
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("storage", "oss");
        metadata.put("contentType", session.getContentType());
        metadata.put("originalObjectKey", session.getObjectKey());
        metadata.put("fingerprint", session.getFingerprint());
        metadata.put("checkpointKey", checkpointKey(session));

        com.ruoyi.cook.operation.domain.MediaAsset media = new com.ruoyi.cook.operation.domain.MediaAsset();
        media.setOwnerId(userId);
        media.setBizType("user_upload");
        media.setFileType("video");
        media.setOriginalName(session.getOriginalName());
        media.setUrl(ossMediaStorageService.publicObjectUrl(session.getObjectKey()));
        media.setStatus(STATUS_UPLOADED);
        media.setSizeBytes(objectSize);
        media.setMetadataJson(JSON.toJSONString(metadata));
        operationMapper.insertMedia(media);
        operationMapper.completeUploadSession(session.getSessionId(), userId, STATUS_UPLOADED, media.getId(), null);
        enqueueTranscodeAfterCommit(media.getId());
        return getMediaAsset(media.getId());
    }

    @Override
    public VideoMultipartSessionVo getVideoMultipartSession(String sessionId)
    {
        Long userId = requireUserId();
        return toSessionVo(loadUploadSession(sessionId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelVideoMultipartUpload(String sessionId)
    {
        Long userId = requireUserId();
        MediaUploadSession session = loadUploadSession(sessionId, userId);
        if (session.getMediaId() != null)
        {
            throw new ServiceException("视频资源已创建，不能取消上传会话");
        }
        operationMapper.cancelUploadSession(sessionId, userId);
    }

    @Override
    public String loadHlsPlaylist(Long mediaId)
    {
        com.ruoyi.cook.operation.domain.MediaAsset media = loadVideoMedia(mediaId);
        String hlsPrefix = resolveHlsPrefix(media);
        String playlistKey = hlsPrefix + "index.m3u8";
        if (!ossMediaStorageService.objectExists(playlistKey))
        {
            throw new ServiceException("HLS播放列表不存在，请使用原始视频地址播放");
        }
        String playlist = new String(ossMediaStorageService.readObjectBytes(playlistKey), StandardCharsets.UTF_8);
        return playlist.lines()
                .map(line -> rewritePlaylistLine(mediaId, line))
                .collect(Collectors.joining("\n")) + "\n";
    }

    @Override
    public byte[] loadHlsSegment(Long mediaId, String segmentName)
    {
        if (!StringUtils.hasText(segmentName) || segmentName.contains("/") || segmentName.contains(".."))
        {
            throw new ServiceException("非法 HLS 分片名称");
        }
        com.ruoyi.cook.operation.domain.MediaAsset media = loadVideoMedia(mediaId);
        String segmentKey = resolveHlsPrefix(media) + segmentName;
        if (!ossMediaStorageService.objectExists(segmentKey))
        {
            throw new ServiceException("HLS分片不存在，请使用原始视频地址播放");
        }
        return ossMediaStorageService.readObjectBytes(segmentKey);
    }

    @Override
    public Resource loadMediaContent(Long mediaId)
    {
        com.ruoyi.cook.operation.domain.MediaAsset media = operationMapper.selectMediaById(mediaId);
        if (media == null)
        {
            throw new ServiceException("资源不存在");
        }
        Map<String, Object> metadata = parseMetadata(media.getMetadataJson());
        Object localPath = metadata.get("localPath");
        if (!(localPath instanceof String pathValue) || pathValue.isBlank())
        {
            throw new ServiceException("资源文件不存在");
        }
        try
        {
            Resource resource = new UrlResource(Paths.get(pathValue).toUri());
            if (!resource.exists())
            {
                throw new ServiceException("资源文件不存在");
            }
            return resource;
        }
        catch (Exception ex)
        {
            throw new ServiceException("读取资源文件失败");
        }
    }

    private com.ruoyi.cook.operation.domain.MediaAsset loadVideoMedia(Long mediaId)
    {
        com.ruoyi.cook.operation.domain.MediaAsset media = operationMapper.selectMediaById(mediaId);
        if (media == null || !"video".equals(media.getFileType()))
        {
            throw new ServiceException("视频资源不存在");
        }
        if (!"ready".equals(media.getStatus()))
        {
            throw new ServiceException("视频尚未转码完成");
        }
        return media;
    }

    private String resolveHlsPrefix(com.ruoyi.cook.operation.domain.MediaAsset media)
    {
        Map<String, Object> metadata = parseMetadata(media.getMetadataJson());
        return resolveHlsPrefix(media.getId(), metadata);
    }

    private String resolveHlsPrefix(Long mediaId, Map<String, Object> metadata)
    {
        Object prefix = metadata == null ? null : metadata.get("hlsObjectPrefix");
        if (prefix instanceof String value && StringUtils.hasText(value))
        {
            return value.replaceAll("^/+", "").replaceAll("/+$", "") + "/";
        }
        String configuredPrefix = StringUtils.hasText(mediaProperties.getHlsPrefix()) ? mediaProperties.getHlsPrefix()
                : "uploads/videos/hls";
        return configuredPrefix.replaceAll("^/+", "").replaceAll("/+$", "") + "/" + mediaId + "/";
    }

    private boolean hasHlsPlaylist(Long mediaId, Map<String, Object> metadata)
    {
        try
        {
            return ossMediaStorageService.objectExists(resolveHlsPrefix(mediaId, metadata) + "index.m3u8");
        }
        catch (ServiceException ex)
        {
            return false;
        }
    }

    private String proxyHlsUrl(Long mediaId)
    {
        return "/api/v1/uploads/" + mediaId + "/hls/index.m3u8";
    }

    private String rewritePlaylistLine(Long mediaId, String line)
    {
        if (!StringUtils.hasText(line) || line.startsWith("#") || line.startsWith("http://")
                || line.startsWith("https://"))
        {
            return line;
        }
        return "/api/v1/uploads/" + mediaId + "/hls/" + line;
    }

    /**
     * 首页推荐菜谱直接复用现有 cook_recipes 查询，登录用户按八大菜系兴趣优先排序。
     */
    private List<RecipeListItemVo> listRecommendedRecipes(Integer limit)
    {
        int safeLimit = limit == null || limit < 1 ? DEFAULT_RECIPE_LIMIT : Math.min(limit, 20);
        PageHelper.startPage(1, safeLimit);
        return recipeMapper.selectPublicList(null, null, null, preferredCategoryCodes(SecurityUtils.getUserId()));
    }

    /**
     * 搜索菜谱时复用现有 cook_recipes 列表 SQL，避免重复维护筛选逻辑。
     */
    private PageVo<RecipeListItemVo> listRecipesByKeyword(String keyword, Integer page, Integer pageSize)
    {
        startPage(page, pageSize);
        List<RecipeListItemVo> rows = recipeMapper.selectPublicList(null, null, keyword, List.of());
        return toPage(rows);
    }

    private List<String> preferredCategoryCodes(Long userId)
    {
        if (userId == null)
        {
            return List.of();
        }
        CookUser user = userMapper.selectById(userId);
        if (user == null || !StringUtils.hasText(user.getInterestTagsJson()))
        {
            return List.of();
        }
        try
        {
            return RecipeConstants.categoryCodesByNames(JSON.parseArray(user.getInterestTagsJson(), String.class));
        }
        catch (Exception ex)
        {
            return List.of();
        }
    }

    /**
     * 将关键词写入 Redis 热词 ZSET，并维护用户最近搜索列表。
     */
    private void rememberSearchKeyword(String keyword)
    {
        stringRedisTemplate.opsForZSet().incrementScore(SEARCH_HOT_KEY, keyword, 1D);
        Long userId = SecurityUtils.getUserId();
        if (userId == null)
        {
            return;
        }
        String key = historyKey(userId);
        stringRedisTemplate.opsForList().remove(key, 0, keyword);
        stringRedisTemplate.opsForList().leftPush(key, keyword);
        stringRedisTemplate.opsForList().trim(key, 0, 9);
        stringRedisTemplate.expire(key, 180, TimeUnit.DAYS);
    }

    private String historyKey(Long userId)
    {
        return SEARCH_HISTORY_KEY_PREFIX + userId;
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

    private Long requireUserId()
    {
        Long userId = SecurityUtils.getUserId();
        if (userId == null)
        {
            throw new ServiceException("用户未登录");
        }
        return userId;
    }

    /**
     * 上传文件的落盘流程：
     * 1. 校验文件非空；
     * 2. 保存到本机目录；
     * 3. 写入 cook_media_assets；
     * 4. 回填一个可通过本服务下载的 raw 地址。
     */
    private MediaAssetVo saveUploadedFile(MultipartFile file, String fileType)
    {
        Long userId = requireUserId();
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("上传文件不能为空");
        }
        assertUploadSize(file, fileType);

        try
        {
            String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "upload.bin";
            String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : "";
            String relativeDir = LocalDateTime.now().toLocalDate().toString();
            Path targetDir = UPLOAD_BASE_DIR.resolve(relativeDir);
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(java.util.UUID.randomUUID() + extension);
            file.transferTo(targetFile);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("contentType", file.getContentType());
            metadata.put("localPath", targetFile.toAbsolutePath().toString());
            metadata.put("relativeDir", relativeDir);

            com.ruoyi.cook.operation.domain.MediaAsset media = new com.ruoyi.cook.operation.domain.MediaAsset();
            media.setOwnerId(userId);
            media.setBizType("user_upload");
            media.setFileType(fileType);
            media.setOriginalName(originalName);
            media.setUrl("");
            media.setStatus("ready");
            media.setSizeBytes(file.getSize());
            media.setMetadataJson(JSON.toJSONString(metadata));
            operationMapper.insertMedia(media);

            String rawUrl = "/api/v1/uploads/" + media.getId() + "/raw";
            operationMapper.updateMediaUrl(media.getId(), rawUrl);
            return getMediaAsset(media.getId());
        }
        catch (Exception ex)
        {
            throw new ServiceException("上传文件失败");
        }
    }

    private void assertUploadSize(MultipartFile file, String fileType)
    {
        long maxSize = "video".equals(fileType) ? MAX_VIDEO_SIZE : "image".equals(fileType) ? MAX_IMAGE_SIZE : 0L;
        if (maxSize > 0 && file.getSize() > maxSize)
        {
            String label = "video".equals(fileType) ? "视频" : "图片";
            long maxSizeMb = maxSize / MB;
            throw new ServiceException(label + "不能超过 " + maxSizeMb + "MB");
        }
    }

    private void assertVideoSize(Long fileSize)
    {
        if (fileSize == null || fileSize <= 0)
        {
            throw new ServiceException("视频文件大小必须大于 0");
        }
        if (fileSize > MAX_VIDEO_SIZE)
        {
            throw new ServiceException("视频不能超过 " + (MAX_VIDEO_SIZE / MB) + "MB");
        }
    }

    private MediaUploadSession loadUploadSession(String sessionId, Long userId)
    {
        MediaUploadSession session = operationMapper.selectUploadSessionById(sessionId, userId);
        if (session == null)
        {
            throw new ServiceException("上传会话不存在");
        }
        return session;
    }

    private VideoMultipartInitVo toInitVo(MediaUploadSession session)
    {
        VideoMultipartInitVo vo = new VideoMultipartInitVo();
        vo.setSessionId(session.getSessionId());
        vo.setObjectKey(session.getObjectKey());
        vo.setPartSize(mediaProperties.getPartSize());
        vo.setCheckpointKey(checkpointKey(session));
        vo.setExpiresAt(session.getExpiresAt());
        vo.setStatus(session.getStatus());
        return vo;
    }

    private VideoMultipartSessionVo toSessionVo(MediaUploadSession session)
    {
        VideoMultipartSessionVo vo = new VideoMultipartSessionVo();
        vo.setSessionId(session.getSessionId());
        vo.setObjectKey(session.getObjectKey());
        vo.setOriginalName(session.getOriginalName());
        vo.setSizeBytes(session.getSizeBytes());
        vo.setFingerprint(session.getFingerprint());
        vo.setStatus(session.getStatus());
        vo.setMediaId(session.getMediaId());
        vo.setErrorMessage(session.getErrorMessage());
        vo.setExpiresAt(session.getExpiresAt());
        vo.setCreatedAt(session.getCreatedAt());
        vo.setUpdatedAt(session.getUpdatedAt());
        if (session.getMediaId() != null)
        {
            vo.setMedia(getMediaAsset(session.getMediaId()));
        }
        return vo;
    }

    private String buildVideoObjectKey(Long userId, String fileName)
    {
        String prefix = StringUtils.hasText(mediaProperties.getUploadPrefix()) ? mediaProperties.getUploadPrefix()
                : "uploads/videos/original";
        String datePath = LocalDateTime.now().toLocalDate().toString().replace("-", "/");
        return prefix.replaceAll("^/+", "").replaceAll("/+$", "") + "/" + datePath + "/" + userId + "/"
                + UUID.randomUUID() + extension(fileName);
    }

    private String extension(String fileName)
    {
        if (!StringUtils.hasText(fileName) || !fileName.contains("."))
        {
            return ".mp4";
        }
        String extension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
        return extension.matches("\\.[a-z0-9]{1,10}") ? extension : ".mp4";
    }

    private String checkpointKey(MediaUploadSession session)
    {
        return session.getOwnerId() + ":" + session.getFingerprint() + ":" + session.getSizeBytes();
    }

    private void enqueueTranscodeAfterCommit(Long mediaId)
    {
        if (!TransactionSynchronizationManager.isSynchronizationActive())
        {
            videoTranscodeService.enqueueMediaTranscode(mediaId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
        {
            @Override
            public void afterCommit()
            {
                videoTranscodeService.enqueueMediaTranscode(mediaId);
            }
        });
    }

    private Map<String, Object> parseMetadata(String metadataJson)
    {
        if (metadataJson == null || metadataJson.isBlank())
        {
            return Collections.emptyMap();
        }
        return JSON.parseObject(metadataJson);
    }
}
