package semo.back.service.feature.feedback.vo;

import java.util.List;

public record ClubFeedbackHomeResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean featureEnabled,
        int totalVisibleCount,
        int mySubmissionCount,
        int publicVisibleCount,
        int answeredCount,
        List<ClubFeedbackSummaryResponse> items
) {
}
