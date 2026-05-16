package com.ruoyi.cook.checkin.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 打卡记录视图对象。
 */
@Data
public class CheckinVo
{
    private Long id;
    private Long userId;
    private Long recipeId;
    private String recipeTitle;
    private Long generatedPostId;
    private LocalDate checkinDate;
    private String content;
    private List<Long> mediaIds;
    private Map<String, Object> source;
    private LocalDateTime createdAt;
}
