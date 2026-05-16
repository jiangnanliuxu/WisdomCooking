package com.ruoyi.cook.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端手工录入知识库文本请求。
 */
@Data
@Schema(description = "AI知识库文本录入请求")
public class AiKnowledgeTextRequest
{
    @NotBlank(message = "文件名不能为空")
    @Size(max = 120, message = "文件名不能超过120个字符")
    @Schema(description = "文件名，建议以.md或.txt结尾", example = "减脂晚餐建议.md")
    private String fileName;

    @NotBlank(message = "文本内容不能为空")
    @Schema(description = "文本或Markdown内容")
    private String content;
}
