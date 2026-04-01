package com.qa.testmanagement.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileUploadService {

    @Value("${file.upload.dir:./uploads}")
    private String uploadDir;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * Upload a screenshot file
     */
    public String uploadScreenshot(MultipartFile file) throws IOException {
        // Validate file
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed (PNG, JPG, JPEG, GIF)");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }

        // Get file extension
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Generate unique filename
        String filename = UUID.randomUUID().toString() + extension;

        // Create directory structure based on date
        String datePath = LocalDateTime.now().format(DATE_FORMAT);

        // Use absolute path for the upload directory
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().resolve(datePath);

        // Create directories if they don't exist
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            System.out.println("Created upload directory: " + uploadPath.toAbsolutePath());
        }

        // Save original file
        Path filePath = uploadPath.resolve(filename);
        file.transferTo(filePath.toFile());
        System.out.println("Saved file to: " + filePath.toAbsolutePath());

        // Create thumbnail
        String thumbnailFilename = "thumb_" + filename;
        Path thumbnailPath = uploadPath.resolve(thumbnailFilename);

        try {
            Thumbnails.of(filePath.toFile())
                    .size(200, 200)
                    .keepAspectRatio(true)
                    .toFile(thumbnailPath.toFile());
            System.out.println("Created thumbnail: " + thumbnailPath.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("Failed to generate thumbnail: " + e.getMessage());
        }

        // Return relative URL
        return "/uploads/" + datePath.replace("\\", "/") + "/" + filename;
    }

    /**
     * Get thumbnail URL from original image URL
     */
    public String getThumbnailUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        int lastSlash = imageUrl.lastIndexOf("/");
        if (lastSlash > 0) {
            String path = imageUrl.substring(0, lastSlash + 1);
            String filename = imageUrl.substring(lastSlash + 1);
            return path + "thumb_" + filename;
        }
        return null;
    }

    /**
     * Delete a file by its URL
     */
    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return false;
        }

        try {
            String relativePath = fileUrl.startsWith("/") ? fileUrl.substring(1) : fileUrl;
            Path filePath = Paths.get(uploadDir).toAbsolutePath().resolve(relativePath);
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                System.out.println("Deleted file: " + filePath.toAbsolutePath());
            }
            return deleted;
        } catch (IOException e) {
            System.err.println("Failed to delete file: " + e.getMessage());
            return false;
        }
    }
}