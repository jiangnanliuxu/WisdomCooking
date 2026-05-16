package com.ruoyi.cook.operation.config;

import java.nio.file.Paths;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

/**
 * OSS 视频上传与 FFmpeg HLS 转码配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "cook.media")
public class CookMediaProperties
{
    private String endpoint;
    private String region;
    private String bucket;
    private String publicBaseUrl;
    private String accessKeyId;
    private String accessKeySecret;
    private String stsRoleArn;
    private String uploadPrefix = "uploads/videos/original";
    private String hlsPrefix = "uploads/videos/hls";
    private long partSize = 5L * 1024L * 1024L;
    private int stsDurationSeconds = 3600;
    private String ffmpegPath = "ffmpeg";
    private String tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "cook-video-transcode").toString();
    private int hlsSegmentSeconds = 6;
    private int transcodeThreads = 2;
}
