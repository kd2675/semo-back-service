package semo.back.service.common.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import semo.back.service.common.exception.SemoException;

@Component
public class AttachmentFileUrlResolver {
    private final String imageBaseUrl;

    public AttachmentFileUrlResolver(
            @Value("${integration.image.base-url:http://localhost:8081}") String imageBaseUrl
    ) {
        this.imageBaseUrl = imageBaseUrl.endsWith("/")
                ? imageBaseUrl.substring(0, imageBaseUrl.length() - 1)
                : imageBaseUrl;
    }

    public String resolveDownloadUrl(String fileName, String originalFileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        String normalizedFileName = fileName.trim();
        if (normalizedFileName.startsWith("/")
                || normalizedFileName.contains("..")
                || normalizedFileName.contains("\\")) {
            throw new SemoException.ValidationException("저장된 첨부파일 경로가 올바르지 않습니다.");
        }
        String encodedDownloadName = URLEncoder.encode(
                originalFileName,
                StandardCharsets.UTF_8
        ).replace("+", "%20");
        return imageBaseUrl + "/files/" + normalizedFileName + "?downloadName=" + encodedDownloadName;
    }
}
