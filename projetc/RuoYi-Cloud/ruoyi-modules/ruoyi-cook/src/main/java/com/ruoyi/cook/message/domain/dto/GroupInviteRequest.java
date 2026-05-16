package com.ruoyi.cook.message.domain.dto;

import lombok.Data;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * 邀请群成员请求。
 */
@Data
@Schema(description = "邀请群成员请求")
public class GroupInviteRequest
{
    @NotEmpty(message = "邀请成员不能为空")
    @Size(max = 100, message = "单次最多邀请100人")
    @Schema(description = "用户ID列表")
    private List<Long> userIds;
}
