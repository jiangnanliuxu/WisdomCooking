package com.ruoyi.cook.community.service.impl;

import static com.ruoyi.cook.recipe.domain.RecipeConstants.ACTION_FAVORITE;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.ACTION_FOLLOW;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.ACTION_LIKE;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.BIZ_POST;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.STATUS_ACTIVE;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.STATUS_INACTIVE;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.STATUS_PENDING_REVIEW;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.STATUS_PUBLISHED;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.STATUS_REJECTED;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.STATUS_WITHDRAWN;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.TARGET_POST;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.TARGET_USER;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.cook.auth.domain.CookUser;
import com.ruoyi.cook.auth.mapper.CookUserMapper;
import com.ruoyi.cook.common.domain.vo.PageVo;
import com.ruoyi.cook.community.convert.PostConvert;
import com.ruoyi.cook.community.domain.Post;
import com.ruoyi.cook.community.domain.dto.PostBlockRequest;
import com.ruoyi.cook.community.domain.dto.PostSaveRequest;
import com.ruoyi.cook.community.domain.vo.PostVo;
import com.ruoyi.cook.community.domain.vo.TopicVo;
import com.ruoyi.cook.community.mapper.PostMapper;
import com.ruoyi.cook.recipe.domain.AdminOperationLog;
import com.ruoyi.cook.recipe.domain.ContentInteraction;
import com.ruoyi.cook.recipe.domain.RecipeConstants;
import com.ruoyi.cook.recipe.mapper.AdminOperationLogMapper;
import com.ruoyi.cook.recipe.mapper.ContentInteractionMapper;
import com.ruoyi.cook.community.service.ICommunityService;

/**
 * 社区主链路服务实现。
 * <p>
 * 第三阶段只做社区核心链路：动态发布、公开流、互动和后台审核。
 * 消息通知、处罚记录明细和举报会在后续阶段继续扩展。
 * </p>
 */
@Service
public class CommunityServiceImpl implements ICommunityService
{
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_TOPIC_COUNT = 5;
    private static final int MAX_TOPIC_LENGTH = 20;

    private static final String VISIBILITY_PUBLIC = "public";
    private static final String SOURCE_NORMAL = "normal";
    private static final String STATUS_BLOCKED = "blocked";
    private static final String ACTION_BLOCK = "block";
    private static final String ACTION_WARN = "warn";
    private static final String ACTION_MUTE = "mute";

    /**
     * 首版固定话题，不建 topics 表；后续需要运营配置、热榜时再拆表。
     */
    private static final List<TopicVo> FIXED_TOPICS = List.of(
            new TopicVo("daily", "日常餐桌"),
            new TopicVo("healthy", "健康轻食"),
            new TopicVo("baking", "烘焙甜品"),
            new TopicVo("homework", "交作业"),
            new TopicVo("kitchen", "厨房技巧"),
            new TopicVo("shopping", "食材采购"));

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private CookUserMapper userMapper;

    @Autowired
    private ContentInteractionMapper interactionMapper;

    @Autowired
    private AdminOperationLogMapper adminLogMapper;

    @Autowired
    private PostConvert postConvert;

    @Override
    public PageVo<PostVo> listPublicPosts(String topicCode, String keyword, String recipeCategoryCode, Integer page,
            Integer pageSize)
    {
        Long userId = optionalUserId();
        startPage(page, pageSize);
        List<PostVo> rows = postConvert.toVoList(postMapper.selectPublicList(topicCode, keyword,
                normalizeRecipeCategoryCode(recipeCategoryCode), preferredCategoryCodes(userId),
                RecipeConstants.CHINESE_CATEGORY_CODES));
        enrichPostFlags(rows, userId);
        return toPage(rows, page, pageSize);
    }

