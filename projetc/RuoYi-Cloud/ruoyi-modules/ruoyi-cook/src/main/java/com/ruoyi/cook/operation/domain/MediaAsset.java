package com.ruoyi.cook.operation.domain;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 上传资源实体。
 */
@Data
public class MediaAsset
{
    private Long id;
    private Long ownerId;
    private String bizType;
    private String fileType;
    private String originalName;
    private String url;
    private String hlsUrl;
    private String status;
    private Long sizeBytes;
    private String metadataJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
