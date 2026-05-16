package com.ruoyi.cook.auth.domain;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 短信验证码，对应 cook.cook_verification_codes。
 */
@Data
public class VerificationCode implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String phone;
    private String scene;
    private String code;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private Integer errorCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
