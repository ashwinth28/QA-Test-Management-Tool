package com.qa.testmanagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Get absolute path
        Path absolutePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = "file:" + absolutePath.toString() + "/";

        System.out.println("Serving files from: " + location);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}