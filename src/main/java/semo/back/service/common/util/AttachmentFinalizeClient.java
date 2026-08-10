package semo.back.service.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import semo.back.service.common.exception.SemoException;

@Component
public class AttachmentFinalizeClient {
    private final RestClient restClient;

    public AttachmentFinalizeClient(
            @Value("${integration.image.base-url:http://localhost:8081}") String imageBaseUrl
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(imageBaseUrl)
                .build();
    }

    public FinalizedAttachment finalizeAttachment(String fileName, String targetDir) {
        try {
            FinalizedAttachment response = restClient.post()
                    .uri("/files/finalize-attachment")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new FinalizeAttachmentRequest(fileName, targetDir))
                    .retrieve()
                    .body(FinalizedAttachment.class);
            if (response == null
                    || response.fileName() == null
                    || response.fileName().isBlank()
                    || response.contentType() == null
                    || response.contentType().isBlank()
                    || response.sizeBytes() < 1
                    || response.temporary()) {
                throw new SemoException.ValidationException("첨부파일 확정 응답이 올바르지 않습니다.");
            }
            return response;
        } catch (SemoException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SemoException.ValidationException("파일 서버 첨부파일 확정에 실패했습니다.");
        }
    }

    private record FinalizeAttachmentRequest(String fileName, String targetDir) {
    }

    public record FinalizedAttachment(
            String fileName,
            String originalFileName,
            String contentType,
            long sizeBytes,
            String downloadUrl,
            boolean temporary
    ) {
    }
}
