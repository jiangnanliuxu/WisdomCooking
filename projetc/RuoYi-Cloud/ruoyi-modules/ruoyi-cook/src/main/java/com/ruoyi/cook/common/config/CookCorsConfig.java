package com.ruoyi.cook.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 码上智厨业务模块跨域配置。
 * <p>
 * 当前管理端页面以本地静态 HTML 方式打开，浏览器会从 file:// 或独立端口访问后端接口；
 * 这里放开业务模块接口跨域，保证管理端模型配置页面可以直接调用真实后端。
 * </p>
 */
@Configuration
public class CookCorsConfig implements WebMvcConfigurer
{
    @Override
    public void addCorsMappings(CorsRegistry registry)
    {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
