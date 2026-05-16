package com.ruoyi.cook.operation.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.cook.operation.config.CookMediaProperties;
import com.ruoyi.cook.operation.domain.MediaAsset;
import com.ruoyi.cook.operation.mapper.OperationMapper;

/**
 * FFmpeg 单清晰度 HLS 转码服务。
 */
@Service
public class VideoTranscodeService
{
    private static final String STATUS_UPLOADED = "uploaded";
    private static final String STATUS_TRANSCODING = "transcoding";
    private static final String STATUS_READY = "ready";
    private static final String STATUS_FAILED = "failed";

    @Autowired
    private OperationMapper operationMapper;

    @Autowired
    private OssMediaStorageService ossMediaStorageService;

    @Autowired
    private CookMediaProperties mediaProperties;

    @Autowired
    @Qualifier("cookMediaTaskExecutor")
    private ThreadPoolTaskExecutor mediaTaskExecutor;

    public void enqueueMediaTranscode(Long mediaId)
    {
        if (mediaId == null)
        {
            throw new ServiceException("视频资源 ID 不能为空");
        }
        MediaAsset media = operationMapper.selectMediaById(mediaId);
        if (media == null)
        {
            throw new ServiceException("视频资源不存在");
        }
        if (!"video".equals(media.getFileType()))
        {
            throw new ServiceException("仅支持视频资源转码");
        }
        if (STATUS_READY.equals(media.getStatus()) || STATUS_TRANSCODING.equals(media.getStatus()))
        {
            return;
        }

        Map<String, Object> metadata = parseMetadata(media.getMetadataJson());
        metadata.put("transcodeStartedAt", LocalDateTime.now().toString());
        metadata.remove("errorMessage");
        operationMapper.updateMediaTranscodeStart(mediaId, STATUS_TRANSCODING, JSON.toJSONString(metadata));

        mediaTaskExecutor.execute(() -> transcodeMedia(mediaId));
    }

    private void transcodeMedia(Long mediaId)
    {
        Path workDir = Paths.get(mediaProperties.getTempDir(), String.valueOf(mediaId), UUID.randomUUID().toString());
        try
        {
            MediaAsset media = operationMapper.selectMediaById(mediaId);
            if (media == null)
            {
                return;
            }
            Map<String, Object> metadata = parseMetadata(media.getMetadataJson());
            Path inputFile = prepareInputFile(media, metadata, workDir);
            Path hlsDir = workDir.resolve("hls");
            Files.createDirectories(hlsDir);
            runFfmpeg(inputFile, hlsDir);

            String hlsPrefix = normalizePrefix(mediaProperties.getHlsPrefix()) + "/" + mediaId + "/";
            List<String> hlsFiles = uploadHlsFiles(hlsDir, hlsPrefix);
            String hlsKey = hlsPrefix + "index.m3u8";
            String hlsUrl = ossMediaStorageService.publicObjectUrl(hlsKey);

            metadata.put("hlsObjectPrefix", hlsPrefix);
            metadata.put("hlsFiles", hlsFiles);
            metadata.put("transcodeCompletedAt", LocalDateTime.now().toString());
            metadata.remove("errorMessage");
            operationMapper.updateMediaTranscodeSuccess(mediaId, STATUS_READY, hlsUrl, JSON.toJSONString(metadata));
        }
        catch (Exception ex)
        {
            markFailed(mediaId, ex.getMessage());
        }
        finally
        {
            deleteQuietly(workDir);
        }
    }

    private Path prepareInputFile(MediaAsset media, Map<String, Object> metadata, Path workDir) throws Exception
    {
        Files.createDirectories(workDir);
        Object localPath = metadata.get("localPath");
        if (localPath instanceof String value && StringUtils.hasText(value))
        {
            Path path = Paths.get(value);
            if (Files.exists(path))
            {
                return path;
            }
        }

        Object originalObjectKey = metadata.get("originalObjectKey");
        if (!(originalObjectKey instanceof String objectKey) || !StringUtils.hasText(objectKey))
        {
            throw new ServiceException("视频原始 OSS 对象不存在，无法转码");
        }
        Path inputFile = workDir.resolve("original" + extension(media.getOriginalName()));
        ossMediaStorageService.downloadObject(objectKey, inputFile);
        return inputFile;
    }

