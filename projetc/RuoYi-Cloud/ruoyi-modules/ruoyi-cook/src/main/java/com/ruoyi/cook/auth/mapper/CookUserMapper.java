package com.ruoyi.cook.auth.mapper;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.cook.auth.domain.CookUser;

/**
 * 用户 Mapper，对应 cook.cook_users 表。
 * <p>
 * 所有查询自动过滤已删除记录（deleted_at IS NULL）。
 * </p>
 */
public interface CookUserMapper
{
    /** 按主键查询，自动排除已删除 */
    CookUser selectById(Long id);

    /** 按手机号查询（唯一），自动排除已删除 */
    CookUser selectByPhone(String phone);

    /** 新增用户，自动回填主键 ID 和默认时间戳 */
    int insertUser(CookUser user);

    /** 更新密码哈希 */
    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    /** 动态更新用户资料，仅更新非空字段 */
    int updateProfile(CookUser user);

    /** 覆盖写入兴趣标签 JSON */
    int updateInterestTags(@Param("id") Long id, @Param("interestTagsJson") String interestTagsJson);

    /** 更新用户状态，例如 normal、muted、banned */
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
