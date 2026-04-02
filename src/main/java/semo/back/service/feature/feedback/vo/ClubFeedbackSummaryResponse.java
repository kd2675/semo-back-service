package semo.back.service.feature.feedback.vo;

public record ClubFeedbackSummaryResponse(
        Long feedbackId,
        String feedbackType,
        String feedbackTypeLabel,
        String visibilityScope,
        String visibilityLabel,
        String statusCode,
        String statusLabel,
        String title,
        String contentPreview,
        boolean anonymous,
        String authorDisplayName,
        boolean mine,
        boolean answered,
        String createdAt,
        String createdAtLabel,
        String answeredAt,
        String answeredAtLabel
) {
}
