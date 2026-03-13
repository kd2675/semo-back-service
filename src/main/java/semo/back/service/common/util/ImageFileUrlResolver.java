package semo.back.service.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class ImageFileUrlResolver {
    private final String imageBaseUrl;

    public ImageFileUrlResolver(@Value("${integration.image.base-url:http://localhost:8081}") String imageBaseUrl) {
        this.imageBaseUrl = normalizeBaseUrl(imageBaseUrl);
    }

    public String resolveImageUrl(String fileName) {
        String normalized = normalizeFileName(fileName);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized;
        }
        if (normalized.startsWith("/images/")) {
            return imageBaseUrl + normalized;
        }
        if (normalized.startsWith("images/")) {
            return imageBaseUrl + "/" + normalized;
        }
        return imageBaseUrl + "/images/" + normalized;
    }

    public String resolveThumbnailUrl(String fileName) {
        String normalized = normalizeFileName(fileName);
        if (normalized == null) {
            return null;
        }
        int extensionIndex = normalized.lastIndexOf('.');
        if (extensionIndex < 0) {
            return resolveImageUrl(normalized + "_thumb");
        }
        String thumbFileName = normalized.substring(0, extensionIndex)
                + "_thumb"
                + normalized.substring(extensionIndex);
        return resolveImageUrl(thumbFileName);
    }

    private String normalizeBaseUrl(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String normalizeFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        String trimmed = fileName.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            try {
                return URI.create(trimmed).getPath();
            } catch (IllegalArgumentException ex) {
                return trimmed;
            }
        }
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed;
    }
}
