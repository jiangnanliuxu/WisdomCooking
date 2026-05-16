package com.ruoyi.cook.checkin.service.impl;

import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.cook.checkin.domain.Checkin;
import com.ruoyi.cook.checkin.dto.CheckinSaveRequest;
import com.ruoyi.cook.checkin.mapper.CheckinMapper;
import com.ruoyi.cook.checkin.service.ICookCheckinService;
import com.ruoyi.cook.checkin.vo.CheckinSummaryVo;
import com.ruoyi.cook.checkin.vo.CheckinVo;
import com.ruoyi.cook.community.domain.Post;
import com.ruoyi.cook.community.mapper.PostMapper;

/**
 * 打卡服务实现。
 * <p>
 * 打卡是用户自有内容，不走后台审核即可保存；
 * 但“打卡生成动态”会额外创建一条待审核社区动态。
 * </p>
 */
@Service
public class CookCheckinServiceImpl implements ICookCheckinService
{
    @Autowired
    private CheckinMapper checkinMapper;

    @Autowired
    private PostMapper postMapper;

    @Override
    public CheckinSummaryVo getSummary()
    {
        Long userId = requireUserId();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        CheckinSummaryVo summary = new CheckinSummaryVo();
        summary.setTotalCount(defaultLong(checkinMapper.countByUser(userId)));
        summary.setMonthCount(defaultLong(checkinMapper.countByUserAndRange(userId, monthStart, monthEnd)));
        summary.setLatestCheckinDate(checkinMapper.selectLatestCheckinDate(userId));
        summary.setStreakDays(calculateStreakDays(checkinMapper.selectRecentDates(userId)));
        return summary;
    }

    @Override
    public List<CheckinVo> listByMonth(LocalDate anyDayInMonth)
    {
        Long userId = requireUserId();
        LocalDate baseDay = anyDayInMonth == null ? LocalDate.now() : anyDayInMonth;
        LocalDate startDate = baseDay.withDayOfMonth(1);
        LocalDate endDate = baseDay.withDayOfMonth(baseDay.lengthOfMonth());
        return checkinMapper.selectByRange(userId, startDate, endDate);
    }

    @Override
    public List<CheckinVo> listByDate(LocalDate date)
    {
        Long userId = requireUserId();
        if (date == null)
        {
            throw new ServiceException("查询日期不能为空");
        }
        return checkinMapper.selectByDate(userId, date);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckinVo createCheckin(CheckinSaveRequest request)
    {
        Long userId = requireUserId();
        Checkin checkin = new Checkin();
        checkin.setUserId(userId);
        fillCheckin(checkin, request);
        checkinMapper.insertCheckin(checkin);
        return loadDetail(userId, checkin.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckinVo updateCheckin(Long id, CheckinSaveRequest request)
    {
        Long userId = requireUserId();
        Checkin checkin = loadOwnedCheckin(id, userId);
        if (checkin.getGeneratedPostId() != null)
        {
            // 已经生成过动态的打卡仍允许编辑原打卡内容，但不自动回写动态，避免审计链路混乱。
        }
        fillCheckin(checkin, request);
        checkinMapper.updateCheckin(checkin);
        return loadDetail(userId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCheckin(Long id)
    {
        Long userId = requireUserId();
        if (checkinMapper.softDelete(id, userId) <= 0)
        {
            throw new ServiceException("打卡不存在或无权删除");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckinVo generatePost(Long id)
    {
        Long userId = requireUserId();
        Checkin checkin = loadOwnedCheckinForUpdate(id, userId);
        if (checkin.getGeneratedPostId() != null)
        {
            return loadDetail(userId, id);
        }

        // 打卡生成动态时统一进入待审核状态，满足“所有动态先审后发”的约束。
        Post post = new Post();
        post.setUserId(userId);
        post.setRelatedRecipeId(checkin.getRecipeId());
        post.setContent(checkin.getContent() == null || checkin.getContent().isBlank() ? "今日饮食打卡" : checkin.getContent());
        post.setMediaIdsJson(checkin.getMediaIdsJson());
        post.setSourceType("checkin");
        post.setVisibility("public");
        post.setStatus("pending_review");
        postMapper.insertPost(post);

        checkinMapper.updateGeneratedPostId(id, post.getId());
        return loadDetail(userId, id);
    }

    /**
     * 连续天数算法按“去重后的最近日期序列”计算，
     * 从今天或昨天开始往前数，遇到断档即停止。
     */
    private int calculateStreakDays(List<LocalDate> dates)
    {
        if (dates == null || dates.isEmpty())
        {
            return 0;
        }
        LocalDate expected = LocalDate.now();
        if (!dates.get(0).equals(expected))
        {
            if (dates.get(0).equals(expected.minusDays(1)))
            {
                expected = expected.minusDays(1);
            }
            else
            {
                return 0;
            }
        }
        int streak = 0;
        for (LocalDate date : dates)
        {
            if (date.equals(expected))
            {
                streak++;
                expected = expected.minusDays(1);
            }
            else if (date.isBefore(expected))
            {
                break;
            }
        }
        return streak;
    }

    private void fillCheckin(Checkin checkin, CheckinSaveRequest request)
    {
        checkin.setRecipeId(request.getRecipeId());
        checkin.setCheckinDate(request.getCheckinDate());
        checkin.setContent(request.getContent());
        checkin.setMediaIdsJson(JSON.toJSONString(request.getMediaIds()));
        checkin.setSourceJson(JSON.toJSONString(request.getSource()));
    }

    private Checkin loadOwnedCheckin(Long id, Long userId)
    {
        Checkin checkin = checkinMapper.selectById(id);
        if (checkin == null || !userId.equals(checkin.getUserId()))
        {
            throw new ServiceException("打卡不存在或无权访问");
        }
        return checkin;
    }

    private Checkin loadOwnedCheckinForUpdate(Long id, Long userId)
    {
        Checkin checkin = checkinMapper.selectByIdForUpdate(id);
        if (checkin == null || !userId.equals(checkin.getUserId()))
        {
            throw new ServiceException("打卡不存在或无权访问");
        }
        return checkin;
    }

    private CheckinVo loadDetail(Long userId, Long id)
    {
        return checkinMapper.selectByDate(userId, loadOwnedCheckin(id, userId).getCheckinDate()).stream()
                .filter(item -> id.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new ServiceException("打卡不存在"));
    }

    private Long defaultLong(Long value)
    {
        return value == null ? 0L : value;
    }

    private Long requireUserId()
    {
        Long userId = SecurityUtils.getUserId();
        if (userId == null)
        {
            throw new ServiceException("用户未登录");
        }
        return userId;
    }
}
