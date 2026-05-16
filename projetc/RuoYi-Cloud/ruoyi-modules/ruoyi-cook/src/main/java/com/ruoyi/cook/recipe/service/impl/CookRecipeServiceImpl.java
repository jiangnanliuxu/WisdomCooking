package com.ruoyi.cook.recipe.service.impl;

import static com.ruoyi.cook.recipe.domain.RecipeConstants.ACTION_FAVORITE;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.ACTION_FOLLOW;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.ACTION_LIKE;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.ACTION_SHARE;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.BIZ_RECIPE;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.PUBLISH_OFFLINE;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.PUBLISH_ONLINE;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.STATUS_ACTIVE;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.STATUS_DRAFT;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.STATUS_INACTIVE;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.STATUS_NORMAL;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.STATUS_PENDING_REVIEW;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.STATUS_PUBLISHED;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.STATUS_REJECTED;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.STATUS_WITHDRAWN;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.TARGET_COMMENT;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.TARGET_POST;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.TARGET_RECIPE;
import static com.ruoyi.cook.recipe.domain.RecipeConstants.TARGET_USER;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
import com.ruoyi.cook.community.domain.Post;
import com.ruoyi.cook.community.mapper.PostMapper;
import com.ruoyi.cook.operation.mapper.OperationMapper;
import com.ruoyi.cook.operation.service.OssMediaStorageService;
import com.ruoyi.cook.operation.service.VideoTranscodeService;
import com.ruoyi.cook.operation.vo.MediaAssetVo;
import com.ruoyi.cook.recipe.domain.AdminOperationLog;
import com.ruoyi.cook.recipe.domain.Comment;
import com.ruoyi.cook.recipe.domain.ContentInteraction;
import com.ruoyi.cook.recipe.domain.Recipe;
import com.ruoyi.cook.recipe.domain.RecipeVersion;
import com.ruoyi.cook.recipe.domain.dto.CommentCreateRequest;
import com.ruoyi.cook.recipe.domain.dto.RecipeSaveRequest;
import com.ruoyi.cook.recipe.domain.vo.CommentVo;
import com.ruoyi.cook.recipe.domain.vo.RecipeDetailVo;
import com.ruoyi.cook.recipe.domain.vo.RecipeListItemVo;
import com.ruoyi.cook.recipe.convert.CommentConvert;
import com.ruoyi.cook.recipe.mapper.AdminOperationLogMapper;
import com.ruoyi.cook.recipe.mapper.CommentMapper;
import com.ruoyi.cook.recipe.mapper.ContentInteractionMapper;
import com.ruoyi.cook.recipe.mapper.RecipeMapper;
import com.ruoyi.cook.recipe.mapper.RecipeVersionMapper;
import com.ruoyi.cook.recipe.service.ICookCategoryService;
import com.ruoyi.cook.recipe.service.ICookRecipeService;

/**
 * 菜谱主链路服务实现。
 */
@Service
public class CookRecipeServiceImpl implements ICookRecipeService
{
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private RecipeMapper recipeMapper;

    @Autowired
    private RecipeVersionMapper versionMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private ContentInteractionMapper interactionMapper;

    @Autowired
    private AdminOperationLogMapper adminLogMapper;

    @Autowired
    private CookUserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private OperationMapper operationMapper;

    @Autowired
    private VideoTranscodeService videoTranscodeService;

    @Autowired
    private OssMediaStorageService ossMediaStorageService;

    @Autowired
    private CommentConvert commentConvert;

    @Autowired
    private ICookCategoryService categoryService;

    @Override
    public PageVo<RecipeListItemVo> listPublicRecipes(String categoryCode, String difficulty, String keyword,
            Integer page, Integer pageSize)
    {
        startPage(page, pageSize);
        List<RecipeListItemVo> rows = recipeMapper.selectPublicList(categoryCode, difficulty, keyword, List.of());
        enrichRecipeFlags(rows, optionalUserId());
        return toPage(rows, page, pageSize);
    }

