package com.safepark.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * /uploads/** 요청을 실제 파일 시스템의 uploads/ 폴더로 매핑
     * 예) GET /uploads/analysis/123_image.jpg
     *     → ./uploads/analysis/123_image.jpg (프로젝트 루트 기준)
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadsPath = "file:" + System.getProperty("user.dir") + "/uploads/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsPath);
    }
}
