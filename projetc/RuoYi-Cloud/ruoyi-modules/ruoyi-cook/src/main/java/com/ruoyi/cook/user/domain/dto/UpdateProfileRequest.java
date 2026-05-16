package com.ruoyi.cook.user.domain.dto;

import lombok.Data;
import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Data
@Schema(description = "编辑用户资料请求")
public class UpdateProfileRequest
{
    @Size(max = 30, message = "昵称长度不能超过30个字符")
    @Schema(description = "用户昵称", example = "美食达人小王")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "性别：male/female/other", example = "male")
    private String gender;

    @Schema(description = "生日", example = "1995-06-15")
    private LocalDate birthday;

    @Size(max = 120, message = "地区长度不能超过120个字符")
    @Schema(description = "所在地区", example = "上海市 徐汇区")
    private String region;

    @Size(max = 500, message = "个性签名长度不能超过500个字符")
    @Schema(description = "个性签名", example = "热爱美食，享受生活")
    private String bio;

}

