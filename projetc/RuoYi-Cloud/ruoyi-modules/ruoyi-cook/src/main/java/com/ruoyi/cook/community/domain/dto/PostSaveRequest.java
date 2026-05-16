package com.ruoyi.cook.community.domain.dto;

import lombok.Data;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 动态保存请求。
 * <p>
 * 创建动态后直接进入 pending_review，所有动态必须先审后发。
 * </p>
 */
@Data
@Schema(description = "动态创建或编辑请求")
public class PostSaveRequest
{
    @NotBlank(message = "动态内容不能为空")
    @Size(max = 500, message = "动态内容不能超过500个字符")
    @Schema(description = "动态文本内容，最多500字", example = "今天复刻了川味家常菜，味道很稳定。")
    private String content;

    @Size(max = 20, message = "可见范围不能超过20个字符")
    @Schema(description = "可见范围：public/followers/private，默认 public", example = "public")
    private String visibility;

    @Size(max = 9, message = "动态图片最多9张")
    @Schema(description = "图片媒体ID列表，最多9张")
    private List<Long> mediaIds;

    @Size(max = 10, message = "话题最多10个")
    @Schema(description = "话题编码列表，例如 food、daily")
    private List<String> topicCodes;

    @Size(max = 100, message = "位置不能超过100个字符")
    @Schema(description = "位置名称", example = "上海市 徐汇区")
    private String location;

    @Schema(description = "可选关联菜谱ID")
    private Long relatedRecipeId;

    @Size(max = 20, message = "来源类型不能超过20个字符")
    @Schema(description = "来源类型：normal/checkin/homework，默认 normal", example = "normal")
    private String sourceType;
}
