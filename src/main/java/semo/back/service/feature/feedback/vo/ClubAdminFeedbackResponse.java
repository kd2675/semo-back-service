package semo.back.service.feature.feedback.vo;

import java.util.List;

public record ClubAdminFeedbackResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean featureEnabled,
        int totalCount,
        int receivedCount,
        int inReviewCount,
        int answeredCount,
        int closedCount,
        int privateCount,
        int publicCount,
        List<ClubFeedbackSummaryResponse> items
) {
}
