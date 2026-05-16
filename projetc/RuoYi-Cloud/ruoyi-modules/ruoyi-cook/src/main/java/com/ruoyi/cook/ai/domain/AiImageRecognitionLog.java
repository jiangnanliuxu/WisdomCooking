package com.ruoyi.cook.ai.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * AI 识图日志实体，对应 cook_ai_image_recognition_logs 表。
 * <p>
 * 识别结果同时保存常用字段和 JSON 原文：常用字段便于列表筛选，JSON 字段便于前端展示完整营养结构。
 * </p>
 */
@Data
public class AiImageRecognitionLog
{
    private Long id;
    private Long userId;
    private Long modelId;
    private Long imageMediaId;
    private String imageUrl;
    private String resultJson;
    private String nutritionJson;
    private String candidatesJson;
    private Integer responseTimeMs;
    private String errorMessage;
    private String status;
    private String recognizedName;
    private BigDecimal confidence;
    private Integer calories;
    private String suggestion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    /** 管理端列表展示用：用户昵称。 */
    private String userNickname;

    /** 管理端列表展示用：用户头像。 */
    private String userAvatarUrl;

    /** 管理端列表展示用：模型名称。 */
    private String modelName;
}
