package com.ruoyi.cook.message.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 群详情展示对象。
 */
@Data
@Schema(description = "群详情展示对象")
public class GroupVo
{
    private Long id;
    private Long ownerId;
    private String ownerNickname;
    private Long conversationId;
    private String name;
    private Long avatarMediaId;
    private String intro;
    private String notice;
    private String status;
    private Integer memberCount;
    private Integer messageCount;
    private LocalDateTime createdAt;
}
