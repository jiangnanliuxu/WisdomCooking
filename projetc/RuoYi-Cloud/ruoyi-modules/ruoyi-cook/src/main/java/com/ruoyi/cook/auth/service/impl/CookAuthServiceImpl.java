package com.ruoyi.cook.auth.service.impl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.security.service.TokenService;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.cook.auth.domain.CookUser;
import com.ruoyi.cook.auth.domain.VerificationCode;
import com.ruoyi.cook.auth.domain.dto.LoginRequest;
import com.ruoyi.cook.auth.domain.dto.RegisterRequest;
import com.ruoyi.cook.auth.domain.dto.ResetPasswordRequest;
import com.ruoyi.cook.auth.domain.dto.SendCodeRequest;
import com.ruoyi.cook.auth.domain.vo.AuthTokenVo;
import com.ruoyi.cook.auth.mapper.CookUserMapper;
import com.ruoyi.cook.auth.mapper.VerificationCodeMapper;
import com.ruoyi.cook.auth.service.ICookAuthService;
import com.ruoyi.cook.user.convert.UserProfileConvert;
import com.ruoyi.system.api.domain.SysUser;
import com.ruoyi.system.api.model.LoginUser;

/**
 * 用户端认证服务实现。
 * <p>
 * 处理手机号注册、验证码/密码登录、密码重置和验证码发送。
 * 密码使用 BCrypt 哈希存储，登录成功签发 JWT Token（通过若依 TokenService）。
 * </p>
 */
@Service
public class CookAuthServiceImpl implements ICookAuthService
{
    /** 支持的验证码场景 */
    private static final Set<String> SUPPORTED_SCENES = Set.of("register", "login", "reset_password");

    /** 验证码有效期（分钟） */
    private static final int CODE_EXPIRE_MINUTES = 5;

    /** 验证码最大错误尝试次数 */
    private static final int MAX_ERROR_COUNT = 5;

    /** 同一手机号同场景发送验证码的最小间隔（秒） */
    private static final int CODE_SEND_COOLDOWN_SECONDS = 60;

    /**
     * 开发模式开关。
     * 开启时 sendCode 接口会在响应中返回 dev_code 字段（明文验证码），方便本地联调。
     * 生产环境必须设为 false。
     */
    @Value("${cook.auth.dev-mode:false}")
    private boolean devMode;

    @Autowired
    private CookUserMapper userMapper;

    @Autowired
    private VerificationCodeMapper codeMapper;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserProfileConvert userProfileConvert;

    /**
     * 发送短信验证码。
     * <p>
     * 生成 6 位数字验证码写入 cook_verification_codes 表，有效期 5 分钟。
     * 首阶段未接入短信服务时，可通过 {@code cook.auth.dev-mode=true} 在响应中返回明文验证码。
     * 同一手机号同场景 60 秒内不可重复请求。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> sendCode(SendCodeRequest request)
    {
        String scene = normalizeScene(request.getScene());

        // 发送频率控制：同一手机号同场景 60 秒内不可重复请求
        VerificationCode latest = codeMapper.selectLatestByPhoneAndScene(request.getPhone(), scene);
        if (latest != null && latest.getCreatedAt() != null)
        {
            long secondsSinceLast = java.time.Duration.between(
                    latest.getCreatedAt(), LocalDateTime.now()).getSeconds();
            if (secondsSinceLast < CODE_SEND_COOLDOWN_SECONDS)
            {
                throw new ServiceException("验证码已发送，请"
                        + (CODE_SEND_COOLDOWN_SECONDS - secondsSinceLast) + "秒后再试");
            }
        }

        // 生成 6 位随机数字验证码
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));

        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setPhone(request.getPhone());
        verificationCode.setScene(scene);
        verificationCode.setCode(code);
        verificationCode.setExpiresAt(LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES));
        verificationCode.setErrorCount(0);
        codeMapper.insertCode(verificationCode);

        Map<String, Object> result = new HashMap<>();
        result.put("expires_in", CODE_EXPIRE_MINUTES * 60);
        result.put("scene", scene);
        if (devMode)
        {
            // 开发环境返回明文验证码，生产环境不返回，短信到达手机
            result.put("dev_code", code);
        }
        return result;
    }

    /**
     * 手机号密码注册。
     * <p>
     * 检查手机号唯一 → BCrypt 哈希密码 → 写入 cook_users 表 → 签发 Token。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthTokenVo register(RegisterRequest request)
    {
        assertPasswordConfirmed(request.getPassword(), request.getConfirmPassword());
        if (userMapper.selectByPhone(request.getPhone()) != null)
        {
            throw new ServiceException("手机号已注册");
        }

        CookUser user = new CookUser();
        user.setPhone(request.getPhone());
        user.setNickname(request.getNickname());
        user.setPasswordHash(SecurityUtils.encryptPassword(request.getPassword()));
        user.setStatus("normal");
        user.setStatsJson("{}");
        userMapper.insertUser(user);
        return createToken(user);
    }

    /**
     * 登录：支持验证码登录和密码登录两种方式。
     * <p>
     * 优先判断传入的是验证码还是密码：
     * <ul>
     *   <li>验证码登录：校验验证码有效性和正确性</li>
     *   <li>密码登录：使用 BCrypt 比对密码哈希</li>
     * </ul>
     * 已注销（deleted）和封禁（banned）用户不可登录；禁言（muted）用户可以登录。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthTokenVo login(LoginRequest request)
    {
        CookUser user = userMapper.selectByPhone(request.getPhone());
        if (user == null || user.getDeletedAt() != null)
        {
            throw new ServiceException("用户不存在");
        }
        // 只有 normal 和 muted 状态允许登录；banned 和 deleted 不可登录
        if (!"normal".equals(user.getStatus()) && !"muted".equals(user.getStatus()))
        {
            throw new ServiceException("账号状态不可登录");
        }

        if (StringUtils.isNotBlank(request.getCode()))
        {
            verifyCode(request.getPhone(), "login", request.getCode());
        }
        else if (StringUtils.isNotBlank(request.getPassword()))
        {
            // 密码登录路径：不需要事务的写操作，但 verifyCode（验证码路径）需要
            if (StringUtils.isBlank(user.getPasswordHash())
                    || !SecurityUtils.matchesPassword(request.getPassword(), user.getPasswordHash()))
            {
                throw new ServiceException("手机号或密码错误");
            }
        }
        else
        {
            throw new ServiceException("请输入密码或验证码");
        }
        return createToken(user);
    }

    /**
     * 验证码重置密码。
     * <p>
     * 流程：校验验证码 → 检查用户存在 → BCrypt 哈希新密码 → 更新密码字段。
     * 不销毁已有 Token（用户可能已在其他设备登录）。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordRequest request)
    {
        assertPasswordConfirmed(request.getPassword(), request.getConfirmPassword());
        CookUser user = userMapper.selectByPhone(request.getPhone());
        if (user == null || user.getDeletedAt() != null)
        {
            throw new ServiceException("用户不存在");
        }
        verifyCode(request.getPhone(), "reset_password", request.getCode());
        userMapper.updatePassword(user.getId(), SecurityUtils.encryptPassword(request.getPassword()));
    }

    /**
     * 校验并规范化验证码场景，不支持的场景直接拒绝。
     */
    private String normalizeScene(String scene)
    {
        if (!SUPPORTED_SCENES.contains(scene))
        {
            throw new ServiceException("不支持的验证码场景");
        }
        return scene;
    }

