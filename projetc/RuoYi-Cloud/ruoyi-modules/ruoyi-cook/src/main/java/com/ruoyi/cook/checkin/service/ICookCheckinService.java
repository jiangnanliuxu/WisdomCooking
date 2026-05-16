package com.ruoyi.cook.checkin.service;

import java.time.LocalDate;
import java.util.List;
import com.ruoyi.cook.checkin.dto.CheckinSaveRequest;
import com.ruoyi.cook.checkin.vo.CheckinSummaryVo;
import com.ruoyi.cook.checkin.vo.CheckinVo;

/**
 * 打卡服务。
 */
public interface ICookCheckinService
{
    CheckinSummaryVo getSummary();

    List<CheckinVo> listByMonth(LocalDate anyDayInMonth);

    List<CheckinVo> listByDate(LocalDate date);

    CheckinVo createCheckin(CheckinSaveRequest request);

    CheckinVo updateCheckin(Long id, CheckinSaveRequest request);

    void deleteCheckin(Long id);

    CheckinVo generatePost(Long id);
}
