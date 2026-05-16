package com.ruoyi.cook.user.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresLogin;
import com.ruoyi.cook.user.domain.dto.UpdateInterestsRequest;
import com.ruoyi.cook.user.domain.dto.UpdateProfileRequest;
import com.ruoyi.cook.user.domain.vo.UserProfileVo;
import com.ruoyi.cook.user.service.ICookUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 用户端用户资料接口。
 * <p>
 * 提供当前用户资料的查询、更新和兴趣标签管理。
 * 所有接口均需登录后调用。
 * </p>
 */
@Tag(name = "用户端-用户资料", description = "查看和编辑个人资料、兴趣标签")
@RestController
@RequestMapping("/api/v1/users")
public class CookUserController
{
    @Autowired
    private ICookUserService userService;

    @Operation(summary = "获取当前用户资料", description = "返回当前登录用户的昵称、头像、性别、生日、地区、简介和兴趣标签")
    @RequiresLogin
    @GetMapping("/me")
    public R<UserProfileVo> getCurrentUser()
    {
        return R.ok(userService.getCurrentUser());
    }

    @Operation(summary = "编辑用户资料", description = "更新当前用户的昵称、头像、性别、生日、地区和个性签名")
    @RequiresLogin
    @PutMapping("/me")
    @Log(title = "编辑用户资料", businessType = BusinessType.UPDATE)
    public R<UserProfileVo> updateCurrentUser(@Valid @RequestBody UpdateProfileRequest request)
    {
        return R.ok(userService.updateCurrentUser(request));
    }

    @Operation(summary = "保存兴趣标签", description = "保存当前用户的兴趣标签，最多 5 个")
    @RequiresLogin
    @PutMapping("/me/interests")
    @Log(title = "保存兴趣标签", businessType = BusinessType.UPDATE)
    public R<UserProfileVo> updateInterestTags(@Valid @RequestBody UpdateInterestsRequest request)
    {
        return R.ok(userService.updateInterestTags(request));
    }
}
