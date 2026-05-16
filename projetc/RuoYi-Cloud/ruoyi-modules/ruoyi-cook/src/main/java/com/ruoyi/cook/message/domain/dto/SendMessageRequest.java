package com.ruoyi.cook.message.domain.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发送消息请求。
 */
@Data
@Schema(description = "发送消息请求")
public class SendMessageRequest
{
    @NotBlank(message = "消息类型不能为空")
    @Size(max = 30, message = "消息类型不能超过30个字符")
    @Schema(description = "消息类型：text/image/voice/system", example = "text")
    private String messageType;

    @Size(max = 2000, message = "消息内容不能超过2000个字符")
    @Schema(description = "文本内容")
    private String content;

    @Size(max = 500, message = "媒体地址不能超过500个字符")
    @Schema(description = "图片或语音媒体地址")
    private String mediaUrl;
}
