package com.ruoyi.cook.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 食物识图请求。
 */
@Data
@Schema(description = "AI食物识图请求")
public class AiRecognizeFoodRequest
{
    @Schema(description = "媒体资源ID，已接入上传模块后可传")
    private Long imageMediaId;

    @NotBlank(message = "图片地址不能为空")
    @Size(max = 500, message = "图片地址不能超过500个字符")
    @Schema(description = "图片地址", example = "https://example.com/food.jpg")
    private String imageUrl;
}
