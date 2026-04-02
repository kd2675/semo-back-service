package semo.back.service.feature.feedback.vo;

public record ClubFeedbackDetailResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean featureEnabled,
        Long feedbackId,
        String feedbackType,
        String feedbackTypeLabel,
        String visibilityScope,
        String visibilityLabel,
        String statusCode,
        String statusLabel,
        String title,
        String content,
        boolean anonymous,
        String authorDisplayName,
        boolean mine,
        boolean canManage,
        String adminAnswer,
        String answeredByDisplayName,
        String createdAt,
        String createdAtLabel,
        String updatedAt,
        String updatedAtLabel,
        String answeredAt,
        String answeredAtLabel
) {
}
