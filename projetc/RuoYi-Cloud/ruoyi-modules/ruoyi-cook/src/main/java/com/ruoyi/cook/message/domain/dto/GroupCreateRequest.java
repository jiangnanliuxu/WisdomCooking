package com.ruoyi.cook.message.domain.dto;

import lombok.Data;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建群聊请求。
 */
@Data
@Schema(description = "创建群聊请求")
public class GroupCreateRequest
{
    @NotBlank(message = "群名不能为空")
    @Size(max = 120, message = "群名不能超过120个字符")
    @Schema(description = "群名", example = "周末下厨小分队")
    private String name;

    @Schema(description = "群头像媒体ID")
    private Long avatarMediaId;

    @Size(max = 500, message = "群简介不能超过500个字符")
    @Schema(description = "群简介")
    private String intro;

    @Size(max = 2000, message = "群公告不能超过2000个字符")
    @Schema(description = "群公告")
    private String notice;

    @Size(max = 100, message = "初始成员最多100人")
    @Schema(description = "初始成员ID列表，创建者会自动加入")
    private List<Long> memberIds;
}
