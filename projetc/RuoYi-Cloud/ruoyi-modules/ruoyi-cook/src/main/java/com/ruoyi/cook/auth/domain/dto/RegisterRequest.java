package com.ruoyi.cook.auth.domain.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求。
 * <p>
 * 需传入手机号、密码（6-32位）、确认密码和昵称（最长30字符）。
 * 密码和确认密码必须一致，服务端做二次校验。
 * </p>
 */
@Data
@Schema(description = "注册请求")
public class RegisterRequest
{
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[0-9]{10}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800000001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @Schema(description = "短信验证码，注册暂不校验；保留字段兼容旧客户端", example = "888888", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String code;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度必须在6到32位之间")
    @Schema(description = "密码，6-32 位", example = "Test123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "确认密码不能为空")
    @Schema(description = "确认密码，须与 password 一致", example = "Test123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String confirmPassword;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 30, message = "昵称长度不能超过30个字符")
    @Schema(description = "用户昵称，最长 30 字符", example = "美食爱好者", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;

}
