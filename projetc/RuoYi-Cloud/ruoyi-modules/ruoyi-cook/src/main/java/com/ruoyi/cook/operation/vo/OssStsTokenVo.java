package com.ruoyi.cook.operation.vo;

import lombok.Data;

/**
 * 浏览器直传 OSS 所需的临时凭证。
 */
@Data
public class OssStsTokenVo
{
    private String accessKeyId;
    private String accessKeySecret;
    private String securityToken;
    private String expiration;
    private String region;
    private String endpoint;
    private String bucket;
}
