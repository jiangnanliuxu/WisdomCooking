package com.ruoyi.cook.user.service;

import java.util.List;
import com.ruoyi.cook.common.domain.vo.PageVo;
import com.ruoyi.cook.user.domain.vo.UserInteractionVo;
import com.ruoyi.cook.user.domain.vo.UserPublicProfileVo;

/**
 * 用户扩展服务：关注、公开主页、达人推荐、点赞收藏列表。
 */
public interface ICookUserExtraService
{
    void followUser(Long targetUserId);

    void unfollowUser(Long targetUserId);

    UserPublicProfileVo getUserProfile(Long userId);

    List<UserPublicProfileVo> listRecommendedUsers(Integer limit);

    PageVo<UserPublicProfileVo> listMyFollowers(Integer page, Integer pageSize);

    PageVo<UserPublicProfileVo> listMyFollowing(Integer page, Integer pageSize);

    PageVo<UserInteractionVo> listMyFavorites(String targetType, Integer page, Integer pageSize);

    PageVo<UserInteractionVo> listMyLikes(String targetType, Integer page, Integer pageSize);
}
