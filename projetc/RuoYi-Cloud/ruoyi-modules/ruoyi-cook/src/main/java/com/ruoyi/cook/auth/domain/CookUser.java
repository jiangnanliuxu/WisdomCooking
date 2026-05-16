package com.ruoyi.cook.auth.domain;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户端普通用户，对应 cook.cook_users。
 */
@Data
public class CookUser implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String phone;
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    private String gender;
    private LocalDate birthday;
    private String region;
    private String bio;
    private String status;
    private String interestTagsJson;
    private String oauthAccountsJson;
    private String statsJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

}
