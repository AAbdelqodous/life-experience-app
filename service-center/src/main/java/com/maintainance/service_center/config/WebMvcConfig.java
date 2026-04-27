package com.maintainance.service_center.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.pattern.PathPatternParser;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final StorageProperties storageProperties;

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Prevent Spring from issuing 301 redirects for double-slash URLs (e.g. //users/me).
        // A redirect strips CORS headers, breaking preflight checks from the web client.
        PathPatternParser parser = new PathPatternParser();
        parser.setMatchOptionalTrailingSeparator(false);
        configurer.setPatternParser(parser);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(storageProperties.getUploadDir()).toAbsolutePath().normalize();
        String resourceLocation = "file:" + uploadPath + "/";
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation)
                .setCachePeriod(3600);
    }
}
