package com.ruoyi.cook.operation.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 管理端用户列表 / 详情视图对象。
 */
@Data
public class AdminUserVo
{
    private Long id;
    private String phone;
    private String nickname;
    private String avatarUrl;
    private String gender;
    private String region;
    private String bio;
    private String status;
    private Long recipeCount;
    private Long postCount;
    private Long followerCount;
    private LocalDateTime createdAt;
}
