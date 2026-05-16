package com.ruoyi.cook.community.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 首版固定话题展示对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "社区话题")
public class TopicVo
{
    private String code;
    private String name;
}
