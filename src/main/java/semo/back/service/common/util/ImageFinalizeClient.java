package semo.back.service.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import semo.back.service.common.exception.SemoException;

@Component
public class ImageFinalizeClient {
    private final RestClient restClient;

    public ImageFinalizeClient(@Value("${integration.image.base-url:http://localhost:8081}") String imageBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(imageBaseUrl)
                .build();
    }

    public FinalizedImage finalizeImage(String fileName, String targetDir) {
        try {
            FinalizedImage response = restClient.post()
                    .uri("/files/finalize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new FinalizeImageRequest(fileName, targetDir))
                    .retrieve()
                    .body(FinalizedImage.class);

            if (response == null || response.fileName() == null || response.fileName().isBlank()) {
                throw new SemoException.ValidationException("이미지 확정 응답이 올바르지 않습니다.");
            }
            return response;
        } catch (SemoException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SemoException.ValidationException("포토 서버 이미지 확정에 실패했습니다.");
        }
    }

    private record FinalizeImageRequest(String fileName, String targetDir) {
    }

    public record FinalizedImage(
            String fileName,
            String originalFileName,
            String imageUrl,
            String thumbnailUrl,
            boolean temporary
    ) {
    }
}
