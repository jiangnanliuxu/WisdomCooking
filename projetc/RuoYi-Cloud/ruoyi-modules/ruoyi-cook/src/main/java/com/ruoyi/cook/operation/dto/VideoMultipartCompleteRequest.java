package com.ruoyi.cook.operation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * OSS 分片上传完成后的业务确认请求。
 */
@Data
public class VideoMultipartCompleteRequest
{
    @NotBlank(message = "上传会话不能为空")
    private String sessionId;

    private String objectKey;
}
