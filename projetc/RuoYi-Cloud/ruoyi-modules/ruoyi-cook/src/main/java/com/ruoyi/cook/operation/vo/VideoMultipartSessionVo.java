package com.ruoyi.cook.operation.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 视频分片上传会话状态。
 */
@Data
public class VideoMultipartSessionVo
{
    private String sessionId;
    private String objectKey;
    private String originalName;
    private Long sizeBytes;
    private String fingerprint;
    private String status;
    private Long mediaId;
    private String errorMessage;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private MediaAssetVo media;
}
