package com.ruoyi.cook.user.service.impl;

import com.alibaba.fastjson2.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.cook.auth.domain.CookUser;
import com.ruoyi.cook.auth.mapper.CookUserMapper;
import com.ruoyi.cook.user.convert.UserProfileConvert;
import com.ruoyi.cook.user.domain.dto.UpdateInterestsRequest;
import com.ruoyi.cook.user.domain.dto.UpdateProfileRequest;
import com.ruoyi.cook.user.domain.vo.UserProfileVo;
import com.ruoyi.cook.user.service.ICookUserService;

/**
 * 用户资料服务实现。
 * <p>
 * 通过 SecurityUtils 从请求上下文获取当前登录用户 ID，
 * 完成资料查询、更新和兴趣标签编辑。
 * </p>
 */
@Service
public class CookUserServiceImpl implements ICookUserService
{
    @Autowired
    private CookUserMapper userMapper;

    @Autowired
    private UserProfileConvert userProfileConvert;

    /**
     * 获取当前用户资料。
     * 从 SecurityUtils 获取用户 ID → 查库 → 转换为 VO 返回。
     */
    @Override
    public UserProfileVo getCurrentUser()
    {
        return userProfileConvert.toVo(loadCurrentUser());
    }

    /**
     * 更新当前用户资料。
     * 允许编辑的字段：昵称、头像、性别、生日、地区、简介。
     * 手机号不可编辑（后续如有需要单独走换绑流程）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVo updateCurrentUser(UpdateProfileRequest request)
    {
        CookUser user = loadCurrentUser();
        user.setNickname(request.getNickname());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setGender(request.getGender());
        user.setBirthday(request.getBirthday());
        user.setRegion(request.getRegion());
        user.setBio(request.getBio());
        userMapper.updateProfile(user);
        return userProfileConvert.toVo(userMapper.selectById(user.getId()));
    }

    /**
     * 保存兴趣标签。
     * 标签以 JSON 数组形式存入 {@code cook_users.interest_tags_json} 字段，
     * 覆盖写入（非追加）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVo updateInterestTags(UpdateInterestsRequest request)
    {
        CookUser user = loadCurrentUser();
        String interestTagsJson = JSON.toJSONString(request.getInterestTags());
        userMapper.updateInterestTags(user.getId(), interestTagsJson);
        return userProfileConvert.toVo(userMapper.selectById(user.getId()));
    }

    /**
     * 从 SecurityUtils 获取当前登录用户 ID 并加载用户信息。
     * 用户不存在或已注销时抛出异常。
     */
    private CookUser loadCurrentUser()
    {
        Long userId = SecurityUtils.getUserId();
        if (userId == null)
        {
            throw new ServiceException("用户未登录");
        }
        CookUser user = userMapper.selectById(userId);
        if (user == null || user.getDeletedAt() != null)
        {
            throw new ServiceException("用户不存在");
        }
        return user;
    }
}
