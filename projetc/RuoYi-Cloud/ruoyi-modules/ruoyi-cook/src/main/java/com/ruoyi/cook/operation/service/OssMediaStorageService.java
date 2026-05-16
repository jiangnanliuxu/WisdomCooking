package com.ruoyi.cook.operation.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.alibaba.fastjson2.JSON;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.sts.model.v20150401.AssumeRoleRequest;
import com.aliyuncs.sts.model.v20150401.AssumeRoleResponse;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.cook.operation.config.CookMediaProperties;
import com.ruoyi.cook.operation.vo.OssStsTokenVo;

/**
 * OSS 对象读写与 STS 授权封装。
 */
@Service
public class OssMediaStorageService
{
    @Autowired
    private CookMediaProperties mediaProperties;

    public OssStsTokenVo assumeUploadRole(String objectKey)
    {
        assertOssConfigured();
        if (!StringUtils.hasText(mediaProperties.getStsRoleArn()))
        {
            throw new ServiceException("OSS STS RoleArn 未配置，无法发放临时上传凭证");
        }
        try
        {
            String region = StringUtils.hasText(mediaProperties.getRegion()) ? mediaProperties.getRegion()
                    : "cn-hangzhou";
            DefaultProfile profile = DefaultProfile.getProfile(region, mediaProperties.getAccessKeyId(),
                    mediaProperties.getAccessKeySecret());
            DefaultAcsClient client = new DefaultAcsClient(profile);
            AssumeRoleRequest request = new AssumeRoleRequest();
            request.setSysMethod(MethodType.POST);
            request.setRoleArn(mediaProperties.getStsRoleArn());
            request.setRoleSessionName("cook-video-upload");
            request.setDurationSeconds((long) mediaProperties.getStsDurationSeconds());
            request.setPolicy(uploadPolicy(objectKey));

            AssumeRoleResponse response = client.getAcsResponse(request);
            AssumeRoleResponse.Credentials credentials = response.getCredentials();
            OssStsTokenVo token = new OssStsTokenVo();
            token.setAccessKeyId(credentials.getAccessKeyId());
            token.setAccessKeySecret(credentials.getAccessKeySecret());
            token.setSecurityToken(credentials.getSecurityToken());
            token.setExpiration(credentials.getExpiration());
            token.setRegion(region);
            token.setEndpoint(mediaProperties.getEndpoint());
            token.setBucket(mediaProperties.getBucket());
            return token;
        }
        catch (Exception ex)
        {
            throw new ServiceException("获取 OSS 临时上传凭证失败：" + ex.getMessage());
        }
    }

    public long objectSize(String objectKey)
    {
        return execute(client -> client.getObjectMetadata(mediaProperties.getBucket(), objectKey).getContentLength());
    }

    public void downloadObject(String objectKey, Path targetFile)
    {
        execute(client -> {
            Files.createDirectories(targetFile.getParent());
            try (InputStream input = client.getObject(mediaProperties.getBucket(), objectKey).getObjectContent())
            {
                Files.copy(input, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return null;
        });
    }

    public byte[] readObjectBytes(String objectKey)
    {
        return execute(client -> {
            try (InputStream input = client.getObject(mediaProperties.getBucket(), objectKey).getObjectContent())
            {
                return input.readAllBytes();
            }
        });
    }

    public boolean objectExists(String objectKey)
    {
        String normalizedKey = trimSlashes(objectKey);
        if (!StringUtils.hasText(normalizedKey))
        {
            return false;
        }
        return execute(client -> client.doesObjectExist(mediaProperties.getBucket(), normalizedKey));
    }

    public void uploadFile(String objectKey, Path sourceFile, String contentType)
    {
        execute(client -> {
            ObjectMetadata metadata = new ObjectMetadata();
            if (StringUtils.hasText(contentType))
            {
                metadata.setContentType(contentType);
            }
            client.putObject(mediaProperties.getBucket(), objectKey, sourceFile.toFile(), metadata);
            return null;
        });
    }

    public String publicObjectUrl(String objectKey)
    {
        String normalizedKey = trimSlashes(objectKey);
        if (StringUtils.hasText(mediaProperties.getPublicBaseUrl()))
        {
            return trimTrailingSlash(mediaProperties.getPublicBaseUrl()) + "/" + normalizedKey;
        }
        String endpoint = trimProtocol(trimTrailingSlash(mediaProperties.getEndpoint()));
        return "https://" + mediaProperties.getBucket() + "." + endpoint + "/" + normalizedKey;
    }

    private <T> T execute(OssCallback<T> callback)
    {
        assertOssConfigured();
        OSS client = new OSSClientBuilder().build(mediaProperties.getEndpoint(), mediaProperties.getAccessKeyId(),
                mediaProperties.getAccessKeySecret());
        try
        {
            return callback.run(client);
        }
        catch (ServiceException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new ServiceException("OSS 操作失败：" + ex.getMessage());
        }
        finally
        {
            client.shutdown();
        }
    }

    private void assertOssConfigured()
    {
        if (!StringUtils.hasText(mediaProperties.getEndpoint()) || !StringUtils.hasText(mediaProperties.getBucket())
                || !StringUtils.hasText(mediaProperties.getAccessKeyId())
                || !StringUtils.hasText(mediaProperties.getAccessKeySecret()))
        {
            throw new ServiceException("OSS 配置不完整，请检查 endpoint、bucket、accessKeyId 和 accessKeySecret");
        }
    }

    private String uploadPolicy(String objectKey)
    {
        String resource = "acs:oss:*:*:" + mediaProperties.getBucket() + "/" + objectKey;
        return JSON.toJSONString(java.util.Map.of(
                "Version", "1",
                "Statement", List.of(java.util.Map.of(
                        "Effect", "Allow",
                        "Action", List.of("oss:PutObject", "oss:AbortMultipartUpload", "oss:ListParts",
                                "oss:InitiateMultipartUpload", "oss:CompleteMultipartUpload"),
                        "Resource", List.of(resource)))));
    }

    private String trimProtocol(String value)
    {
        if (value == null)
        {
            return "";
        }
        return value.replaceFirst("^https?://", "");
    }

    private String trimTrailingSlash(String value)
    {
        if (value == null)
        {
            return "";
        }
        return value.replaceAll("/+$", "");
    }

    private String trimSlashes(String value)
    {
        if (value == null)
        {
            return "";
        }
        return value.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    @FunctionalInterface
    private interface OssCallback<T>
    {
        T run(OSS client) throws Exception;
    }
}
