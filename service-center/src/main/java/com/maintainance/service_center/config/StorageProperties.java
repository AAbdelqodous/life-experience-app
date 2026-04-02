package com.maintainance.service_center.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "application.storage")
public class StorageProperties {
    private String uploadDir;
    private long maxFileSize;
    private List<String> allowedContentTypes;
}
