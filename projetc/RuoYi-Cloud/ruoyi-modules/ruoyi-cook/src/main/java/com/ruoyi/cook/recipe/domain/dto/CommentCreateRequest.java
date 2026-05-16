package com.ruoyi.cook.recipe.domain.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 评论发布请求，支持一级评论和二级回复。
 */
@Data
@Schema(description = "评论发布请求")
public class CommentCreateRequest
{
    @NotBlank(message = "评论对象类型不能为空")
    @Schema(description = "评论对象类型：recipe / post", example = "recipe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String targetType;

    @NotNull(message = "评论对象不能为空")
    @Schema(description = "评论对象ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long targetId;

    @Schema(description = "父评论ID，二级回复时传入", example = "5")
    private Long parentId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过1000个字符")
    @Schema(description = "评论内容，最多 1000 字", example = "这个菜谱太棒了！", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

}
