package com.ruoyi.cook.message.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 群成员展示对象。
 */
@Data
@Schema(description = "群成员展示对象")
public class GroupMemberVo
{
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String role;
    private String status;
    private LocalDateTime joinedAt;
}
