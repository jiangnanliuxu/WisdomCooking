package com.ruoyi.cook.auth.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresLogin;
import com.ruoyi.common.security.auth.AuthUtil;
import com.ruoyi.cook.auth.domain.dto.LoginRequest;
import com.ruoyi.cook.auth.domain.dto.RegisterRequest;
import com.ruoyi.cook.auth.domain.dto.ResetPasswordRequest;
import com.ruoyi.cook.auth.domain.dto.SendCodeRequest;
import com.ruoyi.cook.auth.domain.vo.AuthTokenVo;
import com.ruoyi.cook.auth.service.ICookAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 用户端认证接口。
 * <p>
 * 提供手机号验证码发送、注册、登录（验证码/密码）、退出和密码重置。
 * 登录成功后返回 JWT Token，后续请求在 Authorization 头中携带 Bearer Token。
 * </p>
 */
@Tag(name = "用户端-认证", description = "短信验证码、注册、登录、退出和密码重置")
@RestController
@RequestMapping("/api/v1/auth")
public class CookAuthController
{
    @Autowired
    private ICookAuthService authService;

    @Operation(summary = "发送短信验证码", description = "用于注册、登录、找回密码三种场景")
    @PostMapping("/codes")
    @Log(title = "发送短信验证码", businessType = BusinessType.OTHER)
    public R<?> sendCode(@Valid @RequestBody SendCodeRequest request)
    {
        return R.ok(authService.sendCode(request));
    }

    @Operation(summary = "用户注册", description = "手机号 + 密码 + 昵称注册，返回 JWT Token；注册暂不校验短信验证码")
    @PostMapping("/register")
    @Log(title = "用户注册", businessType = BusinessType.INSERT)
    public R<AuthTokenVo> register(@Valid @RequestBody RegisterRequest request)
    {
        return R.ok(authService.register(request));
    }

    @Operation(summary = "用户登录", description = "支持验证码登录（传 code）和密码登录（传 password），优先使用验证码")
    @PostMapping("/login")
    @Log(title = "用户登录", businessType = BusinessType.OTHER)
    public R<AuthTokenVo> login(@Valid @RequestBody LoginRequest request)
    {
        return R.ok(authService.login(request));
    }

    @Operation(summary = "退出登录", description = "清除当前用户的 JWT Token")
    @RequiresLogin
    @PostMapping("/logout")
    @Log(title = "用户退出", businessType = BusinessType.OTHER)
    public R<?> logout()
    {
        AuthUtil.logout();
        return R.ok();
    }

    @Operation(summary = "验证码重置密码", description = "通过短信验证码重置密码，新密码 6-32 位")
    @PostMapping("/password/reset")
    @Log(title = "密码重置", businessType = BusinessType.UPDATE)
    public R<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request)
    {
        authService.resetPassword(request);
        return R.ok();
    }
}
