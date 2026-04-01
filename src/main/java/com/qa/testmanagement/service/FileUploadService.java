package com.qa.testmanagement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class FileUploadService {

    @Autowired(required = false)
    private CloudinaryUploadService cloudinaryUploadService;

    @Autowired
    private LocalFileUploadService localFileUploadService;

    @Value("${storage.type:local}")
    private String storageType;

    /**
     * Upload a screenshot file (uses configured storage type)
     */
    public String uploadScreenshot(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if ("cloudinary".equalsIgnoreCase(storageType) && cloudinaryUploadService != null) {
            try {
                return cloudinaryUploadService.uploadScreenshot(file);
            } catch (Exception e) {
                System.err.println("Cloudinary upload failed, falling back to local storage: " + e.getMessage());
                return localFileUploadService.uploadScreenshot(file);
            }
        } else {
            return localFileUploadService.uploadScreenshot(file);
        }
    }

    /**
     * Get thumbnail URL
     */
    public String getThumbnailUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }

        if ("cloudinary".equalsIgnoreCase(storageType) && cloudinaryUploadService != null) {
            return cloudinaryUploadService.getThumbnailUrl(imageUrl);
        } else {
            return localFileUploadService.getThumbnailUrl(imageUrl);
        }
    }

    /**
     * Delete a file
     */
    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return false;
        }

        if ("cloudinary".equalsIgnoreCase(storageType) && cloudinaryUploadService != null) {
            return cloudinaryUploadService.deleteFile(fileUrl);
        } else {
            return localFileUploadService.deleteFile(fileUrl);
        }
    }
}