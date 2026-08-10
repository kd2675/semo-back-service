package semo.back.service.common.util;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import semo.back.service.common.exception.SemoException;

@Component
public class AttachmentFinalizeClient {
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-File-Token";

    private final RestClient restClient;
    private final String internalToken;

    public AttachmentFinalizeClient(
            @Value("${integration.image.base-url:http://localhost:8081}") String imageBaseUrl,
            @Value("${integration.image.internal-token}") String internalToken
    ) {
        if (internalToken == null || internalToken.isBlank()) {
            throw new IllegalStateException("integration.image.internal-token must be configured");
        }
        this.restClient = RestClient.builder()
                .baseUrl(imageBaseUrl)
                .build();
        this.internalToken = internalToken;
    }

    public FinalizedAttachment finalizeAttachment(String fileName, String targetDir) {
        try {
            FinalizedAttachment response = restClient.post()
                    .uri("/files/finalize-attachment")
                    .header(INTERNAL_TOKEN_HEADER, internalToken)
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

    public void confirmFinalizedAttachment(String fileName) {
        executeLifecycleRequest("/internal/files/confirm-attachment", fileName, "확정");
    }

    public void deleteFinalizedAttachment(String fileName) {
        try {
            restClient.delete()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/files/finalized-attachment")
                            .queryParam("fileName", fileName)
                            .build())
                    .header(INTERNAL_TOKEN_HEADER, internalToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            throw new SemoException.ValidationException("고아 첨부파일 정리에 실패했습니다.");
        }
    }

    public List<PendingFinalizedAttachment> getPendingFinalizedAttachments() {
        try {
            PendingFinalizedAttachment[] response = restClient.get()
                    .uri("/internal/files/pending-attachments")
                    .header(INTERNAL_TOKEN_HEADER, internalToken)
                    .retrieve()
                    .body(PendingFinalizedAttachment[].class);
            return response == null ? List.of() : Arrays.asList(response);
        } catch (Exception exception) {
            throw new SemoException.ValidationException("고아 첨부파일 목록 조회에 실패했습니다.");
        }
    }

    public byte[] downloadAttachment(String fileName) {
        try {
            byte[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/files/content")
                            .queryParam("fileName", fileName)
                            .build())
                    .header(INTERNAL_TOKEN_HEADER, internalToken)
                    .retrieve()
                    .body(byte[].class);
            if (response == null || response.length == 0) {
                throw new SemoException.ValidationException("첨부파일 내용이 비어 있습니다.");
            }
            return response;
        } catch (SemoException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SemoException.ValidationException("첨부파일 다운로드에 실패했습니다.");
        }
    }

    private void executeLifecycleRequest(String path, String fileName, String action) {
        try {
            restClient.post()
                    .uri(path)
                    .header(INTERNAL_TOKEN_HEADER, internalToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new FinalizeAttachmentRequest(fileName, null))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            throw new SemoException.ValidationException("첨부파일 " + action + " 처리에 실패했습니다.");
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
            boolean temporary,
            boolean newlyFinalized
    ) {
    }

    public record PendingFinalizedAttachment(
            String fileName,
            LocalDateTime finalizedAt
    ) {
    }
}
