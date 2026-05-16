package com.ruoyi.cook.user.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cook.user.domain.vo.UserInteractionVo;
import com.ruoyi.cook.user.domain.vo.UserPublicProfileVo;

/**
 * 用户扩展查询 Mapper。
 * <p>
 * 这里集中处理公开主页、达人推荐、我的点赞和我的收藏等
 * 跨表聚合查询，避免在已有的用户基础 Mapper 中塞入过多联表 SQL。
 * </p>
 */
public interface CookUserExtraMapper
{
    /**
     * 查询用户公开主页聚合信息。
     */
    UserPublicProfileVo selectPublicProfile(Long userId);

    /**
     * 查询推荐用户列表。
     */
    List<UserPublicProfileVo> selectRecommendedUsers(@Param("limit") Integer limit);

    /**
     * 查询关注当前用户的人。
     */
    List<UserPublicProfileVo> selectFollowers(@Param("userId") Long userId);

    /**
     * 查询当前用户关注的人。
     */
    List<UserPublicProfileVo> selectFollowing(@Param("userId") Long userId);

    /**
     * 查询当前用户的点赞 / 收藏列表。
     */
    List<UserInteractionVo> selectMyInteractions(@Param("userId") Long userId, @Param("actionType") String actionType,
            @Param("targetType") String targetType);
}
