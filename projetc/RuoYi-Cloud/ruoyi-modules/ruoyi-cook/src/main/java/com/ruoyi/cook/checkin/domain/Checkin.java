package com.ruoyi.cook.checkin.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 打卡记录实体。
 */
@Data
public class Checkin
{
    private Long id;
    private Long userId;
    private Long recipeId;
    private Long generatedPostId;
    private LocalDate checkinDate;
    private String content;
    private String mediaIdsJson;
    private String sourceJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
