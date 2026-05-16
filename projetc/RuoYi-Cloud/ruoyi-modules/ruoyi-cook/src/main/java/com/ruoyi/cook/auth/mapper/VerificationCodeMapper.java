package com.ruoyi.cook.auth.mapper;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.cook.auth.domain.VerificationCode;

/**
 * 短信验证码 Mapper。
 * <p>
 * 用于验证码的写入、查询、使用标记和错误计数。
 * </p>
 */
public interface VerificationCodeMapper
{
    /** 新增验证码记录 */
    int insertCode(VerificationCode code);

    /** 查询手机号+场景下最新一条未使用且未过期的验证码 */
    VerificationCode selectLatestValid(@Param("phone") String phone, @Param("scene") String scene);

    /** 查询手机号+场景下最新一条记录（含已使用和已过期），用于发送频率控制 */
    VerificationCode selectLatestByPhoneAndScene(@Param("phone") String phone, @Param("scene") String scene);

    /** 标记单条验证码为已使用 */
    int markUsed(Long id);

    /** 将手机号+场景下所有未使用的验证码标记为已使用，防止旧码重用 */
    int markAllUsedByPhoneAndScene(@Param("phone") String phone, @Param("scene") String scene);

    /** 递增验证码错误尝试次数 */
    int increaseErrorCount(Long id);
}
