package com.ruoyi.cook.auth.domain.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 发送短信验证码请求。
 * <p>
 * scene 支持: register（注册）、login（登录）、reset_password（找回密码）。
 * 手机号格式: 1 开头 11 位数字。
 * </p>
 */
@Data
@Schema(description = "发送短信验证码请求")
public class SendCodeRequest
{
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[0-9]{10}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800000001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @NotBlank(message = "验证码场景不能为空")
    @Schema(description = "验证码场景：register / login / reset_password", example = "register", requiredMode = Schema.RequiredMode.REQUIRED)
    private String scene;

}
