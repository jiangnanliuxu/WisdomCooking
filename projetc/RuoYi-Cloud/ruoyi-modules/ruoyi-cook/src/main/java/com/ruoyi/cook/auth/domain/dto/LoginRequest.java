package com.ruoyi.cook.auth.domain.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 登录请求。
 * <p>
 * 支持验证码登录（传 code）和密码登录（传 password）两种方式。
 * 优先使用验证码：如果同时传入 code 和 password，使用 code 登录。
 * </p>
 */
@Data
@Schema(description = "登录请求")
public class LoginRequest
{
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[0-9]{10}$", message = "手机号格式不正确")
    @Schema(description = "手机号，1 开头 11 位", example = "13800000001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @Schema(description = "密码，密码登录时传入", example = "Test123456")
    private String password;

    @Schema(description = "短信验证码，验证码登录时传入", example = "888888")
    private String code;

}