    private void runFfmpeg(Path inputFile, Path hlsDir) throws Exception
    {
        Path playlist = hlsDir.resolve("index.m3u8");
        Path segmentPattern = hlsDir.resolve("segment_%05d.ts");
        ProcessBuilder builder = new ProcessBuilder(
                mediaProperties.getFfmpegPath(),
                "-y",
                "-i", inputFile.toString(),
                "-c:v", "h264",
                "-preset", "veryfast",
                "-c:a", "aac",
                "-f", "hls",
                "-hls_time", String.valueOf(Math.max(2, mediaProperties.getHlsSegmentSeconds())),
                "-hls_playlist_type", "vod",
                "-hls_segment_filename", segmentPattern.toString(),
                playlist.toString());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        process.getInputStream().transferTo(output);
        int exitCode = process.waitFor();
        if (exitCode != 0)
        {
            String message = output.toString(StandardCharsets.UTF_8);
            throw new ServiceException("FFmpeg 转码失败：" + abbreviate(message, 500));
        }
    }

    private List<String> uploadHlsFiles(Path hlsDir, String hlsPrefix) throws Exception
    {
        try (Stream<Path> stream = Files.list(hlsDir))
        {
            return stream
                    .filter(Files::isRegularFile)
                    .sorted()
                    .map(path -> uploadHlsFile(path, hlsPrefix))
                    .toList();
        }
    }

    private String uploadHlsFile(Path path, String hlsPrefix)
    {
        String fileName = path.getFileName().toString();
        String objectKey = hlsPrefix + fileName;
        String contentType = fileName.endsWith(".m3u8") ? "application/vnd.apple.mpegurl" : "video/mp2t";
        ossMediaStorageService.uploadFile(objectKey, path, contentType);
        return objectKey;
    }

    private void markFailed(Long mediaId, String errorMessage)
    {
        MediaAsset media = operationMapper.selectMediaById(mediaId);
        Map<String, Object> metadata = media == null ? new HashMap<>() : parseMetadata(media.getMetadataJson());
        metadata.put("errorMessage", StringUtils.hasText(errorMessage) ? abbreviate(errorMessage, 500) : "视频转码失败");
        metadata.put("transcodeFailedAt", LocalDateTime.now().toString());
        operationMapper.updateMediaTranscodeFailure(mediaId, STATUS_FAILED, JSON.toJSONString(metadata));
    }

    private Map<String, Object> parseMetadata(String metadataJson)
    {
        if (!StringUtils.hasText(metadataJson))
        {
            return new HashMap<>();
        }
        return new HashMap<>(JSON.parseObject(metadataJson));
    }

    private String extension(String fileName)
    {
        if (!StringUtils.hasText(fileName) || !fileName.contains("."))
        {
            return ".mp4";
        }
        return fileName.substring(fileName.lastIndexOf('.'));
    }

    private String normalizePrefix(String prefix)
    {
        if (!StringUtils.hasText(prefix))
        {
            return "uploads/videos/hls";
        }
        return prefix.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private String abbreviate(String value, int maxLength)
    {
        if (value == null || value.length() <= maxLength)
        {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private void deleteQuietly(Path path)
    {
        if (path == null || !Files.exists(path))
        {
            return;
        }
        try (Stream<Path> stream = Files.walk(path))
        {
            stream.sorted(Comparator.reverseOrder()).forEach(item -> {
                try
                {
                    Files.deleteIfExists(item);
                }
                catch (Exception ignored)
                {
                }
            });
        }
        catch (Exception ignored)
        {
        }
    }
}
