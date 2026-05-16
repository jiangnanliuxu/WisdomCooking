package com.ruoyi.cook.ai.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 食物识图展示对象。
 */
@Data
@Schema(description = "AI食物识图结果")
public class AiRecognitionVo
{
    private Long id;
    private Long userId;
    private String userNickname;
    private String userAvatarUrl;
    private Long modelId;
    private String modelName;
    private Long imageMediaId;
    private String imageUrl;
    private String status;
    private String recognizedName;
    private BigDecimal confidence;
    private Integer calories;
    private Map<String, Object> nutrition;
    private String suggestion;
    private Object candidates;
    private Integer latencyMs;
    private String errorMessage;
    private String disclaimer;
    private LocalDateTime createdAt;
}
