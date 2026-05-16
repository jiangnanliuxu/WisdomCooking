package com.ruoyi.cook.checkin.controller;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.security.annotation.RequiresLogin;
import com.ruoyi.cook.checkin.dto.CheckinSaveRequest;
import com.ruoyi.cook.checkin.service.ICookCheckinService;
import com.ruoyi.cook.checkin.vo.CheckinSummaryVo;
import com.ruoyi.cook.checkin.vo.CheckinVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 用户端打卡接口。
 */
@Tag(name = "用户端-饮食打卡", description = "打卡统计、月历列表、按日期查看、新增编辑删除和生成动态")
@RestController
@RequestMapping("/api/v1/checkins")
@RequiresLogin
public class CookCheckinController
{
    @Autowired
    private ICookCheckinService checkinService;

    @Operation(summary = "打卡统计", description = "返回累计打卡数、本月打卡数、连续天数和最近一次打卡日期")
    @GetMapping("/summary")
    public R<CheckinSummaryVo> summary()
    {
        return R.ok(checkinService.getSummary());
    }

    @Operation(summary = "打卡月历列表", description = "按月份返回当前用户打卡记录，默认查询当月")
    @GetMapping
    public R<List<CheckinVo>> listByMonth(
            @Parameter(description = "月份内任意日期，格式 yyyy-MM-dd，默认今天")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date)
    {
        return R.ok(checkinService.listByMonth(date));
    }

    @Operation(summary = "按日期查看打卡", description = "点击月历某一天后，查看当天全部打卡记录")
    @GetMapping("/by-date")
    public R<List<CheckinVo>> listByDate(
            @Parameter(description = "查询日期，格式 yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date)
    {
        return R.ok(checkinService.listByDate(date));
    }

    @Operation(summary = "新增打卡", description = "新增一条饮食打卡记录")
    @PostMapping
    public R<CheckinVo> create(@Valid @RequestBody CheckinSaveRequest request)
    {
        return R.ok(checkinService.createCheckin(request));
    }

    @Operation(summary = "编辑打卡", description = "编辑自己已有的打卡记录")
    @PutMapping("/{id}")
    public R<CheckinVo> update(@PathVariable Long id, @Valid @RequestBody CheckinSaveRequest request)
    {
        return R.ok(checkinService.updateCheckin(id, request));
    }

    @Operation(summary = "删除打卡", description = "软删除自己的打卡记录")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id)
    {
        checkinService.deleteCheckin(id);
        return R.ok();
    }

    @Operation(summary = "打卡生成动态", description = "将当前打卡生成一条待审核的社区动态")
    @PostMapping("/{id}/post")
    public R<CheckinVo> generatePost(@PathVariable Long id)
    {
        return R.ok(checkinService.generatePost(id));
    }
}
