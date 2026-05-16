package com.ruoyi.cook.user.service.impl;

import static com.ruoyi.cook.recipe.domain.RecipeConstants.ACTION_FAVORITE;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.ACTION_LIKE;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.STATUS_ACTIVE;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.cook.auth.domain.CookUser;
import com.ruoyi.cook.auth.mapper.CookUserMapper;
import com.ruoyi.cook.common.domain.vo.PageVo;
import com.ruoyi.cook.recipe.domain.ContentInteraction;
import com.ruoyi.cook.recipe.mapper.ContentInteractionMapper;
import com.ruoyi.cook.user.domain.vo.UserInteractionVo;
import com.ruoyi.cook.user.domain.vo.UserPublicProfileVo;
import com.ruoyi.cook.user.mapper.CookUserExtraMapper;
import com.ruoyi.cook.user.service.ICookUserExtraService;

/**
 * 用户扩展服务实现。
 * <p>
 * 这里承接的是“个人主页和社交关系”相关逻辑：
 * 关注 / 取消关注、公开主页聚合信息、达人推荐、我的点赞和我的收藏。
 * </p>
 */
@Service
public class CookUserExtraServiceImpl implements ICookUserExtraService
{
    private static final String TARGET_USER = "user";
    private static final String ACTION_FOLLOW = "follow";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_RECOMMEND_LIMIT = 8;

    @Autowired
    private CookUserMapper userMapper;

    @Autowired
    private CookUserExtraMapper userExtraMapper;

    @Autowired
    private ContentInteractionMapper interactionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void followUser(Long targetUserId)
    {
        Long currentUserId = requireUserId();
        if (currentUserId.equals(targetUserId))
        {
            throw new ServiceException("不能关注自己");
        }
        assertUserExists(targetUserId);
        ContentInteraction existing = interactionMapper.selectOne(currentUserId, TARGET_USER, targetUserId, ACTION_FOLLOW);
        if (existing == null)
        {
            ContentInteraction interaction = new ContentInteraction();
            interaction.setUserId(currentUserId);
            interaction.setTargetType(TARGET_USER);
            interaction.setTargetId(targetUserId);
            interaction.setActionType(ACTION_FOLLOW);
            interaction.setStatus(STATUS_ACTIVE);
            interactionMapper.insertInteraction(interaction);
            return;
        }
        if (!STATUS_ACTIVE.equals(existing.getStatus()))
        {
            interactionMapper.updateStatus(existing.getId(), STATUS_ACTIVE);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollowUser(Long targetUserId)
    {
        Long currentUserId = requireUserId();
        ContentInteraction existing = interactionMapper.selectOne(currentUserId, TARGET_USER, targetUserId, ACTION_FOLLOW);
        if (existing != null && STATUS_ACTIVE.equals(existing.getStatus()))
        {
            interactionMapper.updateStatus(existing.getId(), "inactive");
        }
    }

    @Override
    public UserPublicProfileVo getUserProfile(Long userId)
    {
        UserPublicProfileVo profile = userExtraMapper.selectPublicProfile(userId);
        if (profile == null)
        {
            throw new ServiceException("用户不存在");
        }
        Long currentUserId = SecurityUtils.getUserId();
        profile.setFollowed(isFollowed(currentUserId, userId));
        return profile;
    }

    @Override
    public List<UserPublicProfileVo> listRecommendedUsers(Integer limit)
    {
        List<UserPublicProfileVo> users = userExtraMapper.selectRecommendedUsers(normalizeLimit(limit));
        Long currentUserId = SecurityUtils.getUserId();
        users.forEach(user -> user.setFollowed(isFollowed(currentUserId, user.getId())));
        return users;
    }

    @Override
    public PageVo<UserPublicProfileVo> listMyFollowers(Integer page, Integer pageSize)
    {
        Long userId = requireUserId();
        int safePage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int safePageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        PageHelper.startPage(safePage, safePageSize);
        List<UserPublicProfileVo> rows = userExtraMapper.selectFollowers(userId);
        rows.forEach(user -> user.setFollowed(isFollowed(userId, user.getId())));
        PageInfo<UserPublicProfileVo> pageInfo = PageInfo.of(rows);
        return new PageVo<>(pageInfo.getPageNum(), pageInfo.getPageSize(), pageInfo.getTotal(), rows);
    }

    @Override
    public PageVo<UserPublicProfileVo> listMyFollowing(Integer page, Integer pageSize)
    {
        Long userId = requireUserId();
        int safePage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int safePageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        PageHelper.startPage(safePage, safePageSize);
        List<UserPublicProfileVo> rows = userExtraMapper.selectFollowing(userId);
        rows.forEach(user -> user.setFollowed(Boolean.TRUE));
        PageInfo<UserPublicProfileVo> pageInfo = PageInfo.of(rows);
        return new PageVo<>(pageInfo.getPageNum(), pageInfo.getPageSize(), pageInfo.getTotal(), rows);
    }

    @Override
    public PageVo<UserInteractionVo> listMyFavorites(String targetType, Integer page, Integer pageSize)
    {
        return listInteractions(ACTION_FAVORITE, targetType, page, pageSize);
    }

    @Override
    public PageVo<UserInteractionVo> listMyLikes(String targetType, Integer page, Integer pageSize)
    {
        return listInteractions(ACTION_LIKE, targetType, page, pageSize);
    }

    /**
     * 分页查询我的互动记录。
     * 这里统一复用 cook_content_interactions，再按目标类型补齐展示文案。
     */
    private PageVo<UserInteractionVo> listInteractions(String actionType, String targetType, Integer page,
            Integer pageSize)
    {
        Long userId = requireUserId();
        int safePage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int safePageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        PageHelper.startPage(safePage, safePageSize);
        List<UserInteractionVo> rows = userExtraMapper.selectMyInteractions(userId, actionType, targetType);
        PageInfo<UserInteractionVo> pageInfo = PageInfo.of(rows);
        return new PageVo<>(pageInfo.getPageNum(), pageInfo.getPageSize(), pageInfo.getTotal(), rows);
    }

    private boolean isFollowed(Long currentUserId, Long targetUserId)
    {
        if (currentUserId == null || targetUserId == null || currentUserId.equals(targetUserId))
        {
            return Boolean.FALSE;
        }
        ContentInteraction interaction = interactionMapper.selectOne(currentUserId, TARGET_USER, targetUserId, ACTION_FOLLOW);
        return interaction != null && STATUS_ACTIVE.equals(interaction.getStatus());
    }

    private void assertUserExists(Long userId)
    {
        CookUser user = userMapper.selectById(userId);
        if (user == null || user.getDeletedAt() != null)
        {
            throw new ServiceException("目标用户不存在");
        }
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

    private int normalizeLimit(Integer limit)
    {
        if (limit == null || limit < 1)
        {
            return DEFAULT_RECOMMEND_LIMIT;
        }
        return Math.min(limit, 50);
    }
}
