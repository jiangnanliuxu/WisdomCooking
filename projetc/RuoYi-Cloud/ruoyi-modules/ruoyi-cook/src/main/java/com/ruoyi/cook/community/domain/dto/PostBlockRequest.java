package com.ruoyi.cook.community.domain.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 动态屏蔽请求。
 * <p>
 * blockAction 用于预留后台处罚动作：仅屏蔽、屏蔽并警告、屏蔽并禁言。
 * </p>
 */
@Data
@Schema(description = "动态屏蔽请求")
public class PostBlockRequest
{
    @NotBlank(message = "屏蔽原因不能为空")
    @Size(max = 100, message = "屏蔽原因不能超过100个字符")
    @Schema(description = "屏蔽原因", example = "广告推广")
    private String reason;

    @Size(max = 20, message = "处理方式不能超过20个字符")
    @Schema(description = "处理方式：block/warn/mute，默认 block", example = "block")
    private String action;
}
