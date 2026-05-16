package com.ruoyi.cook.community.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cook.community.domain.Post;

/**
 * 社区动态 Mapper。
 * <p>
 * 动态列表统一在 SQL 层关联 cook_users 表返回作者信息，
 * 避免业务层出现 N+1 查询。
 * </p>
 */
public interface PostMapper
{
    /** 按 ID 查询动态，自动过滤软删除记录。 */
    Post selectById(Long id);

    /** 按 ID 加锁查询，用于审核、编辑等需要状态一致性的写流程。 */
    Post selectByIdForUpdate(Long id);

    /** 社区公开流：只展示 published 动态。 */
    List<Post> selectPublicList(@Param("topicCode") String topicCode, @Param("keyword") String keyword,
            @Param("recipeCategoryCode") String recipeCategoryCode,
            @Param("preferredCategoryCodes") List<String> preferredCategoryCodes,
            @Param("chineseCategoryCodes") List<String> chineseCategoryCodes);

    /** 我的动态：展示当前用户自己的全部非删除动态。 */
    List<Post> selectMine(@Param("userId") Long userId, @Param("status") String status);

    /** 管理端审核列表：支持状态和关键词筛选。 */
    List<Post> selectAdminList(@Param("status") String status, @Param("keyword") String keyword);

    /** 新增动态，默认进入 pending_review。 */
    int insertPost(Post post);

    /** 编辑动态正文和扩展字段。 */
    int updatePost(Post post);

    /** 更新动态状态。 */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /** 审核通过并写入发布时间。 */
    int approvePost(@Param("id") Long id);

    /** 屏蔽动态并保存违规原因和处理动作。 */
    int blockPost(@Param("id") Long id, @Param("reason") String reason, @Param("action") String action);

    /** 从屏蔽状态恢复为已发布。 */
    int restorePost(Long id);

    /** 作者软删除自己的动态。 */
    int softDeleteByOwner(@Param("id") Long id, @Param("userId") Long userId);

    /** 管理端软删除动态。 */
    int softDeleteById(Long id);

    /** 点赞、收藏、评论计数增加。 */
    int increaseCounter(@Param("id") Long id, @Param("column") String column);

    /** 点赞、收藏、评论计数减少，最小归零。 */
    int decreaseCounter(@Param("id") Long id, @Param("column") String column);
}
