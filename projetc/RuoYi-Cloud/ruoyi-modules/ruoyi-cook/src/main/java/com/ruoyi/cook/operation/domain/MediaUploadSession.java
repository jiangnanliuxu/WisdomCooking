package com.ruoyi.cook.operation.domain;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 浏览器直传 OSS 的业务上传会话。
 */
@Data
public class MediaUploadSession
{
    private String sessionId;
    private Long ownerId;
    private String objectKey;
    private String originalName;
    private String contentType;
    private Long sizeBytes;
    private String fingerprint;
    private String status;
    private Long mediaId;
    private String errorMessage;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
