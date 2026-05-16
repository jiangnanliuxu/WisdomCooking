package com.ruoyi.cook.user.service;

import com.ruoyi.cook.user.domain.dto.UpdateInterestsRequest;
import com.ruoyi.cook.user.domain.dto.UpdateProfileRequest;
import com.ruoyi.cook.user.domain.vo.UserProfileVo;

/**
 * 用户资料服务接口。
 * 提供当前用户资料查询、更新和兴趣标签管理。
 */
public interface ICookUserService
{
    UserProfileVo getCurrentUser();

    UserProfileVo updateCurrentUser(UpdateProfileRequest request);

    UserProfileVo updateInterestTags(UpdateInterestsRequest request);
}

