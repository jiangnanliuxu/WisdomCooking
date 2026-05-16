package com.ruoyi.cook.user.domain.dto;

import lombok.Data;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@Schema(description = "更新兴趣标签请求")
public class UpdateInterestsRequest
{
    @NotNull(message = "兴趣标签不能为空")
    @Size(max = 5, message = "兴趣标签最多选择5个")
    @Schema(description = "兴趣标签列表，最多 5 个", example = "[\"川菜\", \"烘焙\", \"家常菜\"]")
    private List<@NotBlank(message = "兴趣标签不能为空") @Size(max = 20, message = "兴趣标签不能超过20个字符") String> interestTags;

}
