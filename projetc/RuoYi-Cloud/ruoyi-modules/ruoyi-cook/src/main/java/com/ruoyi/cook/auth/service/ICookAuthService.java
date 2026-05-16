package com.ruoyi.cook.auth.service;

import java.util.Map;
import com.ruoyi.cook.auth.domain.dto.LoginRequest;
import com.ruoyi.cook.auth.domain.dto.RegisterRequest;
import com.ruoyi.cook.auth.domain.dto.ResetPasswordRequest;
import com.ruoyi.cook.auth.domain.dto.SendCodeRequest;
import com.ruoyi.cook.auth.domain.vo.AuthTokenVo;

/**
 * 用户端认证服务接口。
 * 提供短信验证码发送、注册、登录（验证码/密码）和密码重置。
 */
public interface ICookAuthService
{
    Map<String, Object> sendCode(SendCodeRequest request);

    AuthTokenVo register(RegisterRequest request);

    AuthTokenVo login(LoginRequest request);

    void resetPassword(ResetPasswordRequest request);
}

