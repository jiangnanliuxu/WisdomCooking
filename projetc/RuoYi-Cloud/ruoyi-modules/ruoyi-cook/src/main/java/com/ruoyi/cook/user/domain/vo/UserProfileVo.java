package com.ruoyi.cook.user.domain.vo;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 用户资料响应对象。
 */
@Data
public class UserProfileVo
{
    private Long id;
    private String phone;
    private String nickname;
    private String avatarUrl;
    private String gender;
    private LocalDate birthday;
    private String region;
    private String bio;
    private String status;
    private List<String> interestTags;
    private String statsJson;
}
