package semo.back.service.feature.attachment.vo;

public record ResourceAttachmentResponse(
        Long attachmentId,
        Long clubId,
        String resourceType,
        Long resourceId,
        Long uploaderClubProfileId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String visibilityScope,
        String downloadUrl,
        String createdAt
) {
}
