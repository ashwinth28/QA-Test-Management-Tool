package com.qa.testmanagement.service;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryUploadService {

    @Autowired(required = false)
    private Cloudinary cloudinary;

    /**
     * Upload a screenshot to Cloudinary
     */
    public String uploadScreenshot(MultipartFile file) throws IOException {
        if (cloudinary == null) {
            throw new IllegalStateException("Cloudinary not configured");
        }

        // Validate file
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed (PNG, JPG, JPEG, GIF)");
        }

        // Validate file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }

        // Generate unique public ID
        String publicId = "qa-screenshots/" + UUID.randomUUID().toString();

        // Upload to Cloudinary
        Map<String, Object> uploadParams = new HashMap<>();
        uploadParams.put("public_id", publicId);
        uploadParams.put("folder", "qa-test-management");
        uploadParams.put("resource_type", "image");

        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);

        return uploadResult.get("secure_url").toString();
    }

    /**
     * Get thumbnail URL from original image URL
     * Cloudinary provides automatic transformations
     */
    public String getThumbnailUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        // Add Cloudinary transformation for thumbnail (200x200, crop to fit)
        return imageUrl.replace("/upload/", "/upload/w_200,h_200,c_fill/");
    }

    /**
     * Delete a file from Cloudinary
     */
    public boolean deleteFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return false;
        }

        if (cloudinary == null) {
            return false;
        }

        try {
            // Extract public ID from URL
            String publicId = extractPublicIdFromUrl(imageUrl);
            if (publicId == null) {
                return false;
            }

            // Delete from Cloudinary
            Map<String, Object> deleteParams = new HashMap<>();
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().destroy(publicId, deleteParams);
            return "ok".equals(result.get("result"));

        } catch (Exception e) {
            System.err.println("Failed to delete file from Cloudinary: " + e.getMessage());
            return false;
        }
    }

    private String extractPublicIdFromUrl(String url) {
        try {
            // URL format:
            // https://res.cloudinary.com/cloud-name/image/upload/v1234567890/qa-test-management/qa-screenshots/uuid.jpg
            String[] parts = url.split("/upload/");
            if (parts.length < 2) {
                return null;
            }

            String pathWithVersion = parts[1];
            String[] pathParts = pathWithVersion.split("/");

            // Skip version part (v1234567890) and build public ID
            StringBuilder publicIdBuilder = new StringBuilder();
            boolean foundVersion = false;

            for (int i = 0; i < pathParts.length; i++) {
                if (pathParts[i].startsWith("v") && !foundVersion) {
                    foundVersion = true;
                    continue;
                }
                if (publicIdBuilder.length() > 0) {
                    publicIdBuilder.append("/");
                }
                publicIdBuilder.append(pathParts[i]);
            }

            String publicId = publicIdBuilder.toString();
            // Remove file extension
            int lastDot = publicId.lastIndexOf(".");
            if (lastDot > 0) {
                publicId = publicId.substring(0, lastDot);
            }

            return publicId;
        } catch (Exception e) {
            System.err.println("Failed to extract public ID: " + e.getMessage());
            return null;
        }
    }
}