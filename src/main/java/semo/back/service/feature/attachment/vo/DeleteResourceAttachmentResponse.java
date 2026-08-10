package semo.back.service.feature.attachment.vo;

public record DeleteResourceAttachmentResponse(
        Long attachmentId,
        boolean deleted
) {
}
