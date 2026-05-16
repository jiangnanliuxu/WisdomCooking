package com.ruoyi.cook.operation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 初始化视频分片直传请求。
 */
@Data
public class VideoMultipartInitRequest
{
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小必须大于 0")
    private Long fileSize;

    private String contentType;

    @NotBlank(message = "文件指纹不能为空")
    private String fingerprint;
}
