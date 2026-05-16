package com.ruoyi.cook.checkin.mapper;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cook.checkin.domain.Checkin;
import com.ruoyi.cook.checkin.vo.CheckinVo;

/**
 * 打卡 Mapper。
 */
public interface CheckinMapper
{
    Checkin selectById(Long id);

    Checkin selectByIdForUpdate(Long id);

    List<CheckinVo> selectByDate(@Param("userId") Long userId, @Param("checkinDate") LocalDate checkinDate);

    List<CheckinVo> selectByRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<LocalDate> selectRecentDates(Long userId);

    Long countByUser(@Param("userId") Long userId);

    Long countByUserAndRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    LocalDate selectLatestCheckinDate(Long userId);

    int insertCheckin(Checkin checkin);

    int updateCheckin(Checkin checkin);

    int softDelete(@Param("id") Long id, @Param("userId") Long userId);

    int updateGeneratedPostId(@Param("id") Long id, @Param("generatedPostId") Long generatedPostId);
}
