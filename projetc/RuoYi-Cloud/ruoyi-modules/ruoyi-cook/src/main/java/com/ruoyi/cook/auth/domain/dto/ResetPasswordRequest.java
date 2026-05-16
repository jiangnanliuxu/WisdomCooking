package com.ruoyi.cook.auth.domain.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 验证码重置密码请求。
 * 新密码 6-32 位，需与确认密码一致。
 */
@Data
@Schema(description = "验证码重置密码请求")
public class ResetPasswordRequest
{
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[0-9]{10}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800000001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "短信验证码", example = "888888", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度必须在6到32位之间")
    @Schema(description = "新密码，6-32 位", example = "NewPass123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "确认密码不能为空")
    @Schema(description = "确认新密码，须与 password 一致", example = "NewPass123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String confirmPassword;

}
