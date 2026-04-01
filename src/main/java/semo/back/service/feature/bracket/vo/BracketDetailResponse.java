package semo.back.service.feature.bracket.vo;

import java.util.List;

public record BracketDetailResponse(
        Long clubId,
        String clubName,
        boolean admin,
        Long bracketRecordId,
        String title,
        String summaryText,
        String approvalStatus,
        String bracketType,
        String participantType,
        String sourceType,
        Long sourceTournamentRecordId,
        String sourceTournamentTitle,
        String authorDisplayName,
        String authorAvatarImageUrl,
        String authorAvatarThumbnailUrl,
        String reviewedByDisplayName,
        String reviewedAtLabel,
        String rejectionReason,
        int participantCount,
        boolean mine,
        boolean canEdit,
        boolean canSubmit,
        boolean canDelete,
        boolean canReview,
        List<BracketParticipantResponse> participants,
        List<BracketRoundResponse> rounds
) {
}