    /**
     * 断言两次输入的密码一致。
     */
    private void assertPasswordConfirmed(String password, String confirmPassword)
    {
        if (!StringUtils.equals(password, confirmPassword))
        {
            throw new ServiceException("两次输入的密码不一致");
        }
    }

    /**
     * 校验验证码。
     * <p>
     * 查询该手机号+场景下最新一条未使用且未过期的验证码，
     * 检查错误次数 → 比对验证码 → 成功后将该手机号+场景下所有未使用验证码标记为已使用。
     * </p>
     */
    private void verifyCode(String phone, String scene, String code)
    {
        VerificationCode latest = codeMapper.selectLatestValid(phone, scene);
        if (latest == null)
        {
            throw new ServiceException("验证码不存在或已过期");
        }
        if (latest.getErrorCount() != null && latest.getErrorCount() >= MAX_ERROR_COUNT)
        {
            throw new ServiceException("验证码错误次数过多，请重新获取");
        }
        if (!StringUtils.equals(latest.getCode(), code))
        {
            codeMapper.increaseErrorCount(latest.getId());
            throw new ServiceException("验证码错误");
        }
        // 验证成功后将该手机号+场景下所有未使用的验证码全部标记为已使用，防止重用旧码
        codeMapper.markAllUsedByPhoneAndScene(phone, scene);
    }

    /**
     * 为指定用户签发 JWT Token 并返回 AuthTokenVo。
     * <p>
     * 将 CookUser 映射为若依 SysUser（复用 TokenService 的 Redis+JWT 机制），
     * 分配 cook_user 角色。
     * </p>
     */
    private AuthTokenVo createToken(CookUser user)
    {
        LoginUser loginUser = new LoginUser();
        SysUser sysUser = new SysUser();
        sysUser.setUserId(user.getId());
        sysUser.setUserName(user.getPhone());
        sysUser.setNickName(user.getNickname());
        sysUser.setPhonenumber(user.getPhone());
        sysUser.setAvatar(user.getAvatarUrl());
        // CookUser.gender 为自由文本，SysUser.sex 为 char(1)，取首字符兜底
        sysUser.setSex(user.getGender() != null && !user.getGender().isEmpty()
                ? user.getGender().substring(0, 1) : "2");
        sysUser.setStatus("normal".equals(user.getStatus()) || "muted".equals(user.getStatus()) ? "0" : "1");
        loginUser.setSysUser(sysUser);
        loginUser.setUserid(user.getId());
        loginUser.setUsername(user.getPhone());
        loginUser.setRoles(Collections.singleton("cook_user"));
        loginUser.setPermissions(Collections.singleton("cook:user"));
        return new AuthTokenVo(tokenService.createToken(loginUser), userProfileConvert.toVo(user));
    }
}
