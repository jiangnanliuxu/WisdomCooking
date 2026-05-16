package com.ruoyi.cook.recipe.domain.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 菜谱保存请求，创建草稿和编辑菜谱共用。
 */
@Data
@Schema(description = "菜谱保存请求")
public class RecipeSaveRequest
{
    @NotBlank(message = "菜谱标题不能为空")
    @Size(max = 120, message = "菜谱标题不能超过120个字符")
    @Schema(description = "菜谱标题，最多 120 字", example = "宫保鸡丁", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "分类不能为空")
    @Size(max = 50, message = "分类编码不能超过50个字符")
    @Schema(description = "分类编码", example = "sichuan", requiredMode = Schema.RequiredMode.REQUIRED)
    private String categoryCode;

    @Schema(description = "封面图片媒体ID")
    private Long coverMediaId;

    @Size(max = 2000, message = "菜谱简介不能超过2000个字符")
    @Schema(description = "菜谱简介", example = "经典川菜，鸡肉丁与花生米搭配，麻辣鲜香")
    private String intro;

    @Size(max = 30, message = "难度不能超过30个字符")
    @Schema(description = "难度：easy/medium/hard", example = "medium")
    private String difficulty;

    @Size(max = 30, message = "烹饪时间不能超过30个字符")
    @Schema(description = "烹饪时间", example = "30分钟")
    private String cookTime;

    @Size(max = 30, message = "份量不能超过30个字符")
    @Schema(description = "份量", example = "2人份")
    private String serving;

    @Size(max = 100, message = "食材最多100项")
    @Schema(description = "食材列表，每项含 name/amount 等字段")
    private List<Map<String, Object>> ingredients;

    @Size(max = 100, message = "步骤最多100项")
    @Schema(description = "步骤列表，每项含 stepNumber/content/image 等字段")
    private List<Map<String, Object>> steps;

    @Size(max = 50, message = "小贴士最多50项")
    @Schema(description = "小贴士列表")
    private List<Map<String, Object>> tips;

    @Schema(description = "视频信息，含 mediaId/duration/coverUrl 等字段")
    private Map<String, Object> video;

}
