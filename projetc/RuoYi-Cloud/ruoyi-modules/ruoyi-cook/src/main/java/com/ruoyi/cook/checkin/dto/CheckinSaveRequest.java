package com.ruoyi.cook.checkin.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 打卡新增 / 编辑请求。
 */
@Data
public class CheckinSaveRequest
{
    private Long recipeId;

    @NotNull(message = "打卡日期不能为空")
    private LocalDate checkinDate;

    private String content;

    private List<Long> mediaIds;

    private Map<String, Object> source;
}
