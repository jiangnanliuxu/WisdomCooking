package com.ruoyi.cook.operation.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 初始化视频分片直传响应。
 */
@Data
public class VideoMultipartInitVo
{
    private String sessionId;
    private String objectKey;
    private Long partSize;
    private String checkpointKey;
    private LocalDateTime expiresAt;
    private OssStsTokenVo sts;
    private MediaAssetVo media;
    private String status;
}
