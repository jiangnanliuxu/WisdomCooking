package com.ruoyi.cook.user.domain.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户公开主页信息。
 * <p>
 * 该对象面向用户端“用户主页 / 达人推荐”场景，
 * 在基础资料之外补充了菜谱数、动态数、粉丝数等聚合字段。
 * </p>
 */
@Data
public class UserPublicProfileVo
{
    private Long id;

    private String nickname;

    private String avatarUrl;

    private String bio;

    private String gender;

    private String region;

    private String status;

    private Long recipeCount;

    private Long postCount;

    private Long followerCount;

    private Long followingCount;

    private Boolean followed;

    private LocalDateTime createdAt;
}