    @Override
    public PostVo getPostDetail(Long id)
    {
        Post post = loadPost(id);
        Long userId = optionalUserId();
        if (!isPublicPost(post) && !isOwner(post, userId))
        {
            throw new ServiceException("动态不存在或未公开");
        }
        PostVo vo = postConvert.toVo(post);
        enrichPostFlag(vo, userId);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostVo createPost(PostSaveRequest request)
    {
        Long userId = requireUserId();
        assertUserCanPublish(userId);

        Post post = new Post();
        post.setUserId(userId);
        fillPost(post, request);
        // 所有动态先审后发：创建后直接进入 pending_review，不进入公开社区流。
        post.setStatus(STATUS_PENDING_REVIEW);
        postMapper.insertPost(post);
        return postConvert.toVo(loadPost(post.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostVo updatePost(Long id, PostSaveRequest request)
    {
        Long userId = requireUserId();
        Post post = postMapper.selectByIdForUpdate(id);
        assertOwner(post, userId);
        if (STATUS_BLOCKED.equals(post.getStatus()))
        {
            throw new ServiceException("已屏蔽动态不能编辑");
        }
        fillPost(post, request);
        // 编辑后重新进入审核，避免已发布内容被直接替换上线。
        post.setStatus(STATUS_PENDING_REVIEW);
        post.setRejectReason(null);
        post.setBlockReason(null);
        post.setBlockAction(null);
        postMapper.updatePost(post);
        return postConvert.toVo(loadPost(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostVo submitPost(Long id)
    {
        Long userId = requireUserId();
        Post post = postMapper.selectByIdForUpdate(id);
        assertOwner(post, userId);
        if (!STATUS_REJECTED.equals(post.getStatus()) && !STATUS_WITHDRAWN.equals(post.getStatus()))
        {
            throw new ServiceException("当前状态不能提交审核");
        }
        postMapper.updateStatus(id, STATUS_PENDING_REVIEW);
        return postConvert.toVo(loadPost(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostVo withdrawPost(Long id)
    {
        Long userId = requireUserId();
        Post post = postMapper.selectByIdForUpdate(id);
        assertOwner(post, userId);
        if (!STATUS_PENDING_REVIEW.equals(post.getStatus()))
        {
            throw new ServiceException("只有审核中的动态可以撤回");
        }
        postMapper.updateStatus(id, STATUS_WITHDRAWN);
        return postConvert.toVo(loadPost(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long id)
    {
        Long userId = requireUserId();
        if (postMapper.softDeleteByOwner(id, userId) <= 0)
        {
            throw new ServiceException("动态不存在或无权删除");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likePost(Long id)
    {
        Long userId = requireUserId();
        assertInteractablePost(loadPost(id), userId);
        if (activateInteraction(userId, TARGET_POST, id, ACTION_LIKE))
        {
            postMapper.increaseCounter(id, "like_count");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikePost(Long id)
    {
        Long userId = requireUserId();
        if (deactivateInteraction(userId, TARGET_POST, id, ACTION_LIKE))
        {
            postMapper.decreaseCounter(id, "like_count");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favoritePost(Long id)
    {
        Long userId = requireUserId();
        assertInteractablePost(loadPost(id), userId);
        if (activateInteraction(userId, TARGET_POST, id, ACTION_FAVORITE))
        {
            postMapper.increaseCounter(id, "favorite_count");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfavoritePost(Long id)
    {
        Long userId = requireUserId();
        if (deactivateInteraction(userId, TARGET_POST, id, ACTION_FAVORITE))
        {
            postMapper.decreaseCounter(id, "favorite_count");
        }
    }

    @Override
    public PageVo<PostVo> listMyPosts(String status, Integer page, Integer pageSize)
    {
        Long userId = requireUserId();
        startPage(page, pageSize);
        List<PostVo> rows = postConvert.toVoList(postMapper.selectMine(userId, status));
        enrichPostFlags(rows, userId);
        return toPage(rows, page, pageSize);
    }

    @Override
    public List<TopicVo> listTopics()
    {
        return FIXED_TOPICS;
    }

    @Override
    public PageVo<PostVo> listAdminPosts(String status, String keyword, Integer page, Integer pageSize)
    {
        String resolvedStatus = StringUtils.isBlank(status) ? STATUS_PENDING_REVIEW : status;
        startPage(page, pageSize);
        return toPage(postConvert.toVoList(postMapper.selectAdminList(resolvedStatus, keyword)), page, pageSize);
    }

    @Override
    public PostVo getAdminPostDetail(Long id)
    {
        return postConvert.toVo(loadPost(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostVo approvePost(Long id)
    {
        Post before = postMapper.selectByIdForUpdate(id);
        if (before == null)
        {
            throw new ServiceException("动态不存在");
        }
        postMapper.approvePost(id);
        Post after = loadPost(id);
        writeAdminLog(id, "approve", JSON.toJSONString(before), JSON.toJSONString(after), "动态审核通过");
        return postConvert.toVo(after);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostVo blockPost(Long id, PostBlockRequest request)
    {
        Post before = postMapper.selectByIdForUpdate(id);
        if (before == null)
        {
            throw new ServiceException("动态不存在");
        }
        String action = normalizeBlockAction(request.getAction());
        postMapper.blockPost(id, request.getReason(), action);
        if (ACTION_MUTE.equals(action))
        {
            // 屏蔽并禁言：用户仍可浏览和登录，但不能发布动态、评论和消息。
            userMapper.updateStatus(before.getUserId(), "muted");
        }
        Post after = loadPost(id);
        writeAdminLog(id, "block", JSON.toJSONString(before), JSON.toJSONString(after),
                "动态屏蔽：" + request.getReason());
        return postConvert.toVo(after);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostVo restorePost(Long id)
    {
        Post before = postMapper.selectByIdForUpdate(id);
        if (before == null)
        {
            throw new ServiceException("动态不存在");
        }
        if (!STATUS_BLOCKED.equals(before.getStatus()))
        {
            throw new ServiceException("只有已屏蔽动态可以恢复");
        }
        postMapper.restorePost(id);
        Post after = loadPost(id);
        writeAdminLog(id, "restore", JSON.toJSONString(before), JSON.toJSONString(after), "恢复动态");
        return postConvert.toVo(after);
    }

    private void fillPost(Post post, PostSaveRequest request)
    {
        post.setContent(request.getContent());
        post.setVisibility(StringUtils.isBlank(request.getVisibility()) ? VISIBILITY_PUBLIC : request.getVisibility());
        post.setMediaIdsJson(toJson(request.getMediaIds()));
        post.setTopicCodesJson(toJson(normalizeTopicCodes(request.getTopicCodes())));
        post.setLocation(request.getLocation());
        post.setRelatedRecipeId(request.getRelatedRecipeId());
        post.setSourceType(StringUtils.isBlank(request.getSourceType()) ? SOURCE_NORMAL : request.getSourceType());
    }

    private void assertUserCanPublish(Long userId)
    {
        CookUser user = userMapper.selectById(userId);
        if (user == null || !"normal".equals(user.getStatus()))
        {
            throw new ServiceException("当前账号状态不能发布动态");
        }
    }

    private List<String> normalizeTopicCodes(List<String> topicCodes)
    {
        if (topicCodes == null || topicCodes.isEmpty())
        {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String rawTopic : topicCodes)
        {
            if (StringUtils.isBlank(rawTopic))
            {
                continue;
            }
            String topic = rawTopic.trim();
            if (topic.length() > MAX_TOPIC_LENGTH)
            {
                throw new ServiceException("单个动态话题不能超过20个字");
            }
            normalized.add(topic);
            if (normalized.size() > MAX_TOPIC_COUNT)
            {
                throw new ServiceException("动态话题最多5个");
            }
        }
        return new ArrayList<>(normalized);
    }

    private Post loadPost(Long id)
    {
        Post post = postMapper.selectById(id);
        if (post == null)
        {
            throw new ServiceException("动态不存在");
        }
        return post;
    }

    private void assertOwner(Post post, Long userId)
    {
        if (post == null || !userId.equals(post.getUserId()))
        {
            throw new ServiceException("动态不存在或无权操作");
        }
    }

    private void assertInteractablePost(Post post, Long userId)
    {
        if (post == null || (!isPublicPost(post) && !isOwner(post, userId)))
        {
            throw new ServiceException("动态不存在或未公开");
        }
    }

    private boolean isPublicPost(Post post)
    {
        return STATUS_PUBLISHED.equals(post.getStatus()) && VISIBILITY_PUBLIC.equals(post.getVisibility());
    }

    private boolean isOwner(Post post, Long userId)
    {
        return userId != null && userId.equals(post.getUserId());
    }

    private void enrichPostFlags(List<PostVo> rows, Long userId)
    {
        if (userId == null)
        {
            return;
        }
        rows.forEach(row -> enrichPostFlag(row, userId));
    }

    private void enrichPostFlag(PostVo row, Long userId)
    {
        if (userId == null)
        {
            return;
        }
        row.setLiked(isInteractionActive(userId, TARGET_POST, row.getId(), ACTION_LIKE));
        row.setFavorited(isInteractionActive(userId, TARGET_POST, row.getId(), ACTION_FAVORITE));
        row.setAuthorFollowed(isAuthorFollowed(userId, row.getUserId()));
    }

    private boolean isAuthorFollowed(Long userId, Long authorId)
    {
        return userId != null && authorId != null && !userId.equals(authorId)
                && isInteractionActive(userId, TARGET_USER, authorId, ACTION_FOLLOW);
    }

    private String normalizeRecipeCategoryCode(String recipeCategoryCode)
    {
        return StringUtils.isBlank(recipeCategoryCode) ? null : recipeCategoryCode.trim();
    }

    private List<String> preferredCategoryCodes(Long userId)
    {
        if (userId == null)
        {
            return List.of();
        }
        CookUser user = userMapper.selectById(userId);
        if (user == null || StringUtils.isBlank(user.getInterestTagsJson()))
        {
            return List.of();
        }
        try
        {
            return RecipeConstants.categoryCodesByNames(JSON.parseArray(user.getInterestTagsJson(), String.class));
        }
        catch (Exception ex)
        {
            return List.of();
        }
    }

    private boolean activateInteraction(Long userId, String targetType, Long targetId, String actionType)
    {
        ContentInteraction interaction = interactionMapper.selectOne(userId, targetType, targetId, actionType);
        if (interaction == null)
        {
            interaction = new ContentInteraction();
            interaction.setUserId(userId);
            interaction.setTargetType(targetType);
            interaction.setTargetId(targetId);
            interaction.setActionType(actionType);
            interaction.setStatus(STATUS_ACTIVE);
            interactionMapper.insertInteraction(interaction);
            return true;
        }
        if (STATUS_ACTIVE.equals(interaction.getStatus()))
        {
            return false;
        }
        interactionMapper.updateStatus(interaction.getId(), STATUS_ACTIVE);
        return true;
    }

    private boolean deactivateInteraction(Long userId, String targetType, Long targetId, String actionType)
    {
        ContentInteraction interaction = interactionMapper.selectOne(userId, targetType, targetId, actionType);
        if (interaction == null || !STATUS_ACTIVE.equals(interaction.getStatus()))
        {
            return false;
        }
        interactionMapper.updateStatus(interaction.getId(), STATUS_INACTIVE);
        return true;
    }

    private boolean isInteractionActive(Long userId, String targetType, Long targetId, String actionType)
    {
        ContentInteraction interaction = interactionMapper.selectOne(userId, targetType, targetId, actionType);
        return interaction != null && STATUS_ACTIVE.equals(interaction.getStatus());
    }

    private String normalizeBlockAction(String action)
    {
        if (StringUtils.isBlank(action))
        {
            return ACTION_BLOCK;
        }
        if (ACTION_BLOCK.equals(action) || ACTION_WARN.equals(action) || ACTION_MUTE.equals(action))
        {
            return action;
        }
        throw new ServiceException("不支持的处理方式");
    }

    private String toJson(Object value)
    {
        if (value == null)
        {
            return null;
        }
        return JSON.toJSONString(value);
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

    private Long optionalUserId()
    {
        return SecurityUtils.getUserId();
    }

    private void writeAdminLog(Long postId, String action, String beforeJson, String afterJson, String remark)
    {
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminId(optionalUserId());
        log.setBizType(BIZ_POST);
        log.setBizId(postId);
        log.setAction(action);
        log.setBeforeJson(beforeJson);
        log.setAfterJson(afterJson);
        log.setRemark(remark);
        adminLogMapper.insertLog(log);
    }

    private void startPage(Integer page, Integer pageSize)
    {
        PageHelper.startPage(normalizePage(page), normalizePageSize(pageSize));
    }

    private <T> PageVo<T> toPage(List<T> rows, Integer page, Integer pageSize)
    {
        PageInfo<T> pageInfo = new PageInfo<>(rows);
        return new PageVo<>(normalizePage(page), normalizePageSize(pageSize), pageInfo.getTotal(), rows);
    }

    private int normalizePage(Integer page)
    {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    private int normalizePageSize(Integer pageSize)
    {
        if (pageSize == null || pageSize < 1)
        {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
