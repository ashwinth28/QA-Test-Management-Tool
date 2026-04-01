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
public class LocalFileUploadService {

    @Value("${file.upload.dir:./uploads}")
    private String uploadDir;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    public String uploadScreenshot(MultipartFile file) throws IOException {
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

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String filename = UUID.randomUUID().toString() + extension;
        String datePath = LocalDateTime.now().format(DATE_FORMAT);
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().resolve(datePath);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(filename);
        file.transferTo(filePath.toFile());

        String thumbnailFilename = "thumb_" + filename;
        Path thumbnailPath = uploadPath.resolve(thumbnailFilename);

        try {
            Thumbnails.of(filePath.toFile())
                    .size(200, 200)
                    .keepAspectRatio(true)
                    .toFile(thumbnailPath.toFile());
        } catch (Exception e) {
            System.err.println("Failed to generate thumbnail: " + e.getMessage());
        }

        return "/uploads/" + datePath.replace("\\", "/") + "/" + filename;
    }

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