package com.qa.testmanagement.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        // Check if any credentials are missing (empty or null)
        if (cloudName == null || cloudName.trim().isEmpty() ||
                apiKey == null || apiKey.trim().isEmpty() ||
                apiSecret == null || apiSecret.trim().isEmpty()) {
            System.out.println("Cloudinary credentials not configured. Using local storage.");
            return null;
        }

        // Use HashMap for type safety
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        config.put("secure", "true");

        System.out.println("Cloudinary configured with cloud name: " + cloudName);
        return new Cloudinary(config);
    }
}