    @Override
    public RecipeDetailVo getRecipeDetail(Long id)
    {
        Recipe recipe = loadRecipe(id);
        Long currentUserId = optionalUserId();
        boolean owner = currentUserId != null && currentUserId.equals(recipe.getAuthorId());
        RecipeVersion version;

        if (owner)
        {
            // 作者查看详情时返回最新版本，方便编辑草稿和查看驳回原因。
            version = versionMapper.selectLatestByRecipeId(id);
        }
        else
        {
            assertPublicVisible(recipe);
            version = versionMapper.selectCurrentByRecipeId(id);
        }
        RecipeDetailVo detail = buildDetail(recipe, version);
        enrichRecipeFlag(detail, currentUserId);
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecipeDetailVo createRecipe(RecipeSaveRequest request)
    {
        Long userId = requireUserId();
        assertKnownCategory(request.getCategoryCode());

        Recipe recipe = new Recipe();
        recipe.setAuthorId(userId);
        recipe.setCategoryCode(request.getCategoryCode());
        recipe.setReviewStatus(STATUS_DRAFT);
        recipe.setPublishStatus(PUBLISH_OFFLINE);
        recipeMapper.insertRecipe(recipe);

        RecipeVersion version = buildVersion(recipe.getId(), userId, 1, STATUS_DRAFT, request);
        versionMapper.insertVersion(version);
        return buildDetail(loadRecipe(recipe.getId()), versionMapper.selectById(version.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecipeDetailVo updateRecipe(Long id, RecipeSaveRequest request)
    {
        Long userId = requireUserId();
        assertKnownCategory(request.getCategoryCode());
        Recipe recipe = recipeMapper.selectByIdForUpdate(id);
        assertRecipeOwner(recipe, userId);

        RecipeVersion latest = versionMapper.selectLatestByRecipeIdForUpdate(id);
        if (latest != null && STATUS_PENDING_REVIEW.equals(latest.getStatus()))
        {
            throw new ServiceException("菜谱正在审核中，不能编辑");
        }

        RecipeVersion savedVersion;
        if (latest == null || shouldCreateNewVersion(recipe, latest))
        {
            // 已发布菜谱编辑时创建新版本，审核通过前 current_version_id 仍指向旧版本。
            int nextVersionNo = nextVersionNo(id);
            savedVersion = buildVersion(id, userId, nextVersionNo, STATUS_DRAFT, request);
            versionMapper.insertVersion(savedVersion);
        }
        else
        {
            savedVersion = fillVersion(latest, STATUS_DRAFT, request);
            versionMapper.updateVersion(savedVersion);
        }

        recipeMapper.updateCategoryAndReview(id, request.getCategoryCode(), STATUS_DRAFT);
        return buildDetail(loadRecipe(id), versionMapper.selectById(savedVersion.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecipeDetailVo submitRecipe(Long id)
    {
        Long userId = requireUserId();
        Recipe recipe = recipeMapper.selectByIdForUpdate(id);
        assertRecipeOwner(recipe, userId);
        RecipeVersion latest = versionMapper.selectLatestByRecipeIdForUpdate(id);
        if (latest == null)
        {
            throw new ServiceException("菜谱版本不存在");
        }
        if (!isSubmitAllowed(latest.getStatus()))
        {
            throw new ServiceException("当前状态不能提交审核");
        }

        versionMapper.updateReject(latest.getId(), STATUS_PENDING_REVIEW, null);
        recipeMapper.updateReviewStatus(id, STATUS_PENDING_REVIEW);
        return buildDetail(loadRecipe(id), versionMapper.selectById(latest.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecipeDetailVo withdrawRecipe(Long id)
    {
        Long userId = requireUserId();
        Recipe recipe = recipeMapper.selectByIdForUpdate(id);
        assertRecipeOwner(recipe, userId);
        RecipeVersion pending = versionMapper.selectPendingByRecipeId(id);
        if (pending == null)
        {
            throw new ServiceException("没有待审核版本可撤回");
        }

        versionMapper.updateStatus(pending.getId(), STATUS_WITHDRAWN);
        recipeMapper.updateReviewStatus(id, STATUS_WITHDRAWN);
        return buildDetail(loadRecipe(id), versionMapper.selectById(pending.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecipe(Long id)
    {
        Long userId = requireUserId();
        if (recipeMapper.softDeleteByOwner(id, userId) <= 0)
        {
            throw new ServiceException("菜谱不存在或无权删除");
        }
    }

    @Override
    public PageVo<RecipeListItemVo> listMyRecipes(String status, Integer page, Integer pageSize)
    {
        Long userId = requireUserId();
        startPage(page, pageSize);
        List<RecipeListItemVo> rows = recipeMapper.selectMine(userId, status);
        enrichRecipeFlags(rows, userId);
        return toPage(rows, page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeRecipe(Long id)
    {
        Long userId = requireUserId();
        assertInteractableRecipe(loadRecipe(id), userId);
        if (activateInteraction(userId, TARGET_RECIPE, id, ACTION_LIKE))
        {
            recipeMapper.increaseCounter(id, "like_count");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeRecipe(Long id)
    {
        Long userId = requireUserId();
        if (deactivateInteraction(userId, TARGET_RECIPE, id, ACTION_LIKE))
        {
            recipeMapper.decreaseCounter(id, "like_count");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favoriteRecipe(Long id)
    {
        Long userId = requireUserId();
        assertInteractableRecipe(loadRecipe(id), userId);
        if (activateInteraction(userId, TARGET_RECIPE, id, ACTION_FAVORITE))
        {
            recipeMapper.increaseCounter(id, "favorite_count");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfavoriteRecipe(Long id)
    {
        Long userId = requireUserId();
        if (deactivateInteraction(userId, TARGET_RECIPE, id, ACTION_FAVORITE))
        {
            recipeMapper.decreaseCounter(id, "favorite_count");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shareRecipe(Long id)
    {
        Long userId = requireUserId();
        assertInteractableRecipe(loadRecipe(id), userId);
        activateInteraction(userId, TARGET_RECIPE, id, ACTION_SHARE);
    }

    @Override
    public PageVo<CommentVo> listComments(String targetType, Long targetId, Integer page, Integer pageSize)
    {
        assertSupportedCommentTarget(targetType);
        if (TARGET_RECIPE.equals(targetType))
        {
            assertCommentVisible(loadRecipe(targetId), optionalUserId());
        }
        if (TARGET_POST.equals(targetType))
        {
            assertPostCommentVisible(loadPost(targetId), optionalUserId());
        }
        startPage(page, pageSize);
        List<CommentVo> rows = commentConvert.toVoList(commentMapper.selectList(targetType, targetId));
        Long userId = optionalUserId();
        if (userId != null)
        {
            rows.forEach(row -> row.setLiked(isInteractionActive(userId, TARGET_COMMENT, row.getId(), ACTION_LIKE)));
        }
        return toPage(rows, page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentVo createComment(CommentCreateRequest request)
    {
        Long userId = requireUserId();
        assertUserCanComment(userId);
        assertSupportedCommentTarget(request.getTargetType());
        if (TARGET_RECIPE.equals(request.getTargetType()))
        {
            assertInteractableRecipe(loadRecipe(request.getTargetId()), userId);
        }
        if (TARGET_POST.equals(request.getTargetType()))
        {
            assertPostInteractable(loadPost(request.getTargetId()), userId);
        }
        if (request.getParentId() != null)
        {
            assertParentComment(request);
        }

        Comment comment = new Comment();
        comment.setTargetType(request.getTargetType());
        comment.setTargetId(request.getTargetId());
        comment.setUserId(userId);
        comment.setParentId(request.getParentId());
        comment.setContent(request.getContent());
        comment.setStatus(STATUS_NORMAL);
        commentMapper.insertComment(comment);
        if (TARGET_RECIPE.equals(request.getTargetType()))
        {
            recipeMapper.increaseCounter(request.getTargetId(), "comment_count");
        }
        if (TARGET_POST.equals(request.getTargetType()))
        {
            postMapper.increaseCounter(request.getTargetId(), "comment_count");
        }
        return commentConvert.toVo(commentMapper.selectById(comment.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long id)
    {
        Long userId = requireUserId();
        Comment comment = commentMapper.selectById(id);
        if (comment == null)
        {
            throw new ServiceException("评论不存在");
        }
        if (commentMapper.softDeleteByOwner(id, userId) <= 0)
        {
            throw new ServiceException("评论不存在或无权删除");
        }
        if (TARGET_RECIPE.equals(comment.getTargetType()))
        {
            recipeMapper.decreaseCounter(comment.getTargetId(), "comment_count");
        }
        if (TARGET_POST.equals(comment.getTargetType()))
        {
            postMapper.decreaseCounter(comment.getTargetId(), "comment_count");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long id)
    {
        Long userId = requireUserId();
        if (commentMapper.selectById(id) == null)
        {
            throw new ServiceException("评论不存在");
        }
        if (activateInteraction(userId, TARGET_COMMENT, id, ACTION_LIKE))
        {
            commentMapper.increaseLikeCount(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeComment(Long id)
    {
        Long userId = requireUserId();
        if (deactivateInteraction(userId, TARGET_COMMENT, id, ACTION_LIKE))
        {
            commentMapper.decreaseLikeCount(id);
        }
    }

    @Override
    public PageVo<RecipeListItemVo> listAdminRecipes(String reviewStatus, String publishStatus, String keyword,
            Integer page, Integer pageSize)
    {
        startPage(page, pageSize);
        return toPage(recipeMapper.selectAdminList(reviewStatus, publishStatus, keyword), page, pageSize);
    }

    @Override
    public PageVo<RecipeListItemVo> listRecipeAudits(String status, String keyword, Integer page, Integer pageSize)
    {
        String auditStatus = StringUtils.isBlank(status) ? STATUS_PENDING_REVIEW : status;
        startPage(page, pageSize);
        return toPage(recipeMapper.selectAuditList(auditStatus, keyword), page, pageSize);
    }

    @Override
    public RecipeDetailVo getAdminRecipeDetail(Long id)
    {
        Recipe recipe = loadRecipe(id);
        RecipeVersion pending = versionMapper.selectPendingByRecipeId(id);
        RecipeVersion version = pending == null ? versionMapper.selectLatestByRecipeId(id) : pending;
        return buildDetail(recipe, version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteRecipe(Long id)
    {
        Recipe recipe = recipeMapper.selectByIdForUpdate(id);
        if (recipe == null)
        {
            throw new ServiceException("菜谱不存在");
        }
        recipeMapper.softDeleteById(id);
        writeAdminLog(id, "delete", JSON.toJSONString(recipe), null, "后台删除菜谱");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecipeDetailVo approveRecipe(Long id)
    {
        Recipe recipe = recipeMapper.selectByIdForUpdate(id);
        if (recipe == null)
        {
            throw new ServiceException("菜谱不存在");
        }
        RecipeVersion pending = versionMapper.selectPendingByRecipeId(id);
        if (pending == null)
        {
            throw new ServiceException("没有待审核版本");
        }
        String beforeJson = JSON.toJSONString(pending);

        String videoJson = markVideoTranscodingIfNeeded(pending.getVideoJson());
        if (!StringUtils.equals(videoJson, pending.getVideoJson()))
        {
            versionMapper.updateVideoJson(pending.getId(), videoJson);
            pending.setVideoJson(videoJson);
        }
        versionMapper.updateReject(pending.getId(), STATUS_PUBLISHED, null);
        recipeMapper.updateCurrentVersionAndStatus(id, pending.getId(), STATUS_PUBLISHED, PUBLISH_ONLINE);
        writeAdminLog(id, "approve", beforeJson, JSON.toJSONString(versionMapper.selectById(pending.getId())), "菜谱审核通过");
        return buildDetail(loadRecipe(id), versionMapper.selectById(pending.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecipeDetailVo rejectRecipe(Long id, String reason)
    {
        Recipe recipe = recipeMapper.selectByIdForUpdate(id);
        if (recipe == null)
        {
            throw new ServiceException("菜谱不存在");
        }
        RecipeVersion pending = versionMapper.selectPendingByRecipeId(id);
        if (pending == null)
        {
            throw new ServiceException("没有待审核版本");
        }
        String beforeJson = JSON.toJSONString(pending);

        versionMapper.updateReject(pending.getId(), STATUS_REJECTED, reason);
        recipeMapper.updateReviewStatus(id, STATUS_REJECTED);
        writeAdminLog(id, "reject", beforeJson, JSON.toJSONString(versionMapper.selectById(pending.getId())), reason);
        return buildDetail(loadRecipe(id), versionMapper.selectById(pending.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecipeDetailVo onlineRecipe(Long id)
    {
        Recipe recipe = recipeMapper.selectByIdForUpdate(id);
        if (recipe == null || recipe.getCurrentVersionId() == null)
        {
            throw new ServiceException("菜谱未审核通过，不能上架");
        }
        RecipeVersion current = versionMapper.selectCurrentByRecipeId(id);
        if (current == null || !STATUS_PUBLISHED.equals(current.getStatus()))
        {
            throw new ServiceException("当前版本未发布，不能上架");
        }
        recipeMapper.updatePublishStatus(id, PUBLISH_ONLINE);
        writeAdminLog(id, "online", JSON.toJSONString(recipe), JSON.toJSONString(loadRecipe(id)), "菜谱上架");
        return buildDetail(loadRecipe(id), current);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecipeDetailVo offlineRecipe(Long id)
    {
        Recipe recipe = recipeMapper.selectByIdForUpdate(id);
        if (recipe == null)
        {
            throw new ServiceException("菜谱不存在");
        }
        recipeMapper.updatePublishStatus(id, PUBLISH_OFFLINE);
        writeAdminLog(id, "offline", JSON.toJSONString(recipe), JSON.toJSONString(loadRecipe(id)), "菜谱下架");
        RecipeVersion version = recipe.getCurrentVersionId() == null ? versionMapper.selectLatestByRecipeId(id)
                : versionMapper.selectCurrentByRecipeId(id);
        return buildDetail(loadRecipe(id), version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecipeDetailVo triggerVideoTranscode(Long id)
    {
        Recipe recipe = recipeMapper.selectByIdForUpdate(id);
        if (recipe == null)
        {
            throw new ServiceException("菜谱不存在");
        }
        RecipeVersion version = versionMapper.selectLatestByRecipeId(id);
        if (version == null || StringUtils.isBlank(version.getVideoJson()))
        {
            throw new ServiceException("当前版本没有视频信息");
        }
        String beforeJson = version.getVideoJson();
        String videoJson = markVideoTranscodingIfNeeded(version.getVideoJson());
        versionMapper.updateVideoJson(version.getId(), videoJson);
        Long mediaId = extractVideoMediaId(videoJson);
        if (mediaId == null)
        {
            throw new ServiceException("当前版本没有可转码的视频资源");
        }
        videoTranscodeService.enqueueMediaTranscode(mediaId);
        writeAdminLog(id, "video_transcode", beforeJson, videoJson, "触发视频转码");
        return buildDetail(loadRecipe(id), versionMapper.selectById(version.getId()));
    }

    private RecipeVersion buildVersion(Long recipeId, Long authorId, int versionNo, String status, RecipeSaveRequest request)
    {
        RecipeVersion version = new RecipeVersion();
        version.setRecipeId(recipeId);
        version.setAuthorId(authorId);
        version.setVersionNo(versionNo);
        return fillVersion(version, status, request);
    }

    private RecipeVersion fillVersion(RecipeVersion version, String status, RecipeSaveRequest request)
    {
        version.setTitle(request.getTitle());
        version.setCoverMediaId(request.getCoverMediaId());
        version.setIntro(request.getIntro());
        version.setDifficulty(request.getDifficulty());
        version.setCookTime(request.getCookTime());
        version.setServing(request.getServing());
        version.setIngredientsJson(toJson(request.getIngredients()));
        version.setStepsJson(toJson(request.getSteps()));
        version.setTipsJson(toJson(request.getTips()));
        version.setVideoJson(toJson(request.getVideo()));
        version.setStatus(status);
        version.setRejectReason(null);
        return version;
    }

    private boolean shouldCreateNewVersion(Recipe recipe, RecipeVersion latest)
    {
        return recipe.getCurrentVersionId() != null && recipe.getCurrentVersionId().equals(latest.getId())
                && STATUS_PUBLISHED.equals(latest.getStatus());
    }

    private boolean isSubmitAllowed(String status)
    {
        return STATUS_DRAFT.equals(status) || STATUS_REJECTED.equals(status) || STATUS_WITHDRAWN.equals(status);
    }

    private int nextVersionNo(Long recipeId)
    {
        Integer maxVersionNo = versionMapper.selectMaxVersionNo(recipeId);
        return maxVersionNo == null ? 1 : maxVersionNo + 1;
    }

    private Recipe loadRecipe(Long id)
    {
        Recipe recipe = recipeMapper.selectById(id);
        if (recipe == null)
        {
            throw new ServiceException("菜谱不存在");
        }
        return recipe;
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

    private void assertRecipeOwner(Recipe recipe, Long userId)
    {
        if (recipe == null || !userId.equals(recipe.getAuthorId()))
        {
            throw new ServiceException("菜谱不存在或无权操作");
        }
    }

    private void assertPublicVisible(Recipe recipe)
    {
        if (recipe == null || recipe.getCurrentVersionId() == null || !PUBLISH_ONLINE.equals(recipe.getPublishStatus()))
        {
            throw new ServiceException("菜谱不存在或未公开");
        }
    }

    private void assertCommentVisible(Recipe recipe, Long userId)
    {
        if (recipe == null)
        {
            throw new ServiceException("菜谱不存在或未公开");
        }
        if (isPublicRecipe(recipe) || isRecipeOwner(recipe, userId))
        {
            return;
        }
        throw new ServiceException("菜谱不存在或未公开");
    }

    private void assertInteractableRecipe(Recipe recipe, Long userId)
    {
        if (recipe == null)
        {
            throw new ServiceException("菜谱不存在或未公开");
        }
        if (isPublicRecipe(recipe) || isRecipeOwner(recipe, userId))
        {
            return;
        }
        // 互动接口允许作者操作自己的未公开菜谱，其他用户仍只能操作公开菜谱。
        throw new ServiceException("菜谱不存在或未公开");
    }

    private boolean isPublicRecipe(Recipe recipe)
    {
        return recipe.getCurrentVersionId() != null && PUBLISH_ONLINE.equals(recipe.getPublishStatus());
    }

    private boolean isRecipeOwner(Recipe recipe, Long userId)
    {
        return userId != null && userId.equals(recipe.getAuthorId());
    }

    private void assertPostCommentVisible(Post post, Long userId)
    {
        if (post == null)
        {
            throw new ServiceException("动态不存在或未公开");
        }
        if (isPublicPost(post) || isPostOwner(post, userId))
        {
            return;
        }
        throw new ServiceException("动态不存在或未公开");
    }

    private void assertPostInteractable(Post post, Long userId)
    {
        if (post == null)
        {
            throw new ServiceException("动态不存在或未公开");
        }
        if (isPublicPost(post) || isPostOwner(post, userId))
        {
            return;
        }
        // 动态评论复用通用评论表；未公开动态只允许作者自己评论。
        throw new ServiceException("动态不存在或未公开");
    }

    private boolean isPublicPost(Post post)
    {
        return STATUS_PUBLISHED.equals(post.getStatus()) && "public".equals(post.getVisibility());
    }

    private boolean isPostOwner(Post post, Long userId)
    {
        return userId != null && userId.equals(post.getUserId());
    }

    private void assertKnownCategory(String categoryCode)
    {
        categoryService.assertEnabledCategory(categoryCode);
    }

    private void assertSupportedCommentTarget(String targetType)
    {
        if (!TARGET_RECIPE.equals(targetType) && !TARGET_POST.equals(targetType))
        {
            throw new ServiceException("当前阶段仅支持菜谱和动态评论");
        }
    }

    private void assertParentComment(CommentCreateRequest request)
    {
        Comment parent = commentMapper.selectById(request.getParentId());
        if (parent == null || !request.getTargetType().equals(parent.getTargetType())
                || !request.getTargetId().equals(parent.getTargetId()))
        {
            throw new ServiceException("父评论不存在或不属于当前对象");
        }
    }

    private void assertUserCanComment(Long userId)
    {
        CookUser user = userMapper.selectById(userId);
        if (user == null || !"normal".equals(user.getStatus()))
        {
            throw new ServiceException("当前账号状态不能评论");
        }
    }

    private RecipeDetailVo buildDetail(Recipe recipe, RecipeVersion version)
    {
        if (version == null)
        {
            throw new ServiceException("菜谱版本不存在");
        }
        CookUser author = userMapper.selectById(recipe.getAuthorId());
        RecipeDetailVo detail = RecipeDetailVo.from(recipe, version, author == null ? null : author.getNickname());
        enrichVideoState(detail);
        return detail;
    }

    private void enrichVideoState(RecipeDetailVo detail)
    {
        Map<String, Object> video = detail.getVideo();
        Long mediaId = extractVideoMediaId(video);
        if (mediaId == null)
        {
            return;
        }
        MediaAssetVo media = operationMapper.selectMediaVoById(mediaId);
        if (media == null)
        {
            return;
        }
        Map<String, Object> merged = new HashMap<>(video);
        merged.put("mediaId", media.getId());
        merged.put("url", media.getUrl());
        merged.put("hlsUrl", proxyHlsUrl(media));
        merged.put("status", media.getStatus());
        merged.put("originalName", media.getOriginalName());
        merged.put("sizeBytes", media.getSizeBytes());
        if (media.getMetadata() != null)
        {
            Object errorMessage = media.getMetadata().get("errorMessage");
            if (errorMessage != null)
            {
                merged.put("errorMessage", errorMessage);
            }
        }
        detail.setVideo(merged);
    }

    private String proxyHlsUrl(MediaAssetVo media)
    {
        if (media == null)
        {
            return null;
        }
        if ("video".equals(media.getFileType()) && "ready".equals(media.getStatus()))
        {
            return hasHlsPlaylist(media) ? "/api/v1/uploads/" + media.getId() + "/hls/index.m3u8" : null;
        }
        return media.getHlsUrl();
    }

    private boolean hasHlsPlaylist(MediaAssetVo media)
    {
        try
        {
            return ossMediaStorageService.objectExists(resolveHlsPrefix(media) + "index.m3u8");
        }
        catch (ServiceException ex)
        {
            return false;
        }
    }

    private String resolveHlsPrefix(MediaAssetVo media)
    {
        Map<String, Object> metadata = media.getMetadata();
        Object prefix = metadata == null ? null : metadata.get("hlsObjectPrefix");
        if (prefix instanceof String value && StringUtils.isNotBlank(value))
        {
            return value.replaceAll("^/+", "").replaceAll("/+$", "") + "/";
        }
        return "uploads/videos/hls/" + media.getId() + "/";
    }

    private void enrichRecipeFlags(List<RecipeListItemVo> rows, Long userId)
    {
        if (userId == null)
        {
            return;
        }
        for (RecipeListItemVo row : rows)
        {
            row.setLiked(isInteractionActive(userId, TARGET_RECIPE, row.getId(), ACTION_LIKE));
            row.setFavorited(isInteractionActive(userId, TARGET_RECIPE, row.getId(), ACTION_FAVORITE));
        }
    }

    private void enrichRecipeFlag(RecipeDetailVo detail, Long userId)
    {
        if (userId == null)
        {
            return;
        }
        detail.setLiked(isInteractionActive(userId, TARGET_RECIPE, detail.getId(), ACTION_LIKE));
        detail.setFavorited(isInteractionActive(userId, TARGET_RECIPE, detail.getId(), ACTION_FAVORITE));
        detail.setAuthorFollowed(isAuthorFollowed(userId, detail.getAuthorId()));
    }

    private boolean isAuthorFollowed(Long userId, Long authorId)
    {
        return userId != null && authorId != null && !userId.equals(authorId)
                && isInteractionActive(userId, TARGET_USER, authorId, ACTION_FOLLOW);
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

    @SuppressWarnings("unchecked")
    private String markVideoTranscodingIfNeeded(String videoJson)
    {
        if (StringUtils.isBlank(videoJson))
        {
            return videoJson;
        }
        Map<String, Object> video = JSON.parseObject(videoJson, Map.class);
        if (video == null || video.isEmpty())
        {
            return videoJson;
        }
        video.put("status", "transcoding");
        video.put("transcodeStartedAt", LocalDateTime.now().toString());
        return JSON.toJSONString(video);
    }

    private Long extractVideoMediaId(String videoJson)
    {
        if (StringUtils.isBlank(videoJson))
        {
            return null;
        }
        return extractVideoMediaId(JSON.parseObject(videoJson, Map.class));
    }

    private Long extractVideoMediaId(Map<String, Object> video)
    {
        if (video == null || video.isEmpty())
        {
            return null;
        }
        Object value = video.get("mediaId");
        if (value instanceof Number number)
        {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text))
        {
            try
            {
                return Long.parseLong(text);
            }
            catch (NumberFormatException ignored)
            {
                return null;
            }
        }
        return null;
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

    private void writeAdminLog(Long recipeId, String action, String beforeJson, String afterJson, String remark)
    {
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminId(optionalUserId());
        log.setBizType(BIZ_RECIPE);
        log.setBizId(recipeId);
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
