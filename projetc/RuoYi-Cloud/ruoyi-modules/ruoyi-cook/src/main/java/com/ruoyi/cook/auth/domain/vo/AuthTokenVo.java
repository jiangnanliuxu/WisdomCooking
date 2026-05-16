package com.ruoyi.cook.auth.domain.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;
import com.ruoyi.cook.user.domain.vo.UserProfileVo;

/**
 * 认证响应 VO，包含 JWT Token、过期时间（秒）和用户资料。
 */
@Data
@NoArgsConstructor
public class AuthTokenVo
{
    private String accessToken;
    private Long expiresIn;
    private UserProfileVo user;

    public AuthTokenVo(Map<String, Object> tokenMap, UserProfileVo user)
    {
        this.accessToken = (String) tokenMap.get("access_token");
        Object expiresInValue = tokenMap.get("expires_in");
        this.expiresIn = expiresInValue instanceof Number ? ((Number) expiresInValue).longValue() : null;
        this.user = user;
    }

}
