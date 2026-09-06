package com.tooldeck.backend.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 基础配置。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String allowedOrigins;

    /**
     * 创建 Web 基础配置。
     *
     * @param allowedOrigins 通过环境变量注入的允许跨域来源
     */
    public WebConfig(@Value("${app.cors.allowed-origins:}") String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * 只按显式环境变量开放本地前端来源，不启用凭据跨域。
     *
     * @param registry Spring MVC 跨域注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);

        if (origins.length == 0) {
            return;
        }

        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
