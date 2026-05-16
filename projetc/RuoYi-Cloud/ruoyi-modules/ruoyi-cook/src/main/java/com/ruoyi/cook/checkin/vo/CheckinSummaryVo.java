package com.ruoyi.cook.checkin.vo;

import java.time.LocalDate;
import lombok.Data;

/**
 * 打卡统计摘要。
 */
@Data
public class CheckinSummaryVo
{
    private Long totalCount;
    private Long monthCount;
    private Integer streakDays;
    private LocalDate latestCheckinDate;
}
