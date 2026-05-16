package com.ruoyi.cook.operation.vo;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

/**
 * 上传资源视图对象。
 */
@Data
public class MediaAssetVo
{
    private Long id;
    private Long ownerId;
    private String fileType;
    private String originalName;
    private String url;
    private String hlsUrl;
    private String status;
    private Long sizeBytes;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
}
